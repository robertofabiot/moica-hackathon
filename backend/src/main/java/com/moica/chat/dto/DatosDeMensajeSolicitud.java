package com.moica.chat.dto;

import com.moica.chat.entity.MensajeSolicitud;
import java.time.OffsetDateTime;

/**
 * Un mensaje del hilo, con lo justo para pintarlo y saber quién lo escribió.
 *
 * <p>El nombre del remitente es el mismo {@code nombreCompleto} que ya viaja en el historial de la
 * solicitud, así que no revela nada nuevo. No lleva correo, estado de cuenta ni ningún otro dato
 * administrativo de la persona.
 */
public record DatosDeMensajeSolicitud(
    Long idMensajeSolicitud,
    Long idSolicitudServicio,
    Long idRemitente,
    String nombreRemitente,
    String contenido,
    OffsetDateTime fechaEnvio) {

  public static DatosDeMensajeSolicitud de(MensajeSolicitud mensaje, String nombreRemitente) {
    return new DatosDeMensajeSolicitud(
        mensaje.getIdMensajeSolicitud(),
        mensaje.getIdSolicitudServicio(),
        mensaje.getIdRemitente(),
        nombreRemitente,
        mensaje.getContenido(),
        mensaje.getFechaEnvio());
  }
}
