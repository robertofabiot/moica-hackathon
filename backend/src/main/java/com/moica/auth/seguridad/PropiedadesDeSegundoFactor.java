package com.moica.auth.seguridad;

import java.time.Duration;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros del segundo factor TOTP, en un solo lugar.
 *
 * <p>Los tres primeros describen los códigos que espera la aplicación autenticadora y deben
 * coincidir con lo que anuncia la URI {@code otpauth://}. Cambiar cualquiera de ellos invalida los
 * secretos ya registrados, así que se declaran una sola vez y se leen desde aquí.
 *
 * <p>La comprobación se hace al arrancar: una configuración ausente o inválida impide el arranque
 * en lugar de descubrirse el día que alguien active su segundo factor.
 *
 * @param claveDeCifrado clave con la que se cifra el secreto TOTP antes de guardarlo, codificada en
 *     Base64 y de 16, 24 o 32 bytes (AES-128, AES-192 o AES-256). Nunca se versiona: llega por la
 *     variable de entorno {@code MOICA_TOTP_CLAVE_CIFRADO}
 * @param digitos cuántos dígitos tiene cada código
 * @param periodo cuánto vale cada código antes de que se genere el siguiente
 * @param pasosDeTolerancia cuántos periodos hacia atrás y hacia delante se aceptan además del
 *     actual, para absorber el desfase de reloj entre el teléfono y el servidor
 */
@ConfigurationProperties("moica.segundo-factor")
public record PropiedadesDeSegundoFactor(
    String claveDeCifrado, int digitos, Duration periodo, int pasosDeTolerancia) {

  /** Longitudes de clave que admite AES. */
  private static final int[] BYTES_ADMITIDOS_DE_LA_CLAVE = {16, 24, 32};

  private static final int DIGITOS_MINIMOS = 6;
  private static final int DIGITOS_MAXIMOS = 8;

  /** Un desfase mayor deja de ser tolerancia de reloj y alarga la vida útil del código. */
  private static final int PASOS_MAXIMOS_DE_TOLERANCIA = 2;

  public PropiedadesDeSegundoFactor {
    // Los mensajes describen qué falta, nunca el valor: acabarían en los
    // registros de arranque.
    if (!esClaveDeCifradoValida(claveDeCifrado)) {
      throw new IllegalStateException(
          "moica.segundo-factor.clave-de-cifrado debe ser Base64 de 16, 24 o 32 bytes."
              + " Defínela en la variable de entorno MOICA_TOTP_CLAVE_CIFRADO.");
    }
    if (digitos < DIGITOS_MINIMOS || digitos > DIGITOS_MAXIMOS) {
      throw new IllegalStateException(
          "moica.segundo-factor.digitos debe estar entre "
              + DIGITOS_MINIMOS
              + " y "
              + DIGITOS_MAXIMOS
              + ".");
    }
    if (periodo == null || periodo.isZero() || periodo.isNegative()) {
      throw new IllegalStateException("moica.segundo-factor.periodo debe ser positivo.");
    }
    if (pasosDeTolerancia < 0 || pasosDeTolerancia > PASOS_MAXIMOS_DE_TOLERANCIA) {
      throw new IllegalStateException(
          "moica.segundo-factor.pasos-de-tolerancia debe estar entre 0 y "
              + PASOS_MAXIMOS_DE_TOLERANCIA
              + ".");
    }
  }

  /** Bytes de la clave de cifrado, ya decodificados. */
  public byte[] bytesDeLaClaveDeCifrado() {
    return Base64.getDecoder().decode(claveDeCifrado);
  }

  /**
   * Se redefine a propósito: la implementación que genera el compilador para un record incluye
   * todos sus componentes, y uno de ellos es una clave criptográfica.
   */
  @Override
  public String toString() {
    return "PropiedadesDeSegundoFactor[claveDeCifrado=(oculta), digitos="
        + digitos
        + ", periodo="
        + periodo
        + ", pasosDeTolerancia="
        + pasosDeTolerancia
        + "]";
  }

  private static boolean esClaveDeCifradoValida(String clave) {
    if (clave == null || clave.isBlank()) {
      return false;
    }
    byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(clave);
    } catch (IllegalArgumentException noEsBase64) {
      return false;
    }
    for (int admitidos : BYTES_ADMITIDOS_DE_LA_CLAVE) {
      if (bytes.length == admitidos) {
        return true;
      }
    }
    return false;
  }
}
