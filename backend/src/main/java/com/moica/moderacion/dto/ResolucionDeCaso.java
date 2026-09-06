package com.moica.moderacion.dto;

import com.moica.moderacion.entity.ResultadoCasoModeracion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * La decisión con la que se cierra un caso.
 *
 * <p>Resultado y resolución llegan juntos porque el cierre es un bloque: {@code
 * ck_caso_moderacion_cierre} exige los dos a la vez, y una decisión sin explicación no sería
 * auditable meses después.
 *
 * <p>El texto se recorta antes de validarlo, igual que hace {@code ReporteAPresentar}: así una
 * resolución de solo espacios se rechaza como vacía y no como válida.
 *
 * @param resultado {@code PROCEDENTE} o {@code DESESTIMADO}; no hay un tercer valor
 * @param resolucion explicación de la decisión, que queda en el historial del caso
 */
public record ResolucionDeCaso(
    @NotNull(message = "Indica si el caso resultó procedente o desestimado.") ResultadoCasoModeracion resultado,
    @NotBlank(message = "Escribe la resolución del caso.") @Size(max = 3000, message = "La resolución no puede pasar de 3000 caracteres.") String resolucion) {

  public ResolucionDeCaso {
    resolucion = resolucion == null ? null : resolucion.strip();
  }
}
