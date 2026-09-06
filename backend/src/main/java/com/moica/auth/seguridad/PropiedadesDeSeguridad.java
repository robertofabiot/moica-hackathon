package com.moica.auth.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros de la autenticación, todos configurables por entorno.
 *
 * <p>Corresponde con la decisión D-SEC-01 del plan: la sesión dura siete días por omisión, no se
 * renueva sola y la cookie viaja segura en producción.
 *
 * @param secretoJwt clave con la que se firma el JWT de sesión. Nunca se versiona: llega por la
 *     variable de entorno {@code MOICA_JWT_SECRETO}
 * @param duracionDeSesion cuánto vale una sesión desde que se abre
 * @param cookieSegura si la cookie exige HTTPS. En producción siempre {@code true}; en desarrollo
 *     debe ser {@code false} porque el proxy de Vite trabaja sobre HTTP
 */
@ConfigurationProperties("moica.seguridad")
public record PropiedadesDeSeguridad(
    String secretoJwt, Duration duracionDeSesion, boolean cookieSegura) {

  /** HMAC-SHA256 exige una clave de al menos 256 bits. */
  private static final int BYTES_MINIMOS_DEL_SECRETO = 32;

  public PropiedadesDeSeguridad {
    if (secretoJwt == null
        || secretoJwt.getBytes(StandardCharsets.UTF_8).length < BYTES_MINIMOS_DEL_SECRETO) {
      // El valor no se incluye en el mensaje: es un secreto y acabaría en los
      // registros de arranque.
      throw new IllegalStateException(
          "moica.seguridad.secreto-jwt debe tener al menos "
              + BYTES_MINIMOS_DEL_SECRETO
              + " bytes. Defínelo en la variable de entorno MOICA_JWT_SECRETO.");
    }
    if (duracionDeSesion == null || duracionDeSesion.isZero() || duracionDeSesion.isNegative()) {
      throw new IllegalStateException("moica.seguridad.duracion-de-sesion debe ser positiva.");
    }
  }

  /**
   * Se redefine a propósito: la representación que genera el compilador para un record incluye
   * todos sus componentes, y uno de ellos es la clave con la que se firman los JWT de sesión.
   */
  @Override
  public String toString() {
    return "PropiedadesDeSeguridad[secretoJwt=(oculto), duracionDeSesion="
        + duracionDeSesion
        + ", cookieSegura="
        + cookieSegura
        + "]";
  }
}
