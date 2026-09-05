package com.moica.moderacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El motivo por el que un caso cerrado vuelve a estar sin decisión definitiva.
 *
 * <p>Reabrir exige que la apelación registrada haya sido aceptada: es lo único que, según la
 * definición 11.1, mueve un caso de {@code CERRADO} a {@code REABIERTO}. El motivo queda en el
 * historial junto a la resolución anterior, que la reapertura conserva íntegra en su versión.
 */
public record ReaperturaDeCaso(
    @NotBlank(message = "Escribe por qué reabres el caso.") @Size(max = 2000, message = "El motivo no puede pasar de 2000 caracteres.") String motivo) {

  public ReaperturaDeCaso {
    motivo = motivo == null ? null : motivo.strip();
  }
}
