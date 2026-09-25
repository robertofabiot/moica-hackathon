package com.moica.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
 * <p>Un correo del dominio de nivel superior {@code .invalid} se rechaza: la RFC 2606 lo reserva
 * para direcciones que nunca reciben correo, así que ninguna persona lo usa. Los datos de
 * demostración viven ahí ({@code demo.moica.invalid}) precisamente por eso, y el cargador
 * identifica sus cuentas por ese correo: si el registro público lo aceptara, cualquiera podría
 * crear antes una cuenta con ese correo y el cargador la adoptaría como propia, verificada.
 *
 * <p>Los mensajes genéricos salen de {@code ValidationMessages.properties}.
 */
public record SolicitudDeRegistro(
    @NotBlank @Size(max = 120) String nombreCompleto,
    @NotBlank @Email @Size(max = 254) @Pattern(
            regexp = "^(?!.*\\.invalid$).*$",
            message = "Escribe un correo electrónico válido.")
        String correoElectronico,
    @NotBlank @ClaveSegura String clave) {

  public SolicitudDeRegistro {
    nombreCompleto = (nombreCompleto == null) ? null : nombreCompleto.strip();
    correoElectronico =
        (correoElectronico == null) ? null : correoElectronico.strip().toLowerCase(Locale.ROOT);
    // La contraseña no se toca: sus espacios forman parte de ella.
  }

  /**
   * Se redefine a propósito: la representación que genera el compilador incluiría la contraseña en
   * claro. El nombre y el correo sí se describen: son los datos que se estaba registrando.
   */
  @Override
  public String toString() {
    return "SolicitudDeRegistro[nombreCompleto="
        + nombreCompleto
        + ", correoElectronico="
        + correoElectronico
        + ", clave=(oculta)]";
  }
}
