package com.moica.solicitud.dto;

import com.moica.solicitud.entity.CambioEstadoSolicitud;
import com.moica.solicitud.entity.EstadoSolicitud;
import java.time.OffsetDateTime;

/** Una entrada del historial, sin correo ni datos internos del actor. */
public record DatosDeCambioEstadoSolicitud(
    Long idCambioEstadoSolicitud,
    EstadoSolicitud estadoAnterior,
    EstadoSolicitud estadoNuevo,
    Long idActor,
    String nombreActor,
    String motivo,
    OffsetDateTime fechaCambio) {

  public static DatosDeCambioEstadoSolicitud de(CambioEstadoSolicitud cambio, String nombreActor) {
    return new DatosDeCambioEstadoSolicitud(
        cambio.getIdCambioEstadoSolicitud(),
        cambio.getEstadoAnterior(),
        cambio.getEstadoNuevo(),
        cambio.getIdActor(),
        nombreActor,
        cambio.getMotivo(),
        cambio.getFechaCambio());
  }
}
