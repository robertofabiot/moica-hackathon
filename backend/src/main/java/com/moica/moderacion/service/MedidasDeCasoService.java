package com.moica.moderacion.service;

import com.moica.auth.entity.MotivoRevocacionSesion;
import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.auth.service.SesionService;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.moderacion.dto.DatosDeExpedienteDeCaso;
import com.moica.moderacion.dto.MedidaAAplicar;
import com.moica.moderacion.entity.CasoModeracion;
import com.moica.moderacion.entity.EstadoCasoModeracion;
import com.moica.moderacion.entity.MedidaAdministrativa;
import com.moica.moderacion.entity.ResultadoCasoModeracion;
import com.moica.moderacion.entity.TipoEventoHistorial;
import com.moica.moderacion.repository.CasoModeracionRepository;
import com.moica.usuario.entity.EstadoCuenta;
import com.moica.usuario.service.UsuarioService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las medidas administrativas que una persona aplica y revoca sobre una cuenta.
 *
 * <p>Es la parte de la moderación que <b>sí</b> sanciona. {@code RevisionDeCasosService} resuelve
 * el expediente y no toca ninguna cuenta; aquí una persona administradora elige una medida del
 * catálogo y la aplica, la revoca o la sustituye, con todo lo que eso arrastra: el estado operativo
 * de la cuenta, las sesiones abiertas y el historial del expediente.
 *
 * <p><b>La sanción la decide siempre una persona.</b> Moica no recomienda medidas, no las elige por
 * reincidencia, no escala por severidad y no puntúa riesgos: la definición 11.3 y la decisión
 * D-MOD-01 lo dejan fuera del MVP. Lo único que ocurre sin intervención humana es la
 * <em>expiración</em> de una medida temporal cuyo plazo fijó una persona, y vive en {@link
 * ExpiracionDeMedidas} precisamente para que se lea como lo que es: ejecutar un plazo, no decidir
 * una sanción.
 *
 * <h2>Una sola medida vigente por cuenta</h2>
 *
 * <p>La regla D-MOD-03 es de la cuenta, no del expediente: una persona con tres casos abiertos
 * sostiene como mucho una sanción. Aplicar una segunda no sustituye nada en silencio; responde 409
 * {@code MEDIDA_VIGENTE_EXISTENTE} diciendo cuál está vigente, y solo un reenvío con {@code
 * confirmaReemplazo} revoca la anterior y aplica la nueva <b>dentro de la misma transacción</b>. No
 * existe ningún instante confirmado en el que la cuenta tenga dos.
 *
 * <h2>Orden de bloqueo, siempre el mismo</h2>
 *
 * <p>Primero la <b>cuenta afectada</b> y después el expediente. Es deliberado y evita dos problemas
 * a la vez:
 *
 * <ul>
 *   <li>Bloquear solo el caso no serviría. Dos administradoras que sancionan a la misma persona
 *       desde expedientes distintos tocan filas distintas, así que las dos leerían que no hay
 *       ninguna medida vigente y las dos la aplicarían. La cuenta es lo único que comparten.
 *   <li>Bloquear el caso antes que la cuenta abriría un abrazo mortal con el reemplazo, que
 *       necesita dos expedientes: una transacción tendría el caso A y querría la cuenta, mientras
 *       la otra tiene la cuenta y quiere el caso A. Con la cuenta siempre primero, dos operaciones
 *       sobre la misma persona se serializan antes de tocar ningún expediente.
 * </ul>
 *
 * <p>El identificador de la persona reportada se lee sin bloqueo para saber a quién bloquear. Es
 * seguro porque es inmutable: {@code idReportado} es {@code updatable = false} desde que P9 abrió
 * el caso.
 *
 * <p>Por si el código se equivocara alguna vez, {@code
 * uq_caso_moderacion_medida_vigente_por_cuenta} sostiene la regla desde PostgreSQL y una carrera
 * que llegara hasta allí sale como 409, no como 500.
 */
@Service
public class MedidasDeCasoService {

  private final CasoModeracionRepository casos;
  private final CatalogoDeMedidasService catalogo;
  private final RevisionDeCasosService expedientes;
  private final VersionadoDeCasos versionado;
  private final UsuarioService usuarios;
  private final SesionService sesiones;
  private final Clock reloj;

  public MedidasDeCasoService(
      CasoModeracionRepository casos,
      CatalogoDeMedidasService catalogo,
      RevisionDeCasosService expedientes,
      VersionadoDeCasos versionado,
      UsuarioService usuarios,
      SesionService sesiones,
      Clock reloj) {
    this.casos = casos;
    this.catalogo = catalogo;
    this.expedientes = expedientes;
    this.versionado = versionado;
    this.usuarios = usuarios;
    this.sesiones = sesiones;
    this.reloj = reloj;
  }

  /**
   * Aplica a la cuenta reportada la medida que una persona administradora eligió.
   *
   * <p>Solo desde un caso {@code CERRADO} con resultado {@link ResultadoCasoModeracion#PROCEDENTE}.
   * Es la única decisión administrativa que, según la definición 11.2, dice que el caso «amerita
   * una decisión administrativa»: sancionar sin haber cerrado saltaría la revisión, y sancionar
   * tras un {@code DESESTIMADO} contradiría la resolución que acaba de firmarse.
   *
   * <p>Cuando la cuenta ya sostiene otra medida, la primera petición responde 409 con el detalle de
   * cuál está vigente y no cambia nada. Confirmada la sustitución, todo ocurre junto: se revoca la
   * anterior, se aplica la nueva, se proyecta el estado de cuenta, se versionan los expedientes
   * afectados y se revocan las sesiones si el acceso deja de estar permitido.
   *
   * @throws ErrorDeAplicacion 404 si el caso o la medida no existen; 403 {@code
   *     CASO_DE_OTRO_ADMINISTRADOR}; 409 {@code CASO_SIN_RESPONSABLE}, {@code
   *     MEDIDA_DESHABILITADA}, {@code MEDIDA_NO_APLICABLE} o {@code MEDIDA_VIGENTE_EXISTENTE}; 400
   *     si el plazo no encaja con la medida elegida
   */
  @Transactional
  public DatosDeExpedienteDeCaso aplicar(
      UsuarioAutenticado sujeto, Long idCaso, MedidaAAplicar pedido) {

    GuardiasDeCaso.exigirPermisoAdministrativo(sujeto);

    // Antes que nada: la medida tiene que existir y seguir ofreciéndose. Si
    // alguien la deshabilitó entre que se pintó el formulario y se envió, esto
    // es lo que impide imponer una sanción que el catálogo ya había retirado.
    MedidaAdministrativa medida = catalogo.exigirAplicable(pedido.idMedidaAdministrativa());

    CasoModeracion caso = bloquearTrasLaCuenta(idCaso);
    GuardiasDeCaso.exigirQueSeaElResponsable(caso, sujeto);

    if (caso.getEstadoActual() != EstadoCasoModeracion.CERRADO
        || caso.getResultadoActual() != ResultadoCasoModeracion.PROCEDENTE) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "MEDIDA_NO_APLICABLE",
          "Solo se aplica una medida desde un caso cerrado como procedente. El caso está en estado "
              + caso.getEstadoActual()
              + ".");
    }

    OffsetDateTime instante = OffsetDateTime.now(reloj);
    OffsetDateTime fechaFin = plazoValidado(medida, pedido.fechaFinMedida(), instante);

    Optional<CasoModeracion> anterior =
        casos.findByIdReportadoAndIdMedidaAdministrativaActualNotNull(caso.getIdReportado());

    if (anterior.isPresent() && !pedido.confirmaReemplazo()) {
      throw medidaVigenteExistente(anterior.get(), caso);
    }

    // El estado resultante se calcula antes de escribir nada porque las dos
    // versiones que puede dejar esta operación —la revocación de la anterior y
    // la aplicación de la nueva— empiezan en el mismo instante y describen ya
    // la cuenta como queda. Una fotografía con el estado viejo mentiría desde
    // el primer segundo de su vigencia.
    EstadoCuenta estadoResultante = estadoQueImpone(medida);

    usuarios.proyectarEstadoDeCuenta(caso.getIdReportado(), estadoResultante, fechaFin);

    boolean reemplazaOtroExpediente =
        anterior.isPresent() && !anterior.get().getIdCasoModeracion().equals(idCaso);

    if (reemplazaOtroExpediente) {
      revocarLaAnteriorDeOtroCaso(anterior.get(), sujeto, estadoResultante, instante);
    }

    caso.aplicarMedida(medida.getIdMedidaAdministrativa(), fechaFin, instante);

    try {
      casos.saveAndFlush(caso);
    } catch (DataIntegrityViolationException carrera) {
      // Solo se llega aquí si el índice único parcial encuentra otra medida
      // vigente que esta transacción no vio. Con el bloqueo de cuenta no debería
      // ocurrir; si ocurriera, sigue siendo un conflicto de negocio y no un fallo.
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "MEDIDA_VIGENTE_EXISTENTE",
          "Esta cuenta acaba de recibir otra medida. Vuelve a abrir el expediente para ver cuál"
              + " está vigente.");
    }

    versionado.versionar(
        caso,
        sujeto.idUsuario(),
        TipoEventoHistorial.MEDIDA_APLICADA,
        estadoResultante,
        detalleDeAplicacion(medida, anterior.orElse(null), idCaso, pedido.justificacion()),
        instante);

    revocarSesionesSiElAccesoSeCierra(caso.getIdReportado(), estadoResultante);

    return expedientes.consultarExpediente(sujeto, idCaso);
  }

  /**
   * Levanta la medida que este expediente sostenía y devuelve la cuenta a {@link
   * EstadoCuenta#ACTIVA}.
   *
   * <p>No exige ningún estado del caso, a diferencia de aplicar. Revocar siempre reduce la sanción
   * y nunca perjudica a nadie; condicionarla a que el caso esté cerrado dejaría a una persona
   * sancionada mientras su expediente vuelve a revisarse, que es justo cuando una apelación
   * aceptada pide levantarla.
   *
   * <p>La cuenta vuelve a {@code ACTIVA} porque era su única medida: la regla D-MOD-03 garantiza
   * que no había otra esperando debajo.
   *
   * <p>No revoca sesiones: volver a estar activa devuelve acceso, no lo quita.
   *
   * @throws ErrorDeAplicacion 404 si el caso no existe; 403 {@code CASO_DE_OTRO_ADMINISTRADOR}; 409
   *     {@code CASO_SIN_RESPONSABLE} o {@code SIN_MEDIDA_VIGENTE} si ya no había ninguna
   */
  @Transactional
  public DatosDeExpedienteDeCaso revocar(UsuarioAutenticado sujeto, Long idCaso, String motivo) {
    GuardiasDeCaso.exigirPermisoAdministrativo(sujeto);

    CasoModeracion caso = bloquearTrasLaCuenta(idCaso);
    GuardiasDeCaso.exigirQueSeaElResponsable(caso, sujeto);

    if (caso.getIdMedidaAdministrativaActual() == null) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "SIN_MEDIDA_VIGENTE",
          "Este caso no sostiene ninguna medida vigente.");
    }

    MedidaAdministrativa revocada = catalogo.obtener(caso.getIdMedidaAdministrativaActual());
    OffsetDateTime instante = OffsetDateTime.now(reloj);

    caso.retirarMedida(instante);
    usuarios.proyectarEstadoDeCuenta(caso.getIdReportado(), EstadoCuenta.ACTIVA, null);

    versionado.versionar(
        caso,
        sujeto.idUsuario(),
        TipoEventoHistorial.MEDIDA_REVOCADA,
        EstadoCuenta.ACTIVA,
        "Se revocó la medida «" + revocada.getNombre() + "». " + motivo,
        instante);

    return expedientes.consultarExpediente(sujeto, idCaso);
  }

  /**
   * Revoca la medida que sostenía otro expediente, dentro de la misma transacción del reemplazo.
   *
   * <p>La versión que deja fotografía el estado de cuenta <b>ya sustituido</b>, no {@code ACTIVA}:
   * su vigencia empieza en el mismo instante en que la medida nueva entra, y decir que la cuenta
   * quedó activa durante ese periodo sería falso. El expediente que pierde la medida no deja libre
   * a nadie; solo deja de ser el que la sostiene.
   *
   * <p>La escritura se vacía antes de que el caso nuevo tome la medida, para que el índice único
   * parcial encuentre el hueco libre. Es el mismo motivo por el que el versionado SCD2 vacía el
   * cierre antes de insertar la versión siguiente.
   */
  private void revocarLaAnteriorDeOtroCaso(
      CasoModeracion anterior,
      UsuarioAutenticado sujeto,
      EstadoCuenta estadoResultante,
      OffsetDateTime instante) {

    CasoModeracion bloqueado = bloquear(anterior.getIdCasoModeracion());
    MedidaAdministrativa sustituida = catalogo.obtener(bloqueado.getIdMedidaAdministrativaActual());

    bloqueado.retirarMedida(instante);
    casos.saveAndFlush(bloqueado);

    versionado.versionar(
        bloqueado,
        sujeto.idUsuario(),
        TipoEventoHistorial.MEDIDA_REVOCADA,
        estadoResultante,
        "Se revocó la medida «"
            + sustituida.getNombre()
            + "» para sustituirla por la que impuso otro expediente del mismo caso de esta"
            + " cuenta.",
        instante);
  }

  /**
   * Corta las sesiones abiertas cuando la medida cierra el acceso.
   *
   * <p>Solo con los dos estados de suspensión. {@link EstadoCuenta#RESTRINGIDA_TEMPORAL} conserva
   * la sesión a propósito: esa persona sigue pudiendo consultar su historial, cerrar compromisos
   * existentes y abrir casos, y expulsarla no protegería nada. Lo que no puede hacer se lo impide
   * la autorización, que relee el estado de la cuenta en cada petición.
   *
   * <p>Con una suspensión no basta con eso: mientras el JWT siga siendo válido, su portador
   * seguiría llegando al servidor. Revocar la fila de sesión es lo que hace que la petición
   * siguiente ya no tenga acceso aunque el token no haya expirado.
   */
  private void revocarSesionesSiElAccesoSeCierra(Long idUsuario, EstadoCuenta estado) {
    if (estado.bloqueaElAcceso()) {
      sesiones.revocarTodasDe(idUsuario, MotivoRevocacionSesion.MEDIDA_ADMINISTRATIVA);
    }
  }

  /**
   * El estado en el que la medida deja la cuenta.
   *
   * <p>Una medida sin estado resultante —una advertencia— no toca el acceso, así que la cuenta
   * queda {@code ACTIVA}. Y queda activa de verdad, no «como estuviera»: al aplicarla se revocó
   * cualquier otra medida vigente, porque solo puede haber una.
   */
  private static EstadoCuenta estadoQueImpone(MedidaAdministrativa medida) {
    return medida.getEstadoCuentaResultante() == null
        ? EstadoCuenta.ACTIVA
        : medida.getEstadoCuentaResultante();
  }

  /**
   * Comprueba que el plazo enviado sea el que la medida elegida admite.
   *
   * <p>Una medida temporal sin fecha no terminaría nunca y una permanente con fecha prometería una
   * reactivación que nadie ejecutaría. Y una fecha ya pasada nacería expirada: el barrido la
   * levantaría en su siguiente pasada, de modo que la sanción no habría existido.
   */
  private static OffsetDateTime plazoValidado(
      MedidaAdministrativa medida, OffsetDateTime fechaFin, OffsetDateTime instante) {

    if (!medida.isRequiereFechaFin()) {
      if (fechaFin != null) {
        throw new ErrorDeAplicacion(
            HttpStatus.BAD_REQUEST,
            "FECHA_FIN_NO_ADMITIDA",
            "Esta medida no termina sola, así que no lleva fecha de finalización.");
      }
      return null;
    }

    if (fechaFin == null) {
      throw new ErrorDeAplicacion(
          HttpStatus.BAD_REQUEST,
          "FECHA_FIN_REQUERIDA",
          "Esta medida es temporal: indica cuándo termina.");
    }
    if (!fechaFin.isAfter(instante)) {
      throw new ErrorDeAplicacion(
          HttpStatus.BAD_REQUEST,
          "FECHA_FIN_INVALIDA",
          "La fecha de finalización tiene que estar en el futuro.");
    }
    return fechaFin;
  }

  /**
   * Bloquea la cuenta afectada y después el expediente, siempre en ese orden.
   *
   * <p>La consulta previa devuelve <b>solo el identificador</b> de la persona reportada, y eso no
   * es un detalle: cargar el caso entero para averiguarlo lo dejaría en el contexto de
   * persistencia, y entonces el {@code SELECT … FOR UPDATE} de {@link #bloquear} bloquearía la fila
   * pero devolvería la copia en memoria sin releerla. La transacción trabajaría con el estado
   * <em>anterior</em> al bloqueo y podría, por ejemplo, revocar una medida que otra transacción
   * acababa de sustituir, dejando la cuenta activa con una sanción viva en otro expediente.
   */
  private CasoModeracion bloquearTrasLaCuenta(Long idCaso) {
    Long idReportado = casos.idReportadoDe(idCaso).orElseThrow(GuardiasDeCaso::casoNoEncontrado);

    usuarios.bloquearCuenta(idReportado);
    return bloquear(idCaso);
  }

  private CasoModeracion bloquear(Long idCaso) {
    return casos.bloquearPorId(idCaso).orElseThrow(GuardiasDeCaso::casoNoEncontrado);
  }

  private static String detalleDeAplicacion(
      MedidaAdministrativa medida, CasoModeracion anterior, Long idCaso, String justificacion) {

    if (anterior == null) {
      return "Se aplicó la medida «" + medida.getNombre() + "». " + justificacion;
    }
    if (anterior.getIdCasoModeracion().equals(idCaso)) {
      return "Se sustituyó la medida vigente de este caso por «"
          + medida.getNombre()
          + "». "
          + justificacion;
    }
    return "Se aplicó la medida «"
        + medida.getNombre()
        + "» en sustitución de la que sostenía el caso "
        + anterior.getIdCasoModeracion()
        + ". "
        + justificacion;
  }

  /**
   * El conflicto que obliga a confirmar la sustitución.
   *
   * <p>Nombra el expediente y la situación vigentes porque quien decide necesita saber qué va a
   * sustituir antes de confirmarlo. Es información administrativa y no sale de {@code /api/admin}.
   */
  private static ErrorDeAplicacion medidaVigenteExistente(
      CasoModeracion anterior, CasoModeracion caso) {

    String donde =
        anterior.getIdCasoModeracion().equals(caso.getIdCasoModeracion())
            ? "por este mismo caso"
            : "por el caso " + anterior.getIdCasoModeracion();

    return new ErrorDeAplicacion(
        HttpStatus.CONFLICT,
        "MEDIDA_VIGENTE_EXISTENTE",
        "Esta cuenta ya tiene una medida vigente impuesta "
            + donde
            + ". Confirma la sustitución para revocarla y aplicar la nueva.");
  }
}
