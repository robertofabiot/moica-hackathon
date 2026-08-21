package com.moica.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * Datos con los que una persona crea su cuenta.
 *
 * <p>Los máximos coinciden con los del diccionario de datos: 120 caracteres el nombre y 254 el
 * correo, que es el máximo de una dirección según la RFC 5321. La contraseña sigue la política
 * {@link ClaveSegura}.
 *
 * <p>El correo y el nombre se normalizan al construir la solicitud, antes de validarla: quien
 * escriba « Persona@Moica.NI » debe quedar registrado igual que quien escriba «persona@moica.ni», y
 * un correo con espacios exteriores no debe rechazarse por «formato inválido».
 *
 * <p>Los mensajes genéricos salen de {@code ValidationMessages.properties}.
 */
public record SolicitudDeRegistro(
    @NotBlank @Size(max = 120) String nombreCompleto,
    @NotBlank @Email @Size(max = 254) String correoElectronico,
    @NotBlank @ClaveSegura String clave) {

  public SolicitudDeRegistro {
    nombreCompleto = (nombreCompleto == null) ? null : nombreCompleto.strip();
    correoElectronico =
        (correoElectronico == null) ? null : correoElectronico.strip().toLowerCase(Locale.ROOT);
    // La contraseña no se toca: sus espacios forman parte de ella.
  }
}
