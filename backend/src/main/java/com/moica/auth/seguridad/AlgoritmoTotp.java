package com.moica.auth.seguridad;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

/**
 * Genera y comprueba los códigos temporales del segundo factor.
 *
 * <p>El algoritmo es el de la RFC 6238 y lo implementa {@code java-otp}: Moica no lo reescribe. Lo
 * que aporta esta clase es lo que rodea al algoritmo y sí es decisión del proyecto: los parámetros
 * de {@link PropiedadesDeSegundoFactor}, la generación del secreto, su representación en Base32 —el
 * alfabeto que esperan las aplicaciones autenticadoras— y la URI {@code otpauth://}.
 *
 * <p>La hora llega siempre por el {@link Clock} inyectado, nunca de {@code Instant.now()}. Así una
 * prueba puede situarse en el instante que necesite en lugar de esperar a que pase un periodo real.
 */
@Component
public class AlgoritmoTotp {

  /**
   * 160 bits, que es el tamaño que la RFC 4226 recomienda para HMAC-SHA1 y el que producen las
   * aplicaciones autenticadoras habituales.
   */
  private static final int BYTES_DEL_SECRETO = 20;

  /** Nombre con el que Moica aparece en la aplicación autenticadora. */
  private static final String EMISOR = "Moica";

  private final SecureRandom aleatorio = new SecureRandom();
  private final Base32 base32 = new Base32();
  private final TimeBasedOneTimePasswordGenerator generador;
  private final PropiedadesDeSegundoFactor propiedades;
  private final Clock reloj;

  public AlgoritmoTotp(PropiedadesDeSegundoFactor propiedades, Clock reloj) {
    this.propiedades = propiedades;
    this.reloj = reloj;
    this.generador =
        new TimeBasedOneTimePasswordGenerator(
            propiedades.periodo(),
            propiedades.digitos(),
            TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
  }

  /** Secreto nuevo, en Base32 y sin relleno, tomado de una fuente criptográficamente segura. */
  public String generarSecreto() {
    byte[] bytes = new byte[BYTES_DEL_SECRETO];
    aleatorio.nextBytes(bytes);
    return base32.encodeAsString(bytes).replace("=", "");
  }

  /**
   * Código que corresponde a un secreto en un instante concreto.
   *
   * <p>Es el mismo cálculo que hace la aplicación autenticadora del teléfono.
   */
  public String codigoPara(String secretoBase32, Instant instante) {
    try {
      return generador.generateOneTimePasswordString(claveDe(secretoBase32), instante, Locale.ROOT);
    } catch (InvalidKeyException secretoInservible) {
      throw new IllegalStateException("El secreto del segundo factor no es utilizable.");
    }
  }

  /**
   * Comprueba un código contra el periodo actual y los de tolerancia.
   *
   * <p>La comparación recorre siempre todos los periodos admitidos, sin cortar en cuanto encuentra
   * uno que cuadra: así el tiempo de respuesta no revela cuál era el correcto.
   */
  public boolean esCodigoValido(String secretoBase32, String codigo) {
    if (codigo == null || codigo.length() != propiedades.digitos()) {
      return false;
    }

    Instant ahora = reloj.instant();
    int tolerancia = propiedades.pasosDeTolerancia();
    boolean valido = false;

    for (int paso = -tolerancia; paso <= tolerancia; paso++) {
      Instant instante = ahora.plus(propiedades.periodo().multipliedBy(paso));
      valido |= sonIguales(codigoPara(secretoBase32, instante), codigo);
    }
    return valido;
  }

  /**
   * URI {@code otpauth://} con la que se configura la aplicación autenticadora.
   *
   * <p>Lleva los mismos parámetros que usa {@link #codigoPara(String, Instant)}: si alguno cambiara
   * sin cambiar el otro, los códigos del teléfono dejarían de coincidir con los del servidor.
   */
  public String uriDeConfiguracion(String correoElectronico, String secretoBase32) {
    String etiqueta = codificar(EMISOR + ":" + correoElectronico);

    return "otpauth://totp/"
        + etiqueta
        + "?secret="
        + secretoBase32
        + "&issuer="
        + codificar(EMISOR)
        + "&algorithm=SHA1&digits="
        + propiedades.digitos()
        + "&period="
        + propiedades.periodo().toSeconds();
  }

  public int digitos() {
    return propiedades.digitos();
  }

  public long periodoEnSegundos() {
    return propiedades.periodo().toSeconds();
  }

  private SecretKeySpec claveDe(String secretoBase32) {
    return new SecretKeySpec(
        base32.decode(secretoBase32), TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
  }

  /** Comparación de tiempo constante: un código no debe poder adivinarse dígito a dígito. */
  private static boolean sonIguales(String esperado, String recibido) {
    return java.security.MessageDigest.isEqual(
        esperado.getBytes(StandardCharsets.UTF_8), recibido.getBytes(StandardCharsets.UTF_8));
  }

  private static String codificar(String valor) {
    // `URLEncoder` codifica el espacio como «+», que en la parte de ruta de la
    // URI significa un «+» literal; `otpauth://` espera «%20».
    return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
