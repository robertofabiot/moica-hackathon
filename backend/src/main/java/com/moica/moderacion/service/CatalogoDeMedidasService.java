package com.moica.moderacion.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.moderacion.dto.DatosDeMedidaAdministrativa;
import com.moica.moderacion.dto.MedidaACrear;
import com.moica.moderacion.dto.MedidaAEditar;
import com.moica.moderacion.entity.MedidaAdministrativa;
import com.moica.moderacion.repository.MedidaAdministrativaRepository;
import com.moica.usuario.entity.EstadoCuenta;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La gestión del catálogo de medidas administrativas.
 *
 * <p>El catálogo describe qué sanciones existen; no decide ninguna. Aplicar una medida a una cuenta
 * es otra cosa y vive en {@link MedidasDeCasoService}, porque se hace desde un expediente concreto
 * y arrastra estado de cuenta, sesiones e historial.
 *
 * <p><b>Nada se borra.</b> No hay operación de eliminación ni la habrá: una medida citada por un
 * caso o por una versión del historial es la evidencia de una decisión, y sus claves foráneas son
 * {@code RESTRICT}. Lo que el negocio llama «eliminar» es {@link #cambiarHabilitacion
 * deshabilitar}, y su efecto es exactamente el que pide la definición 11.3: la medida deja de
 * ofrecerse para aplicaciones nuevas y sigue describiendo correctamente las decisiones anteriores.
 *
 * <p>Dos reglas que las anotaciones no pueden expresar y por eso se comprueban aquí:
 *
 * <ul>
 *   <li>El código y el nombre no se repiten. La comprobación previa cubre el caso normal y las
 *       restricciones únicas de PostgreSQL arbitran dos altas simultáneas, igual que hace el
 *       registro de cuentas: el perdedor recibe 409 y no un 500.
 *   <li>Una medida que <b>exige</b> fecha de fin no puede dejar la cuenta {@code
 *       SUSPENDIDA_PERMANENTE}, y una que no la exige no puede dejarla en un estado temporal. Si no
 *       coincidieran, el catálogo prometería un plazo que el estado no admite, o al revés.
 * </ul>
 */
@Service
public class CatalogoDeMedidasService {

  private final MedidaAdministrativaRepository medidas;

  public CatalogoDeMedidasService(MedidaAdministrativaRepository medidas) {
    this.medidas = medidas;
  }

  /**
   * El catálogo completo, de la medida más leve a la más grave.
   *
   * <p>Incluye las deshabilitadas: la pantalla de gestión las necesita para poder volver a
   * habilitarlas, y quien consulta ya pasó por rol y segundo factor. Cuál puede aplicarse es una
   * decisión distinta, y la toma {@link MedidasDeCasoService}.
   */
  @Transactional(readOnly = true)
  public List<DatosDeMedidaAdministrativa> consultarCatalogo(UsuarioAutenticado sujeto) {
    GuardiasDeCaso.exigirPermisoAdministrativo(sujeto);

    return medidas.findAllByOrderByNivelSeveridadAscNombreAsc().stream()
        .map(DatosDeMedidaAdministrativa::de)
        .toList();
  }

  /**
   * Añade una medida al catálogo.
   *
   * @throws ErrorDeAplicacion 409 {@code MEDIDA_DUPLICADA} si el código o el nombre ya existen; 400
   *     {@code MEDIDA_INCOHERENTE} si el plazo y el estado resultante no encajan
   */
  @Transactional
  public DatosDeMedidaAdministrativa crear(UsuarioAutenticado sujeto, MedidaACrear pedido) {
    GuardiasDeCaso.exigirPermisoAdministrativo(sujeto);
    exigirCoherenciaDelPlazo(pedido.estadoCuentaResultante(), pedido.requiereFechaFin());

    if (medidas.existsByCodigoIgnoreCase(pedido.codigo())
        || medidas.existsByNombreIgnoreCase(pedido.nombre())) {
      throw medidaDuplicada();
    }

    MedidaAdministrativa medida =
        new MedidaAdministrativa(
            pedido.codigo(),
            pedido.nombre(),
            pedido.descripcion(),
            pedido.nivelSeveridad(),
            pedido.estadoCuentaResultante(),
            pedido.requiereFechaFin());

    try {
      return DatosDeMedidaAdministrativa.de(medidas.saveAndFlush(medida));
    } catch (DataIntegrityViolationException yaExistia) {
      // La comprobación previa no basta: dos altas simultáneas leen las dos que
      // el código todavía no existe. La unicidad de PostgreSQL decide cuál se
      // queda y la otra sale por aquí como conflicto, no como fallo.
      throw medidaDuplicada();
    }
  }

  /**
   * Reescribe una medida sin tocar su código ni su habilitación.
   *
   * <p>El cambio rige para lo que venga. Las decisiones ya tomadas conservan en su versión del
   * historial el estado de cuenta y la fecha de fin que realmente se les impusieron, así que
   * ninguna se reescribe por corregir aquí una descripción o una severidad.
   *
   * @throws ErrorDeAplicacion 404 si la medida no existe; 409 {@code MEDIDA_DUPLICADA} si otra ya
   *     usa ese nombre; 400 {@code MEDIDA_INCOHERENTE} si el plazo y el estado no encajan
   */
  @Transactional
  public DatosDeMedidaAdministrativa editar(
      UsuarioAutenticado sujeto, Short idMedida, MedidaAEditar pedido) {

    GuardiasDeCaso.exigirPermisoAdministrativo(sujeto);
    exigirCoherenciaDelPlazo(pedido.estadoCuentaResultante(), pedido.requiereFechaFin());

    MedidaAdministrativa medida = buscar(idMedida);

    if (medidas.existsByNombreIgnoreCaseAndIdMedidaAdministrativaNot(pedido.nombre(), idMedida)) {
      throw medidaDuplicada();
    }

    medida.editar(
        pedido.nombre(),
        pedido.descripcion(),
        pedido.nivelSeveridad(),
        pedido.estadoCuentaResultante(),
        pedido.requiereFechaFin());

    try {
      return DatosDeMedidaAdministrativa.de(medidas.saveAndFlush(medida));
    } catch (DataIntegrityViolationException yaExistia) {
      throw medidaDuplicada();
    }
  }

  /**
   * Habilita o deshabilita una medida.
   *
   * <p>Deshabilitar es lo más cerca de «eliminar» que el catálogo llega. La fila permanece, las
   * medidas ya aplicadas siguen vigentes sobre sus cuentas y el historial la sigue nombrando; lo
   * único que cambia es que deja de poder elegirse para una aplicación nueva.
   *
   * @throws ErrorDeAplicacion 404 si la medida no existe
   */
  @Transactional
  public DatosDeMedidaAdministrativa cambiarHabilitacion(
      UsuarioAutenticado sujeto, Short idMedida, boolean habilitada) {

    GuardiasDeCaso.exigirPermisoAdministrativo(sujeto);

    MedidaAdministrativa medida = buscar(idMedida);
    medida.cambiarHabilitacion(habilitada);
    return DatosDeMedidaAdministrativa.de(medida);
  }

  /**
   * La medida que se puede aplicar ahora mismo, para uso de {@link MedidasDeCasoService}.
   *
   * <p>Una medida deshabilitada no llega hasta aquí. Es la comprobación que cierra la carrera entre
   * cargar la pantalla y enviar el formulario: si alguien la deshabilitó mientras tanto, la
   * aplicación se rechaza en lugar de imponer una sanción que el catálogo ya había retirado.
   *
   * @throws ErrorDeAplicacion 404 si no existe; 409 {@code MEDIDA_DESHABILITADA} si ya no se ofrece
   */
  @Transactional(readOnly = true)
  public MedidaAdministrativa exigirAplicable(Short idMedida) {
    MedidaAdministrativa medida = buscar(idMedida);

    if (!medida.isHabilitada()) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "MEDIDA_DESHABILITADA",
          "Esa medida ya no está disponible en el catálogo. Elige otra.");
    }
    return medida;
  }

  /** Una medida cualquiera del catálogo, habilitada o no, para describir el historial. */
  @Transactional(readOnly = true)
  public MedidaAdministrativa obtener(Short idMedida) {
    return buscar(idMedida);
  }

  private MedidaAdministrativa buscar(Short idMedida) {
    return medidas
        .findById(idMedida)
        .orElseThrow(
            () ->
                new ErrorDeAplicacion(
                    HttpStatus.NOT_FOUND,
                    "MEDIDA_NO_ENCONTRADA",
                    "Esa medida administrativa no existe."));
  }

  /**
   * Comprueba que el plazo y el estado resultante digan lo mismo.
   *
   * <p>Los dos estados temporales terminan en una fecha, así que la medida que los impone tiene que
   * pedirla; {@code ACTIVA} y {@code SUSPENDIDA_PERMANENTE} no terminan solos, así que pedirla
   * sería prometer una reactivación que nunca llegaría. Una medida sin estado resultante —una
   * advertencia— tampoco tiene plazo que cumplir.
   */
  private static void exigirCoherenciaDelPlazo(
      EstadoCuenta estadoCuentaResultante, boolean requiereFechaFin) {

    boolean esTemporal =
        estadoCuentaResultante == EstadoCuenta.RESTRINGIDA_TEMPORAL
            || estadoCuentaResultante == EstadoCuenta.SUSPENDIDA_TEMPORAL;

    if (esTemporal != requiereFechaFin) {
      throw new ErrorDeAplicacion(
          HttpStatus.BAD_REQUEST,
          "MEDIDA_INCOHERENTE",
          esTemporal
              ? "Una medida que restringe o suspende temporalmente tiene que exigir fecha de"
                  + " finalización."
              : "Solo las medidas temporales pueden exigir fecha de finalización.");
    }
  }

  private static ErrorDeAplicacion medidaDuplicada() {
    return new ErrorDeAplicacion(
        HttpStatus.CONFLICT,
        "MEDIDA_DUPLICADA",
        "Ya existe una medida con ese código o ese nombre.");
  }
}
