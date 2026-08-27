package com.moica.verificacion.dto;

import com.moica.verificacion.entity.EstadoSolicitudVerificacion;
import com.moica.verificacion.entity.NivelVerificacionSolicitado;
import com.moica.verificacion.entity.SolicitudVerificacionPrestador;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Una solicitud de verificación tal como la ve su propietario.
 *
 * <p>Lleva el estado, las fechas, el motivo de una decisión negativa y los metadatos de su
 * expediente. Deja fuera **quién** la revisó: al prestador le corresponde saber qué se decidió y
 * por qué, no qué persona del equipo lo decidió.
 *
 * @param observacionResolucion el motivo del rechazo o de la revocación; {@code null} en el resto
 *     de estados
 */
public record DatosDeSolicitudVerificacion(
    Long idSolicitudVerificacion,
    NivelVerificacionSolicitado nivelSolicitado,
    EstadoSolicitudVerificacion estadoSolicitud,
    String observacionResolucion,
    OffsetDateTime fechaSolicitud,
    OffsetDateTime fechaInicioRevision,
    OffsetDateTime fechaResolucion,
    List<DatosDeDocumentoVerificacion> documentos) {

  public DatosDeSolicitudVerificacion {
    documentos = List.copyOf(documentos);
  }

  public static DatosDeSolicitudVerificacion de(
      SolicitudVerificacionPrestador solicitud, List<DatosDeDocumentoVerificacion> documentos) {
    return new DatosDeSolicitudVerificacion(
        solicitud.getIdSolicitudVerificacion(),
        solicitud.getNivelSolicitado(),
        solicitud.getEstadoSolicitud(),
        solicitud.getObservacionResolucion(),
        solicitud.getFechaSolicitud(),
        solicitud.getFechaInicioRevision(),
        solicitud.getFechaResolucion(),
        documentos);
  }
}
