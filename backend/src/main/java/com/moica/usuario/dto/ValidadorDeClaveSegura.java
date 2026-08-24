package com.moica.usuario.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

/**
 * Comprueba la parte de {@link ClaveSegura} que no se puede expresar con anotaciones estándar: que
 * la contraseña quepa en los 72 bytes que admite BCrypt.
 *
 * <p>El resto de la política —longitud en caracteres y composición— lo imponen las restricciones
 * con las que {@link ClaveSegura} está compuesta.
 */
public class ValidadorDeClaveSegura implements ConstraintValidator<ClaveSegura, String> {

  private static final int BYTES_MAXIMOS = 72;

  @Override
  public boolean isValid(String clave, ConstraintValidatorContext contexto) {
    // Una contraseña ausente o vacía es asunto de @NotBlank, no de esta regla.
    if (clave == null) {
      return true;
    }
    return clave.getBytes(StandardCharsets.UTF_8).length <= BYTES_MAXIMOS;
  }
}
