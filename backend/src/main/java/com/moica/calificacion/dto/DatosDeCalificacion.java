package com.moica.calificacion.dto;

import com.moica.calificacion.entity.CalificacionUsuario;
import com.moica.calificacion.entity.RolCalificado;
import java.time.OffsetDateTime;

/**
 * Una calificación ya emitida, tal como se la devolvemos a quien la escribió.
 *
 * <p>Se entrega para que la interfaz pueda mostrar un estado final honesto —esto fue lo que
 * pusiste, y no se puede cambiar— en lugar de un formulario vacío tras recargar la página.
 *
 * <p>No lleva nombres ni correos: es la calificación de la sesión sobre una solicitud que la sesión
 * ya tiene abierta. Tampoco se publica en ninguna superficie pública; allí solo viaja el agregado.
 */
public record DatosDeCalificacion(
    Long idCalificacionUsuario,
    Long idSolicitudServicio,
    Long idCalificador,
    Long idCalificado,
    RolCalificado rolCalificado,
    short puntuacion,
    String comentario,
    OffsetDateTime fechaCreacion) {

  public static DatosDeCalificacion de(CalificacionUsuario calificacion) {
    return new DatosDeCalificacion(
        calificacion.getIdCalificacionUsuario(),
        calificacion.getIdSolicitudServicio(),
        calificacion.getIdCalificador(),
        calificacion.getIdCalificado(),
        calificacion.getRolCalificado(),
        calificacion.getPuntuacion(),
        calificacion.getComentario(),
        calificacion.getFechaCreacion());
  }
}
