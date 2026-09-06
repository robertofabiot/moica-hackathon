package com.moica.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Código temporal que presenta la persona desde su aplicación autenticadora.
 *
 * <p>Aquí no se comprueba cuántos dígitos tiene: ese parámetro es configurable y decirlo en el
 * error revelaría la forma del código. Lo único que se acota es un tamaño máximo razonable, para no
 * llevar hasta el verificador una cadena arbitrariamente larga.
 *
 * <p>Los espacios se retiran porque las aplicaciones autenticadoras muestran el código partido en
 * dos grupos y es habitual copiarlo tal cual.
 */
public record SolicitudDeCodigoTotp(@NotBlank @Size(max = 16) String codigo) {

  public SolicitudDeCodigoTotp {
    codigo = (codigo == null) ? null : codigo.replaceAll("\s", "");
  }

  /** Se redefine a propósito: el único componente es un código válido durante su ventana. */
  @Override
  public String toString() {
    return "SolicitudDeCodigoTotp[codigo=(oculto)]";
  }
}
