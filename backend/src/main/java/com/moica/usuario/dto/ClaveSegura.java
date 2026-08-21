package com.moica.usuario.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Política de contraseña de Moica, en un solo lugar.
 *
 * <p>Es la decisión D-SEC-02 del plan: de 8 a 72 caracteres, con al menos una mayúscula, una
 * minúscula, un número y un símbolo. «Alternar tipos» no obliga a que cada carácter cambie de
 * clase.
 *
 * <p>Las clases del patrón son Unicode a propósito: «Ñ» cuenta como mayúscula y «á» como minúscula,
 * no como símbolo. Símbolo es cualquier carácter que no sea letra ni número.
 *
 * <p>El máximo de 72 lo impone BCrypt, que solo tiene en cuenta los primeros 72 <em>bytes</em> de
 * la contraseña y rechaza lo que los supere. Como en UTF-8 una eñe o un emoji ocupan más de un
 * byte, el límite se comprueba dos veces: en caracteres, con {@link Size}, y en bytes, con {@link
 * ValidadorDeClaveSegura}. Sin la segunda comprobación una contraseña de 72 caracteres acentuados
 * llegaría hasta BCrypt y fallaría allí.
 */
@Documented
@Size(min = 8, max = 72, message = ClaveSegura.LONGITUD) @Pattern(regexp = ClaveSegura.PATRON, message = ClaveSegura.COMPOSICION) @Constraint(validatedBy = ValidadorDeClaveSegura.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ClaveSegura {

  /** Al menos una minúscula, una mayúscula, un número y un carácter que no sea letra ni número. */
  String PATRON = "^(?=.*\\p{Ll})(?=.*\\p{Lu})(?=.*\\p{N})(?=.*[^\\p{L}\\p{N}])[\\s\\S]+$";

  String LONGITUD = "La contraseña debe tener entre 8 y 72 caracteres.";

  String COMPOSICION =
      "La contraseña debe incluir al menos una mayúscula, una minúscula, un número y un símbolo.";

  String DEMASIADO_LARGA =
      "La contraseña es demasiado larga. Los acentos, las eñes y los emojis ocupan más de un"
          + " espacio, así que prueba con una más corta.";

  String message() default DEMASIADO_LARGA;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
