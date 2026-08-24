package com.moica.auth.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Locale;

/**
 * Credenciales con las que se inicia sesión.
 *
 * <p>Aquí no se valida el formato del correo ni la política de contraseña: iniciar sesión solo
 * comprueba si las credenciales son correctas, y describir lo que se espera ayudaría a adivinarlas.
 *
 * <p>El correo se normaliza igual que al registrarse, para que escribirlo con otras mayúsculas o
 * con espacios exteriores siga llevando a la misma cuenta.
 */
public record SolicitudDeInicioSesion(@NotBlank String correoElectronico, @NotBlank String clave) {

  public SolicitudDeInicioSesion {
    correoElectronico =
        (correoElectronico == null) ? null : correoElectronico.strip().toLowerCase(Locale.ROOT);
  }
}
