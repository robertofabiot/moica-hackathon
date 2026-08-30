package com.moica.solicitud.dto;

import com.moica.solicitud.entity.EstadoSolicitud;
import com.moica.solicitud.entity.SolicitudServicio;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Fila de una bandeja: lo suficiente para reconocer la solicitud sin su historial. */
public record ResumenDeSolicitudServicio(
    Long idSolicitudServicio,
    Long idServicioPublicado,
    String nombreServicio,
    Long idCliente,
    String nombreCliente,
    Long idPrestador,
    String nombrePublicoPrestador,
    Integer idMunicipio,
    String nombreMunicipio,
    EstadoSolicitud estadoActual,
    LocalDate fechaPreferida,
    OffsetDateTime fechaCreacion) {

  public static ResumenDeSolicitudServicio de(
      SolicitudServicio solicitud,
      String nombreServicio,
      String nombreCliente,
      Long idPrestador,
      String nombrePublicoPrestador,
      String nombreMunicipio) {
    return new ResumenDeSolicitudServicio(
        solicitud.getIdSolicitudServicio(),
        solicitud.getIdServicioPublicado(),
        nombreServicio,
        solicitud.getIdCliente(),
        nombreCliente,
        idPrestador,
        nombrePublicoPrestador,
        solicitud.getIdMunicipio(),
        nombreMunicipio,
        solicitud.getEstadoActual(),
        solicitud.getFechaPreferida(),
        solicitud.getFechaCreacion());
  }
}
