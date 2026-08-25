package com.moica.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos con los que se desactiva el segundo factor.
 *
 * <p>Se exigen las dos cosas —contraseña y código— porque desactivarlo baja el nivel de protección
 * de la cuenta: quien lo haga debe demostrar que tiene ambos factores en ese momento.
 */
public record SolicitudDeDesactivacionDeSegundoFactor(
    @NotBlank String claveActual, @NotBlank @Size(max = 16) String codigo) {

  public SolicitudDeDesactivacionDeSegundoFactor {
    codigo = (codigo == null) ? null : codigo.replaceAll("\s", "");
  }
}
