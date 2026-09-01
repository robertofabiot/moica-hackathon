package com.moica.calificacion.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.calificacion.dto.CalificacionAEmitir;
import com.moica.calificacion.dto.DatosDeCalificacion;
import com.moica.calificacion.dto.EstadoDeCalificacion;
import com.moica.calificacion.dto.ReputacionPorRol;
import com.moica.calificacion.entity.CalificacionUsuario;
import com.moica.calificacion.entity.RolCalificado;
import com.moica.calificacion.repository.CalificacionUsuarioRepository;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.prestador.service.PerfilPrestadorService;
import com.moica.solicitud.dto.ParticipacionEnSolicitud;
import com.moica.solicitud.entity.EstadoSolicitud;
import com.moica.solicitud.service.SolicitudServicioService;
import com.moica.usuario.entity.EstadoCuenta;
import com.moica.usuario.service.UsuarioService;
import java.time.OffsetDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La calificación que cada participante puede emitir sobre el otro al completar una solicitud.
 *
 * <p>No decide por su cuenta quién participa ni en qué estado está el compromiso: se lo pregunta a
 * {@link SolicitudServicioService}, que es dueño de esa regla, igual que hace la capacidad {@code
 * chat}. Aquí queda lo propio de calificar.
 *
 * <p>Cuatro reglas gobiernan todo lo que sigue:
 *
 * <ul>
 *   <li>Un tercero recibe 404, igual que en el resto de recursos propios: no puede confirmar
 *       siquiera que la solicitud exista.
 *   <li>Solo se califica una solicitud {@code COMPLETADA}. Es el único estado que cierra el
 *       servicio, y es definitivo: una vez alcanzado no se reabre.
 *   <li><b>El calificado y su rol salen de la solicitud, nunca del cuerpo.</b> El cliente solo
 *       puede calificar al prestador como {@code PRESTADOR} y el prestador solo puede calificar al
 *       cliente como {@code CLIENTE}. Calificarse a sí misma tampoco es una petición formulable:
 *       una solicitud sobre un servicio propio ya se rechaza al crearse, y {@code
 *       ck_calificacion_usuario_participantes} lo respalda en la base.
 *   <li>Cada participante califica una sola vez. La comprobación previa cubre el caso normal y
 *       {@code uq_calificacion_usuario_solicitud_calificador} decide la carrera entre dos envíos
 *       simultáneos.
 * </ul>
 *
 * <p>Calificar es opcional y no calificar no penaliza: no hay recordatorio, plazo ni efecto sobre
 * ningún promedio.
 */
@Service
public class CalificacionDeSolicitudService {

  private final SolicitudServicioService solicitudes;
  private final CalificacionUsuarioRepository calificaciones;
  private final ReputacionService reputaciones;
  private final PerfilPrestadorService perfiles;
  private final UsuarioService usuarios;

  public CalificacionDeSolicitudService(
      SolicitudServicioService solicitudes,
      CalificacionUsuarioRepository calificaciones,
      ReputacionService reputaciones,
      PerfilPrestadorService perfiles,
      UsuarioService usuarios) {
    this.solicitudes = solicitudes;
    this.calificaciones = calificaciones;
    this.reputaciones = reputaciones;
    this.perfiles = perfiles;
    this.usuarios = usuarios;
  }

  /**
   * A quién califica la sesión, en qué rol, si todavía puede y qué escribió si ya calificó.
   *
   * <p>Se consulta en cualquier estado de la solicitud: antes de completarla la respuesta dice que
   * no está completada y que no puede calificar, que es exactamente lo que la interfaz necesita
   * para no ofrecer una acción falsa. Una cuenta restringida también la consulta; lo que no puede
   * es enviar.
   *
   * @throws ErrorDeAplicacion 404 si la solicitud no existe o el sujeto no participa en ella
   */
  @Transactional(readOnly = true)
  public EstadoDeCalificacion consultar(UsuarioAutenticado sujeto, Long idSolicitudServicio) {
    ParticipacionEnSolicitud participacion =
        solicitudes.participacionDe(sujeto, idSolicitudServicio);

    boolean calificaElCliente = participacion.esCliente(sujeto.idUsuario());
    Long idCalificado = idCalificado(participacion, calificaElCliente);
    RolCalificado rol = RolCalificado.contrapartaDe(calificaElCliente);

    DatosDeCalificacion emitida =
        calificaciones
            .findByIdSolicitudServicioAndIdCalificador(idSolicitudServicio, sujeto.idUsuario())
            .map(DatosDeCalificacion::de)
            .orElse(null);

    boolean completada = participacion.estadoActual() == EstadoSolicitud.COMPLETADA;

    return new EstadoDeCalificacion(
        idSolicitudServicio,
        completada,
        idCalificado,
        nombreDe(idCalificado, rol),
        rol,
        completada && emitida == null && sujeto.estadoCuenta() == EstadoCuenta.ACTIVA,
        emitida);
  }

  /**
   * Registra la calificación de la sesión sobre la contraparte.
   *
   * <p>No bloquea la fila de la solicitud, a diferencia de enviar un mensaje: {@code COMPLETADA} es
   * un estado definitivo, así que una vez leído no puede cambiar bajo los pies de esta transacción.
   * Lo que sí necesita arbitrarse es que dos envíos de la misma persona no dejen dos filas, y de
   * eso se encarga la unicidad de la base.
   *
   * @throws ErrorDeAplicacion 403 si la cuenta no está activa; 404 si el sujeto no participa; 409
   *     {@code SOLICITUD_NO_COMPLETADA} o {@code CALIFICACION_DUPLICADA} según el caso
   */
  @Transactional
  public DatosDeCalificacion calificar(
      UsuarioAutenticado sujeto, Long idSolicitudServicio, CalificacionAEmitir pedido) {

    if (sujeto.estadoCuenta() != EstadoCuenta.ACTIVA) {
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "CUENTA_RESTRINGIDA",
          "Tu cuenta está restringida y por ahora no puede calificar.");
    }

    ParticipacionEnSolicitud participacion =
        solicitudes.participacionDe(sujeto, idSolicitudServicio);

    if (participacion.estadoActual() != EstadoSolicitud.COMPLETADA) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "SOLICITUD_NO_COMPLETADA",
          "Podrás calificar cuando el prestador marque la solicitud como completada.");
    }
    if (calificaciones.existsByIdSolicitudServicioAndIdCalificador(
        idSolicitudServicio, sujeto.idUsuario())) {
      throw yaCalificada();
    }

    boolean calificaElCliente = participacion.esCliente(sujeto.idUsuario());
    CalificacionUsuario calificacion =
        new CalificacionUsuario(
            idSolicitudServicio,
            sujeto.idUsuario(),
            idCalificado(participacion, calificaElCliente),
            RolCalificado.contrapartaDe(calificaElCliente),
            pedido.puntuacion(),
            pedido.comentario(),
            OffsetDateTime.now());

    try {
      return DatosDeCalificacion.de(calificaciones.saveAndFlush(calificacion));
    } catch (DataIntegrityViolationException yaExistia) {
      // La comprobación previa no basta: dos envíos simultáneos pueden leer los
      // dos que todavía no hay calificación. La unicidad de PostgreSQL decide
      // cuál se queda, y el otro sale por aquí como conflicto y no como fallo.
      throw yaCalificada();
    }
  }

  /**
   * La reputación como cliente de la persona que contrató, para el prestador participante.
   *
   * <p>Es una superficie propia y no un campo del detalle de la solicitud, por la misma razón que
   * la revelación de contactos: así la reputación como cliente no viaja por descuido en las
   * bandejas ni en ninguna pantalla pública. Los perfiles de cliente no son públicos y este
   * incremento no los convierte en tales.
   *
   * <p>Responde a una sola persona: el prestador destinatario. El cliente recibe 404 —su propia
   * reputación como cliente no se le publica aquí— y un tercero también, igual que en el resto de
   * recursos propios.
   *
   * <p>No exige que la solicitud esté completada. Quien va a decidir si acepta un encargo es
   * precisamente quien más necesita ese dato, y llega por una solicitud que ya le pertenece.
   *
   * @throws ErrorDeAplicacion 404 si quien pregunta no es el prestador participante
   */
  @Transactional(readOnly = true)
  public ReputacionPorRol reputacionDelCliente(
      UsuarioAutenticado sujeto, Long idSolicitudServicio) {

    ParticipacionEnSolicitud participacion =
        solicitudes.participacionDe(sujeto, idSolicitudServicio);

    if (!participacion.esPrestador(sujeto.idUsuario())) {
      throw new ErrorDeAplicacion(
          HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "Esa solicitud no existe.");
    }

    return reputaciones.reputacionDe(participacion.idCliente(), RolCalificado.CLIENTE);
  }

  private static Long idCalificado(
      ParticipacionEnSolicitud participacion, boolean calificaElCliente) {
    return calificaElCliente ? participacion.idPrestador() : participacion.idCliente();
  }

  /**
   * El nombre con el que se presenta a la contraparte.
   *
   * <p>Es el mismo que ya viaja en el detalle de la solicitud —{@code nombrePublico} del perfil
   * para el prestador, {@code nombreCompleto} para el cliente—, así que no revela nada nuevo.
   */
  private String nombreDe(Long idCalificado, RolCalificado rol) {
    if (rol == RolCalificado.PRESTADOR) {
      return perfiles
          .resumirPerfil(idCalificado)
          .orElseThrow(
              () -> new IllegalStateException("El prestador " + idCalificado + " no tiene perfil"))
          .nombrePublico();
    }
    return usuarios.obtener(idCalificado).nombreCompleto();
  }

  private static ErrorDeAplicacion yaCalificada() {
    return new ErrorDeAplicacion(
        HttpStatus.CONFLICT,
        "CALIFICACION_DUPLICADA",
        "Ya calificaste esta solicitud. Las calificaciones no se editan.");
  }
}
