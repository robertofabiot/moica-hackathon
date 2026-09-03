package com.moica.moderacion.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.moderacion.dto.DatosDeCasoModeracion;
import com.moica.moderacion.dto.EstadoDeReporte;
import com.moica.moderacion.dto.ReporteAPresentar;
import com.moica.moderacion.entity.CasoModeracion;
import com.moica.moderacion.entity.HistorialCaso;
import com.moica.moderacion.repository.CasoModeracionRepository;
import com.moica.moderacion.repository.HistorialCasoRepository;
import com.moica.prestador.service.PerfilPrestadorService;
import com.moica.solicitud.dto.ParticipacionEnSolicitud;
import com.moica.solicitud.service.SolicitudServicioService;
import com.moica.usuario.entity.EstadoCuenta;
import com.moica.usuario.service.UsuarioService;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El reporte con el que un participante abre un caso de moderación sobre el otro.
 *
 * <p>No decide por su cuenta quién participa ni por qué estados pasó la solicitud: se lo pregunta a
 * {@link SolicitudServicioService}, que es dueño de esa regla, igual que hacen {@code chat} y
 * {@code calificacion}. Aquí queda lo propio de reportar.
 *
 * <p>Cinco reglas gobiernan todo lo que sigue:
 *
 * <ul>
 *   <li>Un tercero recibe 404, igual que en el resto de recursos propios: no puede confirmar
 *       siquiera que la solicitud exista.
 *   <li><b>El reportado sale de la solicitud, nunca del cuerpo.</b> Quien reporta es la sesión y el
 *       reportado es la contraparte; no hay forma de formular un reporte contra otra persona.
 *   <li>Solo se reporta desde una solicitud que <em>llegó</em> a estar aceptada. Da igual dónde
 *       terminara: completada o cancelada después de aceptarse siguen admitiendo el reporte, porque
 *       el trato existió. Pendiente, rechazada y cancelada desde pendiente no.
 *   <li>Cada participante abre como máximo un caso por solicitud. La comprobación previa cubre el
 *       caso normal y {@code uq_caso_moderacion_solicitud_reportante} decide la carrera entre dos
 *       envíos simultáneos. Con las dos partes, una solicitud admite hasta dos casos: uno por lado.
 *   <li>Una cuenta {@code RESTRINGIDA_TEMPORAL} conserva el reporte. Es lo contrario que calificar:
 *       reportar es la vía por la que alguien pide ayuda, y quitársela justo a quien ya arrastra
 *       una restricción la dejaría sin recurso frente a la contraparte. Las suspendidas no llegan
 *       hasta aquí: la cadena de seguridad exige sesión plena en toda ruta de negocio.
 * </ul>
 *
 * <p>Reportar no hace nada más que abrir el expediente. No cambia el estado de la solicitud, no
 * toca ninguna cuenta, no asigna administrador, no elige medida, no revoca sesiones y no sanciona.
 * Tampoco depende de reincidencia, severidad ni número de casos: en el MVP cada medida la elige una
 * persona, según la definición 11.3.
 */
@Service
public class CasoModeracionService {

  /**
   * Justificación con la que nace la primera versión del historial.
   *
   * <p>El diccionario exige un {@code detalleCambio} no vacío y {@code
   * ck_historial_caso_detalle_cambio} lo comprueba. En la apertura no hay nada que justificar más
   * allá del propio hecho: quien reportó y sobre quién ya están en la versión, y el motivo y la
   * descripción viven en el caso, donde no se duplican.
   */
  private static final String DETALLE_DE_APERTURA =
      "El participante reportó a la contraparte desde la solicitud y se abrió el caso.";

  private final SolicitudServicioService solicitudes;
  private final CasoModeracionRepository casos;
  private final HistorialCasoRepository historial;
  private final PerfilPrestadorService perfiles;
  private final UsuarioService usuarios;

  public CasoModeracionService(
      SolicitudServicioService solicitudes,
      CasoModeracionRepository casos,
      HistorialCasoRepository historial,
      PerfilPrestadorService perfiles,
      UsuarioService usuarios) {
    this.solicitudes = solicitudes;
    this.casos = casos;
    this.historial = historial;
    this.perfiles = perfiles;
    this.usuarios = usuarios;
  }

  /**
   * A quién puede reportar la sesión, si la solicitud lo admite y qué presentó si ya reportó.
   *
   * <p>Se consulta en cualquier estado de la solicitud: en una que nunca se aceptó la respuesta
   * dice que no es reportable y que no puede reportar, que es exactamente lo que la interfaz
   * necesita para no ofrecer una acción falsa.
   *
   * <p>Devuelve el caso propio y solo el propio. El que la contraparte haya presentado sobre la
   * misma solicitud no aparece por ningún lado, ni siquiera como indicio de que existe.
   *
   * @throws ErrorDeAplicacion 404 si la solicitud no existe o el sujeto no participa en ella
   */
  @Transactional(readOnly = true)
  public EstadoDeReporte consultar(UsuarioAutenticado sujeto, Long idSolicitudServicio) {
    ParticipacionEnSolicitud participacion =
        solicitudes.participacionDe(sujeto, idSolicitudServicio);

    boolean reportaElCliente = participacion.esCliente(sujeto.idUsuario());
    Long idReportado = idReportado(participacion, reportaElCliente);
    String nombreReportado = nombreDe(idReportado, reportaElCliente);

    Optional<CasoModeracion> propio =
        casos.findByIdSolicitudServicioAndIdReportante(idSolicitudServicio, sujeto.idUsuario());

    return new EstadoDeReporte(
        idSolicitudServicio,
        participacion.llegoAAceptada(),
        idReportado,
        nombreReportado,
        participacion.llegoAAceptada() && propio.isEmpty(),
        propio.map(caso -> DatosDeCasoModeracion.de(caso, nombreReportado)).orElse(null));
  }

  /**
   * Abre el caso de la sesión sobre la contraparte y crea su primera versión histórica.
   *
   * <p>Las dos filas se confirman o se revierten juntas: un caso sin versión inicial dejaría un
   * expediente sin fotografía de partida, y una versión sin caso no podría existir por su clave
   * foránea. Por eso la transacción es una sola y el caso se vacía a la base antes de construir la
   * versión, que necesita su identificador.
   *
   * <p>No bloquea la fila de la solicitud. Haber llegado a {@code ACEPTADA} es una condición que
   * una vez cierta ya no deja de serlo, así que ninguna transición simultánea puede invalidarla
   * bajo los pies de esta transacción. Lo que sí necesita arbitrarse es que dos envíos de la misma
   * persona no dejen dos casos, y de eso se encarga la unicidad de la base.
   *
   * @throws ErrorDeAplicacion 404 si el sujeto no participa; 409 {@code SOLICITUD_NO_REPORTABLE} si
   *     la solicitud nunca llegó a aceptarse, o {@code REPORTE_DUPLICADO} si esta persona ya abrió
   *     su caso
   */
  @Transactional
  public DatosDeCasoModeracion reportar(
      UsuarioAutenticado sujeto, Long idSolicitudServicio, ReporteAPresentar pedido) {

    ParticipacionEnSolicitud participacion =
        solicitudes.participacionDe(sujeto, idSolicitudServicio);

    if (!participacion.llegoAAceptada()) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "SOLICITUD_NO_REPORTABLE",
          "Solo puedes reportar desde una solicitud que llegó a estar aceptada.");
    }
    if (casos.existsByIdSolicitudServicioAndIdReportante(idSolicitudServicio, sujeto.idUsuario())) {
      throw yaReportada();
    }

    boolean reportaElCliente = participacion.esCliente(sujeto.idUsuario());
    Long idReportado = idReportado(participacion, reportaElCliente);
    // El mismo reloj para la apertura del caso y para la vigencia de su primera
    // versión: si cada uno leyera el suyo, el historial podría empezar antes o
    // después de existir el expediente que describe.
    OffsetDateTime instante = OffsetDateTime.now();

    CasoModeracion caso =
        new CasoModeracion(
            idSolicitudServicio,
            sujeto.idUsuario(),
            idReportado,
            pedido.motivo(),
            pedido.descripcion(),
            instante);

    CasoModeracion abierto;
    try {
      abierto = casos.saveAndFlush(caso);
    } catch (DataIntegrityViolationException yaExistia) {
      // La comprobación previa no basta: dos envíos simultáneos pueden leer los
      // dos que todavía no hay caso. La unicidad de PostgreSQL decide cuál se
      // queda, y el otro sale por aquí como conflicto y no como fallo.
      throw yaReportada();
    }

    // El estado de la cuenta reportada se lee, no se cambia: la versión
    // fotografía lo que había en el instante de la apertura.
    EstadoCuenta estadoCuentaReportada = usuarios.obtener(idReportado).estadoCuenta();
    historial.save(
        HistorialCaso.deAperturaDe(abierto, estadoCuentaReportada, DETALLE_DE_APERTURA, instante));

    return DatosDeCasoModeracion.de(abierto, nombreDe(idReportado, reportaElCliente));
  }

  private static Long idReportado(
      ParticipacionEnSolicitud participacion, boolean reportaElCliente) {
    return reportaElCliente ? participacion.idPrestador() : participacion.idCliente();
  }

  /**
   * El nombre con el que se presenta a la contraparte.
   *
   * <p>Es el mismo que ya viaja en el detalle de la solicitud —{@code nombrePublico} del perfil
   * para el prestador, {@code nombreCompleto} para el cliente—, así que no revela nada nuevo.
   */
  private String nombreDe(Long idReportado, boolean reportaElCliente) {
    if (reportaElCliente) {
      return perfiles
          .resumirPerfil(idReportado)
          .orElseThrow(
              () -> new IllegalStateException("El prestador " + idReportado + " no tiene perfil"))
          .nombrePublico();
    }
    return usuarios.obtener(idReportado).nombreCompleto();
  }

  private static ErrorDeAplicacion yaReportada() {
    return new ErrorDeAplicacion(
        HttpStatus.CONFLICT,
        "REPORTE_DUPLICADO",
        "Ya reportaste esta solicitud. Tu caso sigue abierto y no se presenta dos veces.");
  }
}
