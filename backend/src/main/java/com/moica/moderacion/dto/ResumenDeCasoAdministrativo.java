package com.moica.moderacion.dto;

import com.moica.moderacion.entity.CasoModeracion;
import com.moica.moderacion.entity.EstadoCasoModeracion;
import com.moica.moderacion.entity.ResultadoCasoModeracion;
import java.time.OffsetDateTime;

/**
 * Una fila de la bandeja administrativa de casos.
 *
 * <p>Lleva lo justo para priorizar y elegir: quién reportó a quién, por qué motivo, en qué etapa
 * está, quién responde por él y desde cuándo espera. La descripción del reporte, el hilo de
 * mensajes y el historial no viajan aquí: son del expediente, y una bandeja no necesita el relato
 * completo de cada caso para ordenarlos.
 *
 * @param nombreAdministradorResponsable nulo mientras nadie lo tenga asignado
 * @param resultadoActual solo lo lleva un caso cerrado; su resolución se lee en el expediente
 */
public record ResumenDeCasoAdministrativo(
    Long idCasoModeracion,
    Long idSolicitudServicio,
    Long idReportante,
    String nombreReportante,
    Long idReportado,
    String nombreReportado,
    String motivo,
    EstadoCasoModeracion estadoActual,
    ResultadoCasoModeracion resultadoActual,
    Long idAdministradorResponsable,
    String nombreAdministradorResponsable,
    OffsetDateTime fechaApertura,
    OffsetDateTime fechaActualizacion) {

  public static ResumenDeCasoAdministrativo de(
      CasoModeracion caso,
      String nombreReportante,
      String nombreReportado,
      String nombreAdministradorResponsable) {

    return new ResumenDeCasoAdministrativo(
        caso.getIdCasoModeracion(),
        caso.getIdSolicitudServicio(),
        caso.getIdReportante(),
        nombreReportante,
        caso.getIdReportado(),
        nombreReportado,
        caso.getMotivo(),
        caso.getEstadoActual(),
        caso.getResultadoActual(),
        caso.getIdAdministradorResponsable(),
        nombreAdministradorResponsable,
        caso.getFechaApertura(),
        caso.getFechaActualizacion());
  }
}
