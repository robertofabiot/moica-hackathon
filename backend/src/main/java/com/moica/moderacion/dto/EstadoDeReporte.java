package com.moica.moderacion.dto;

/**
 * Qué puede hacer la sesión con el reporte de una solicitud en la que participa.
 *
 * <p>Existe por la misma razón que {@code EstadoDeCalificacion}: para que la interfaz no tenga que
 * deducir la regla. Le decimos a quién puede reportar, si la solicitud lo admite y qué presentó si
 * ya reportó, de modo que el formulario, el resumen del caso propio y la ausencia de ambos se
 * decidan con un solo dato del servidor.
 *
 * <p>{@code idReportado} y {@code nombreReportado} describen siempre a la contraparte, haya reporte
 * o no: son lo que permite escribir «Reportas a X» junto al formulario y junto al resumen. No
 * revelan nada nuevo, porque son los mismos datos que ya viajan en el detalle de la solicitud.
 *
 * @param solicitudReportable si la solicitud llegó alguna vez a estar aceptada, único requisito de
 *     estado para reportar. Una {@code COMPLETADA} y una {@code CANCELADA} que antes fue aceptada
 *     lo siguen siendo; una {@code PENDIENTE}, una {@code RECHAZADA} y una {@code CANCELADA} desde
 *     pendiente no
 * @param puedeReportar si esta sesión puede abrir un caso ahora mismo: la solicitud es reportable y
 *     todavía no presentó el suyo. Ocultar el formulario no es el control de seguridad; el envío se
 *     comprueba igual en el servidor
 * @param casoAbierto el caso que la sesión presentó, o {@code null} si aún no reportó. Nunca es el
 *     de la contraparte
 */
public record EstadoDeReporte(
    Long idSolicitudServicio,
    boolean solicitudReportable,
    Long idReportado,
    String nombreReportado,
    boolean puedeReportar,
    DatosDeCasoModeracion casoAbierto) {}
