package com.moica.calificacion.dto;

import com.moica.calificacion.entity.RolCalificado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Reputación de una persona en uno de los dos roles, calculada desde sus calificaciones.
 *
 * <p>El rol viaja en el propio agregado porque la reputación como cliente y como prestador son
 * cifras distintas de la misma persona y nunca se suman: la definición 10 las separa.
 *
 * <p>{@code promedio} es nulo cuando todavía no hay calificaciones. No se envía {@code 0.0}: una
 * persona sin actividad no tiene una nota pésima, no tiene nota. La interfaz lo presenta como «Sin
 * calificaciones» y no dibuja puntuación.
 *
 * <p>El desglose lleva siempre las cinco filas, de cinco a una estrella, con cero donde no hubo
 * votos. Enviar solo los tramos con actividad obligaría a cada pantalla a reconstruir los que
 * faltan, y el cero es un dato real.
 *
 * @param promedio media redondeada a un decimal, o {@code null} si {@code cantidad} es cero
 * @param cantidad número de calificaciones recibidas en ese rol
 */
public record ReputacionPorRol(
    RolCalificado rol, BigDecimal promedio, long cantidad, List<TramoDeReputacion> desglose) {

  /** Cuántas calificaciones recibió una puntuación concreta. */
  public record TramoDeReputacion(short estrellas, long cantidad) {}

  /** Las puntuaciones posibles, de la más alta a la más baja: el orden en que se presentan. */
  private static final short[] PUNTUACIONES = {5, 4, 3, 2, 1};

  public ReputacionPorRol {
    desglose = List.copyOf(desglose);
  }

  /** El agregado de quien todavía no recibió ninguna calificación en ese rol. */
  public static ReputacionPorRol sinCalificaciones(RolCalificado rol) {
    return new ReputacionPorRol(rol, null, 0L, desgloseDe(0L, 0L, 0L, 0L, 0L));
  }

  /** El agregado que corresponde a una fila de la consulta agrupada. */
  public static ReputacionPorRol de(RolCalificado rol, AgregadoDeCalificaciones agregado) {
    long cantidad = agregado.cantidad();
    if (cantidad == 0L) {
      return sinCalificaciones(rol);
    }
    return new ReputacionPorRol(
        rol,
        // Un decimal, que es como se lee la nota en toda la interfaz. Redondear
        // aquí y no en cada pantalla evita que dos superficies muestren cifras
        // distintas del mismo promedio.
        BigDecimal.valueOf(agregado.promedio()).setScale(1, RoundingMode.HALF_UP),
        cantidad,
        desgloseDe(
            agregado.unaEstrella(),
            agregado.dosEstrellas(),
            agregado.tresEstrellas(),
            agregado.cuatroEstrellas(),
            agregado.cincoEstrellas()));
  }

  private static List<TramoDeReputacion> desgloseDe(
      long una, long dos, long tres, long cuatro, long cinco) {
    long[] porPuntuacion = {cinco, cuatro, tres, dos, una};
    return List.of(
        new TramoDeReputacion(PUNTUACIONES[0], porPuntuacion[0]),
        new TramoDeReputacion(PUNTUACIONES[1], porPuntuacion[1]),
        new TramoDeReputacion(PUNTUACIONES[2], porPuntuacion[2]),
        new TramoDeReputacion(PUNTUACIONES[3], porPuntuacion[3]),
        new TramoDeReputacion(PUNTUACIONES[4], porPuntuacion[4]));
  }
}
