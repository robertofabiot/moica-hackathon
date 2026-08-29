package com.moica.solicitud.dto;

import com.moica.solicitud.entity.EstadoSolicitud;
import com.moica.solicitud.entity.SolicitudServicio;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Detalle de una solicitud para sus dos participantes.
 *
 * <p>Incluye la ubicación escrita y el historial cronológico. No lleva correos, contactos externos,
 * documentos ni secretos: aceptar deja el estado listo para revelarlos en un incremento posterior.
 */
public record DatosDeSolicitudServicio(
    Long idSolicitudServicio,
    Long idServicioPublicado,
    String nombreServicio,
    Long idCliente,
    String nombreCliente,
    Long idPrestador,
    String nombrePublicoPrestador,
    Integer idMunicipio,
    String nombreMunicipio,
    String nombreDepartamento,
    String descripcionNecesidad,
    String indicacionUbicacion,
    LocalDate fechaPreferida,
    EstadoSolicitud estadoActual,
    OffsetDateTime fechaCreacion,
    OffsetDateTime fechaActualizacion,
    List<DatosDeCambioEstadoSolicitud> historial) {

  public DatosDeSolicitudServicio {
    historial = List.copyOf(historial);
  }

  public static DatosDeSolicitudServicio de(
      SolicitudServicio solicitud,
      String nombreServicio,
      String nombreCliente,
      Long idPrestador,
      String nombrePublicoPrestador,
      String nombreMunicipio,
      String nombreDepartamento,
      List<DatosDeCambioEstadoSolicitud> historial) {
    return new DatosDeSolicitudServicio(
        solicitud.getIdSolicitudServicio(),
        solicitud.getIdServicioPublicado(),
        nombreServicio,
        solicitud.getIdCliente(),
        nombreCliente,
        idPrestador,
        nombrePublicoPrestador,
        solicitud.getIdMunicipio(),
        nombreMunicipio,
        nombreDepartamento,
        solicitud.getDescripcionNecesidad(),
        solicitud.getIndicacionUbicacion(),
        solicitud.getFechaPreferida(),
        solicitud.getEstadoActual(),
        solicitud.getFechaCreacion(),
        solicitud.getFechaActualizacion(),
        historial);
  }
}
