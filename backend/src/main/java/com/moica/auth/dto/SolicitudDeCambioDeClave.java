package com.moica.auth.dto;

import com.moica.usuario.dto.ClaveSegura;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos con los que una persona cambia su contraseña.
 *
 * <p>La contraseña actual no se valida contra la política: describir lo que se espera de ella
 * ayudaría a adivinarla, y además una cuenta antigua podría tenerla de otra forma. La nueva sí
 * sigue {@link ClaveSegura}, que es la misma política del registro.
 */
public record SolicitudDeCambioDeClave(
    @NotBlank String claveActual, @NotBlank @ClaveSegura String claveNueva) {}
