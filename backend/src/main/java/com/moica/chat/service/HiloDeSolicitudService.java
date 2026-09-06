package com.moica.chat.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.chat.dto.DatosDeMensajeSolicitud;
import com.moica.chat.dto.MensajeAEnviar;
import com.moica.chat.entity.MensajeSolicitud;
import com.moica.chat.repository.MensajeSolicitudRepository;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.solicitud.dto.ParticipacionEnSolicitud;
import com.moica.solicitud.service.SolicitudServicioService;
import com.moica.usuario.entity.EstadoCuenta;
import com.moica.usuario.service.UsuarioService;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El hilo de mensajes de una solicitud aceptada.
 *
 * <p>No hay entidad {@code Conversacion}: el hilo es la solicitud. Por eso este servicio no decide
 * por su cuenta quién participa ni en qué estado está el compromiso: se lo pregunta a {@link
 * SolicitudServicioService}, que es dueño de esa regla, y aquí solo queda lo propio del chat.
 *
 * <p>Tres reglas gobiernan todo lo que sigue:
 *
 * <ul>
 *   <li>Un tercero recibe 404, igual que en el resto de recursos propios: no puede confirmar
 *       siquiera que el hilo exista.
 *   <li>El hilo existe desde que la solicitud fue aceptada **alguna vez**. Una cancelación desde
 *       {@code PENDIENTE} nunca lo abre; una posterior a la aceptación lo deja en solo lectura.
 *   <li>El remitente sale siempre de la sesión. El cuerpo de la petición no lo lleva.
 * </ul>
 */
@Service
public class HiloDeSolicitudService {

  private final SolicitudServicioService solicitudes;
  private final MensajeSolicitudRepository mensajes;
  private final UsuarioService usuarios;

  public HiloDeSolicitudService(
      SolicitudServicioService solicitudes,
      MensajeSolicitudRepository mensajes,
      UsuarioService usuarios) {
    this.solicitudes = solicitudes;
    this.mensajes = mensajes;
    this.usuarios = usuarios;
  }

  /**
   * El hilo completo, en orden cronológico, para cualquiera de los dos participantes.
   *
   * <p>Se lee igual esté la solicitud {@code ACEPTADA}, {@code CANCELADA} tras aceptarse o {@code
   * COMPLETADA}: lo que cambia en esos dos últimos casos es que ya no se puede escribir. Una cuenta
   * restringida conserva esta lectura.
   *
   * @throws ErrorDeAplicacion 404 si el sujeto no participa; 409 {@code CHAT_NO_HABILITADO} si la
   *     solicitud nunca llegó a aceptarse
   */
  @Transactional(readOnly = true)
  public List<DatosDeMensajeSolicitud> listarMensajes(
      UsuarioAutenticado sujeto, Long idSolicitudServicio) {

    ParticipacionEnSolicitud participacion =
        solicitudes.participacionDe(sujeto, idSolicitudServicio);
    exigirHiloHabilitado(participacion);

    Map<Long, String> nombres = new HashMap<>();
    return mensajes
        .findByIdSolicitudServicioOrderByFechaEnvioAscIdMensajeSolicitudAsc(idSolicitudServicio)
        .stream()
        .map(mensaje -> DatosDeMensajeSolicitud.de(mensaje, nombreDe(nombres, mensaje)))
        .toList();
  }

  /**
   * El hilo completo de una solicitud, para el expediente de un caso de moderación.
   *
   * <p>Devuelve exactamente lo mismo que ven los dos participantes. Se distingue de {@link
   * #listarMensajes} en que no exige participación —quien revisa un caso no participa en la
   * solicitud reportada— y en que no exige que el hilo esté habilitado: un caso solo puede nacer de
   * una solicitud que llegó a aceptarse, así que el hilo existe, y si estuviera vacío la respuesta
   * vacía es la información correcta.
   *
   * <p><b>No autoriza.</b> Quien lo invoca ya comprobó rol administrativo, segundo factor y que el
   * caso que ampara la lectura existe. No recibe sujeto a propósito: pedírselo sugeriría que decide
   * con él, y no lo hace.
   *
   * <p>Solo lee: no hay ninguna vía para escribir en el hilo desde el área administrativa.
   */
  @Transactional(readOnly = true)
  public List<DatosDeMensajeSolicitud> mensajesParaModeracion(Long idSolicitudServicio) {
    Map<Long, String> nombres = new HashMap<>();
    return mensajes
        .findByIdSolicitudServicioOrderByFechaEnvioAscIdMensajeSolicitudAsc(idSolicitudServicio)
        .stream()
        .map(mensaje -> DatosDeMensajeSolicitud.de(mensaje, nombreDe(nombres, mensaje)))
        .toList();
  }

  /**
   * Agrega un mensaje al hilo de una solicitud {@code ACEPTADA}.
   *
   * <p>La solicitud se bloquea antes de comprobar su estado y no se suelta hasta que la transacción
   * termina, así que este envío y una cancelación o una finalización simultáneas se ponen en cola:
   * o el mensaje queda confirmado antes de la transición, o la transición gana y el envío se
   * rechaza. Nunca aparece un mensaje posterior al cierre.
   *
   * @throws ErrorDeAplicacion 403 si la cuenta no está activa; 404 si el sujeto no participa; 409
   *     {@code CHAT_NO_HABILITADO} o {@code CHAT_SOLO_LECTURA} según el estado
   */
  @Transactional
  public DatosDeMensajeSolicitud enviarMensaje(
      UsuarioAutenticado sujeto, Long idSolicitudServicio, MensajeAEnviar pedido) {

    if (sujeto.estadoCuenta() != EstadoCuenta.ACTIVA) {
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "CUENTA_RESTRINGIDA",
          "Tu cuenta está restringida y por ahora no puede enviar mensajes.");
    }

    ParticipacionEnSolicitud participacion =
        solicitudes.participacionBloqueada(sujeto, idSolicitudServicio);
    exigirHiloHabilitado(participacion);

    if (!participacion.admiteMensajesNuevos()) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "CHAT_SOLO_LECTURA",
          "Esta solicitud ya se cerró: el historial sigue visible, pero no admite mensajes nuevos.");
    }

    MensajeSolicitud mensaje =
        mensajes.save(
            new MensajeSolicitud(
                idSolicitudServicio, sujeto.idUsuario(), pedido.contenido(), OffsetDateTime.now()));

    return DatosDeMensajeSolicitud.de(mensaje, nombreDe(sujeto.idUsuario()));
  }

  private static void exigirHiloHabilitado(ParticipacionEnSolicitud participacion) {
    if (!participacion.llegoAAceptada()) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "CHAT_NO_HABILITADO",
          "El chat se habilita cuando el prestador acepta la solicitud.");
    }
  }

  /**
   * El nombre del remitente, resuelto una sola vez por persona.
   *
   * <p>Un hilo tiene dos participantes y puede tener muchos mensajes: sin esta memoria, pintar el
   * hilo costaría una consulta por mensaje.
   */
  private String nombreDe(Map<Long, String> nombres, MensajeSolicitud mensaje) {
    return nombres.computeIfAbsent(mensaje.getIdRemitente(), this::nombreDe);
  }

  private String nombreDe(Long idUsuario) {
    return usuarios.obtener(idUsuario).nombreCompleto();
  }
}
