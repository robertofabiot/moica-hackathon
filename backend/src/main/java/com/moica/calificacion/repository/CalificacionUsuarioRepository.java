package com.moica.calificacion.repository;

import com.moica.calificacion.dto.AgregadoDeCalificaciones;
import com.moica.calificacion.entity.CalificacionUsuario;
import com.moica.calificacion.entity.RolCalificado;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalificacionUsuarioRepository extends JpaRepository<CalificacionUsuario, Long> {

  /** La calificación que una persona emitió en una solicitud, si la emitió. */
  Optional<CalificacionUsuario> findByIdSolicitudServicioAndIdCalificador(
      Long idSolicitudServicio, Long idCalificador);

  boolean existsByIdSolicitudServicioAndIdCalificador(Long idSolicitudServicio, Long idCalificador);

  /**
   * Promedio, cantidad y desglose de varias personas en un mismo rol, en una sola consulta.
   *
   * <p>Acepta una colección y no un identificador a propósito: una pantalla de exploración pinta
   * muchas tarjetas del mismo o de distintos prestadores, y resolver la reputación tarjeta por
   * tarjeta añadiría una consulta por tarjeta. Para una sola persona se le pasa una colección de
   * uno, de modo que no haya dos formas de calcular lo mismo.
   *
   * <p>Solo aparecen en el resultado las personas con al menos una calificación: la agrupación no
   * inventa filas. Quien falte no tiene reputación todavía, y eso lo resuelve el servicio.
   *
   * <p>El índice {@code ix_calificacion_usuario_calificado_rol} está declarado con estas mismas
   * columnas más la puntuación, así que el recorrido no vuelve a la tabla.
   */
  @Query(
      """
      SELECT new com.moica.calificacion.dto.AgregadoDeCalificaciones(
        calificacion.idCalificado,
        AVG(calificacion.puntuacion),
        COUNT(calificacion),
        SUM(CASE WHEN calificacion.puntuacion = 1 THEN 1L ELSE 0L END),
        SUM(CASE WHEN calificacion.puntuacion = 2 THEN 1L ELSE 0L END),
        SUM(CASE WHEN calificacion.puntuacion = 3 THEN 1L ELSE 0L END),
        SUM(CASE WHEN calificacion.puntuacion = 4 THEN 1L ELSE 0L END),
        SUM(CASE WHEN calificacion.puntuacion = 5 THEN 1L ELSE 0L END))
      FROM CalificacionUsuario calificacion
      WHERE calificacion.rolCalificado = :rolCalificado
        AND calificacion.idCalificado IN :idsCalificado
      GROUP BY calificacion.idCalificado
      """)
  List<AgregadoDeCalificaciones> agregarPorCalificado(
      @Param("rolCalificado") RolCalificado rolCalificado,
      @Param("idsCalificado") Collection<Long> idsCalificado);
}
