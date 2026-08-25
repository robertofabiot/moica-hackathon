package com.moica.auth.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * El cifrado con el que se guarda el secreto TOTP.
 *
 * <p>Lo que se comprueba aquí es lo que hace que la columna no sirva por sí sola: que el valor
 * guardado no se parece al original, que dos cifrados del mismo secreto no coinciden y que
 * cualquier manipulación se detecta en lugar de producir otro secreto.
 */
class CifradoDeSecretosTest {

  private static final String SECRETO = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

  private final CifradoDeSecretos cifrado =
      new CifradoDeSecretos(propiedades("clave-uno-de-pruebas-de-32bytes!"));

  @Test
  void devuelveElSecretoOriginalAlDescifrar() {
    assertThat(cifrado.descifrar(cifrado.cifrar(SECRETO))).isEqualTo(SECRETO);
  }

  @Test
  void elValorGuardadoNoContieneElSecretoEnClaro() {
    String guardado = cifrado.cifrar(SECRETO);

    assertThat(guardado).doesNotContain(SECRETO);
    assertThat(new String(Base64.getDecoder().decode(guardado), StandardCharsets.ISO_8859_1))
        .doesNotContain(SECRETO);
  }

  @Test
  void cifrarDosVecesElMismoSecretoProduceValoresDistintos() {
    String primero = cifrado.cifrar(SECRETO);
    String segundo = cifrado.cifrar(SECRETO);

    // Es lo que garantiza el nonce aleatorio: sin él, dos cuentas con el mismo
    // secreto se reconocerían mirando la tabla.
    assertThat(primero).isNotEqualTo(segundo);
    assertThat(cifrado.descifrar(primero)).isEqualTo(cifrado.descifrar(segundo));
  }

  @Test
  void rechazaUnValorAlteradoEnLugarDeDevolverOtroSecreto() {
    char[] guardado = cifrado.cifrar(SECRETO).toCharArray();
    // Se cambia un carácter del criptograma, más allá del nonce.
    int posicion = guardado.length - 5;
    guardado[posicion] = (guardado[posicion] == 'A') ? 'B' : 'A';

    assertThatThrownBy(() -> cifrado.descifrar(new String(guardado)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rechazaUnValorCifradoConOtraClave() {
    CifradoDeSecretos otro = new CifradoDeSecretos(propiedades("clave-dos-de-pruebas-de-32bytes!"));

    assertThatThrownBy(() -> otro.descifrar(cifrado.cifrar(SECRETO)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rechazaUnValorQueNiSiquieraTieneLaFormaEsperada() {
    assertThatThrownBy(() -> cifrado.descifrar("esto no es base64"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> cifrado.descifrar(Base64.getEncoder().encodeToString(new byte[4])))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void noRevelaElSecretoEnElMensajeDeError() {
    assertThatThrownBy(() -> cifrado.descifrar(Base64.getEncoder().encodeToString(new byte[4])))
        .hasMessageNotContaining(SECRETO);
  }

  private static PropiedadesDeSegundoFactor propiedades(String claveDe32Bytes) {
    String base64 =
        Base64.getEncoder().encodeToString(claveDe32Bytes.getBytes(StandardCharsets.UTF_8));
    return new PropiedadesDeSegundoFactor(base64, 6, Duration.ofSeconds(30), 1);
  }
}
