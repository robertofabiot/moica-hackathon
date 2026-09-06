package com.moica.calificacion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lo que un participante envía para calificar a la contraparte.
 *
 * <p>Solo lleva la puntuación y el comentario. Ni el calificado ni el rol viajan en el cuerpo: el
 * servicio los deriva de la solicitud y de la sesión, de modo que calificar a otra persona, o
 * calificarla en un rol que no desempeñó, no es una petición que se pueda formular.
 *
 * <p>El comentario es opcional. Se recorta y, si queda vacío, se guarda como {@code null}: un
 * comentario de espacios no es un comentario. El diccionario modela {@code comentario} como {@code
 * TEXT} sin máximo; el tope de {@value #MAXIMO_CARACTERES} caracteres es un límite de la
 * aplicación, documentado en el contrato, igual que en los demás textos libres de Moica.
 *
 * @param puntuacion estrellas otorgadas, de 1 a 5; la restricción {@code
 *     ck_calificacion_usuario_puntuacion} repite el rango en PostgreSQL
 */
public record CalificacionAEmitir(
    @NotNull @Min(1) @Max(5) Short puntuacion,
    @Size(max = CalificacionAEmitir.MAXIMO_CARACTERES) String comentario) {

  public static final int MAXIMO_CARACTERES = 2000;

  public CalificacionAEmitir {
    comentario = normalizar(comentario);
  }

  private static String normalizar(String comentario) {
    if (comentario == null) {
      return null;
    }
    String recortado = comentario.strip();
    return recortado.isEmpty() ? null : recortado;
  }
}
