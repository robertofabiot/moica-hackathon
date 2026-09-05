package com.moica.moderacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El motivo con el que se levanta la medida vigente de un caso.
 *
 * <p>Se exige explicación porque revocar es una decisión tan auditable como aplicar: quien revise
 * el expediente meses después debe poder saber por qué la sanción dejó de estar vigente y no solo
 * que dejó de estarlo.
 */
public record RevocacionDeMedida(
    @NotBlank(message = "Escribe por qué revocas la medida.") @Size(max = 2000, message = "El motivo no puede pasar de 2000 caracteres.") String motivo) {

  public RevocacionDeMedida {
    motivo = motivo == null ? null : motivo.strip();
  }
}
