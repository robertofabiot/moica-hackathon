package com.moica.calificacion.dto;

import com.moica.calificacion.entity.RolCalificado;

/**
 * Qué puede hacer la sesión con la calificación de una solicitud en la que participa.
 *
 * <p>Existe para que la interfaz no tenga que deducir la regla: le decimos a quién califica, en qué
 * rol, si todavía puede hacerlo y qué escribió si ya lo hizo. Así el formulario, el resumen
 * inmutable y la ausencia de ambos se deciden con un solo dato del servidor y no reconstruyendo el
 * permiso en el navegador.
 *
 * <p>{@code idCalificado}, {@code nombreCalificado} y {@code rolCalificado} describen siempre a la
 * contraparte, esté abierta o cerrada la calificación: son lo que permite escribir «Calificas a X
 * como prestador» junto al formulario y junto al resumen.
 *
 * @param solicitudCompletada si la solicitud llegó a {@code COMPLETADA}, único estado que habilita
 *     calificar
 * @param puedeCalificar si esta sesión puede emitir la calificación ahora mismo: la solicitud está
 *     completada, todavía no calificó y su cuenta está {@code ACTIVA}. Ocultar el formulario no es
 *     el control de seguridad; el envío se comprueba igual en el servidor
 * @param calificacionEmitida lo que la sesión ya calificó, o {@code null} si aún no calificó
 */
public record EstadoDeCalificacion(
    Long idSolicitudServicio,
    boolean solicitudCompletada,
    Long idCalificado,
    String nombreCalificado,
    RolCalificado rolCalificado,
    boolean puedeCalificar,
    DatosDeCalificacion calificacionEmitida) {}
