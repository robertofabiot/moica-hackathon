package com.moica.auth.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Cifra y descifra los secretos que Moica debe conservar en claro para poder usarlos.
 *
 * <p>Hoy solo hay uno: el secreto TOTP. No sirve un hash, porque el servidor necesita regenerar los
 * códigos, así que la protección es cifrado autenticado AES-GCM con una clave que llega por
 * variable de entorno y nunca se versiona.
 *
 * <p>Cada cifrado usa un nonce aleatorio distinto —GCM se rompe si un nonce se repite con la misma
 * clave— y el resultado viaja como {@code Base64(nonce || criptograma+etiqueta)}. La etiqueta de
 * autenticación es lo que hace que un valor manipulado en la base de datos falle al descifrarse en
 * lugar de producir un secreto distinto.
 */
@Component
public class CifradoDeSecretos {

  private static final String ALGORITMO = "AES";
  private static final String TRANSFORMACION = "AES/GCM/NoPadding";

  /** 96 bits es el tamaño de nonce recomendado para GCM. */
  private static final int BYTES_DEL_NONCE = 12;

  private static final int BITS_DE_LA_ETIQUETA = 128;

  private final SecureRandom aleatorio = new SecureRandom();
  private final SecretKeySpec clave;

  public CifradoDeSecretos(PropiedadesDeSegundoFactor propiedades) {
    // Las propiedades ya rechazaron al arrancar una clave ausente, mal
    // codificada o de una longitud que AES no admite. Se guarda ya convertida
    // en clave para que los bytes en bruto no queden en ningún campo.
    this.clave = new SecretKeySpec(propiedades.bytesDeLaClaveDeCifrado(), ALGORITMO);
  }

  /** Devuelve el valor cifrado, listo para guardarse en una columna de texto. */
  public String cifrar(String claro) {
    byte[] nonce = new byte[BYTES_DEL_NONCE];
    aleatorio.nextBytes(nonce);

    byte[] criptograma =
        aplicar(Cipher.ENCRYPT_MODE, nonce, claro.getBytes(StandardCharsets.UTF_8));

    byte[] resultado = new byte[nonce.length + criptograma.length];
    System.arraycopy(nonce, 0, resultado, 0, nonce.length);
    System.arraycopy(criptograma, 0, resultado, nonce.length, criptograma.length);

    return Base64.getEncoder().encodeToString(resultado);
  }

  /**
   * Recupera el valor original.
   *
   * @throws IllegalStateException si el valor no se cifró con esta clave o fue alterado. El mensaje
   *     no incluye el valor: describir qué llegó sería describir el secreto
   */
  public String descifrar(String cifrado) {
    byte[] completo;
    try {
      completo = Base64.getDecoder().decode(cifrado);
    } catch (IllegalArgumentException noEsBase64) {
      throw new IllegalStateException("El secreto almacenado no tiene el formato esperado.");
    }
    if (completo.length <= BYTES_DEL_NONCE) {
      throw new IllegalStateException("El secreto almacenado no tiene el formato esperado.");
    }

    byte[] nonce = new byte[BYTES_DEL_NONCE];
    System.arraycopy(completo, 0, nonce, 0, BYTES_DEL_NONCE);

    byte[] criptograma = new byte[completo.length - BYTES_DEL_NONCE];
    System.arraycopy(completo, BYTES_DEL_NONCE, criptograma, 0, criptograma.length);

    return new String(aplicar(Cipher.DECRYPT_MODE, nonce, criptograma), StandardCharsets.UTF_8);
  }

  private byte[] aplicar(int modo, byte[] nonce, byte[] datos) {
    try {
      Cipher cifrador = Cipher.getInstance(TRANSFORMACION);
      cifrador.init(modo, clave, new GCMParameterSpec(BITS_DE_LA_ETIQUETA, nonce));
      return cifrador.doFinal(datos);
    } catch (GeneralSecurityException fallo) {
      // Ni el mensaje ni la causa original salen de aquí: la excepción de JCE
      // puede describir el material con el que se trabajaba.
      throw new IllegalStateException("No se pudo procesar el secreto almacenado.");
    }
  }
}
