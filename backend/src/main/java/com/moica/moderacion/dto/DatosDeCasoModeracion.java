package com.moica.moderacion.dto;

import com.moica.moderacion.entity.CasoModeracion;
import com.moica.moderacion.entity.EstadoCasoModeracion;
import java.time.OffsetDateTime;

/**
 * El caso que la sesión abrió, tal como se lo devolvemos a quien lo presentó.
 *
 * <p>Lleva lo justo para que el reportante reconozca su expediente: a quién reportó, qué escribió,
 * en qué etapa va la revisión y cuándo lo abrió.
 *
 * <p>Lo administrativo se queda fuera a propósito. No viajan el administrador responsable, la
 * medida vinculada, el resultado, la resolución ni las fechas de cierre y de fin de medida: son la
 * decisión de Moica sobre una persona, no el acuse del reporte, y su superficie es el área
 * administrativa de P10A. Tampoco viaja nada del caso que la contraparte haya podido presentar.
 *
 * @param estadoActual etapa vigente de la revisión; recién abierto es siempre {@link
 *     EstadoCasoModeracion#ABIERTO}
 */
public record DatosDeCasoModeracion(
    Long idCasoModeracion,
    Long idSolicitudServicio,
    Long idReportado,
    String nombreReportado,
    String motivo,
    String descripcion,
    EstadoCasoModeracion estadoActual,
    OffsetDateTime fechaApertura) {

  public static DatosDeCasoModeracion de(CasoModeracion caso, String nombreReportado) {
    return new DatosDeCasoModeracion(
        caso.getIdCasoModeracion(),
        caso.getIdSolicitudServicio(),
        caso.getIdReportado(),
        nombreReportado,
        caso.getMotivo(),
        caso.getDescripcion(),
        caso.getEstadoActual(),
        caso.getFechaApertura());
  }
}
