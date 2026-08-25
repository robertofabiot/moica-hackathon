package com.moica.auth.dto;

/**
 * Lo que hace falta para configurar la aplicación autenticadora.
 *
 * <p>Es la única respuesta de Moica que contiene el secreto TOTP, y solo se entrega a la propia
 * persona autenticada al iniciar la activación. En cuanto el segundo factor queda activo, el
 * secreto deja de poder recuperarse: consultarlo después devuelve el estado y nada más.
 *
 * @param claveManual secreto en Base32, para escribirlo a mano en la aplicación autenticadora
 * @param uriDeConfiguracion URI {@code otpauth://} equivalente, que las aplicaciones entienden
 *     directamente
 * @param digitos cuántos dígitos tendrá cada código
 * @param periodoEnSegundos cuánto vale cada código
 */
public record ActivacionDeSegundoFactor(
    String claveManual, String uriDeConfiguracion, int digitos, long periodoEnSegundos) {

  /**
   * Se redefine a propósito: la representación que genera el compilador incluiría el secreto en
   * Base32 y la URI {@code otpauth://}, que lo lleva dentro. Los parámetros sí se describen: no son
   * secretos y ayudan a diagnosticar.
   */
  @Override
  public String toString() {
    return "ActivacionDeSegundoFactor[claveManual=(oculta), uriDeConfiguracion=(oculta), digitos="
        + digitos
        + ", periodoEnSegundos="
        + periodoEnSegundos
        + "]";
  }
}
