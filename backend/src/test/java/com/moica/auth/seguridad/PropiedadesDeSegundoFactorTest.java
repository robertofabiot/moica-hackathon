package com.moica.auth.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * La configuración del segundo factor se comprueba al arrancar.
 *
 * <p>Una clave ausente, mal codificada o de una longitud que AES no admite debe impedir el
 * arranque. Descubrirlo el día que alguien active su segundo factor significaría haber guardado un
 * secreto sin cifrar o no poder recuperarlo.
 */
class PropiedadesDeSegundoFactorTest {

  private static final String CLAVE_DE_32_BYTES = base64De("clave-de-pruebas-totp-de-moica!!");
  private static final String CLAVE_DE_16_BYTES = base64De("clave-de-16bytes");
  private static final String CLAVE_DE_24_BYTES = base64De("clave-de-veinticuatro-24");

  @Test
  void aceptaLasTresLongitudesDeClaveQueAdmiteAes() {
    assertThatCode(() -> propiedadesCon(CLAVE_DE_16_BYTES)).doesNotThrowAnyException();
    assertThatCode(() -> propiedadesCon(CLAVE_DE_24_BYTES)).doesNotThrowAnyException();
    assertThatCode(() -> propiedadesCon(CLAVE_DE_32_BYTES)).doesNotThrowAnyException();
  }

  @Test
  void devuelveLosBytesDeLaClaveYaDecodificados() {
    assertThat(propiedadesCon(CLAVE_DE_32_BYTES).bytesDeLaClaveDeCifrado()).hasSize(32);
  }

  @Test
  void rechazaLaAusenciaDeClave() {
    assertThatThrownBy(() -> propiedadesCon(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MOICA_TOTP_CLAVE_CIFRADO");
    assertThatThrownBy(() -> propiedadesCon("   ")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rechazaUnaClaveQueNoEsBase64() {
    assertThatThrownBy(() -> propiedadesCon("esto no es base64 ni de lejos"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rechazaUnaClaveDeLongitudQueAesNoAdmite() {
    assertThatThrownBy(() -> propiedadesCon(base64De("demasiado-corta")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> propiedadesCon(base64De("clave-de-treinta-y-cuatro-b-1234ab")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void noRevelaLaClaveEnElMensajeDeError() {
    assertThatThrownBy(() -> propiedadesCon(base64De("secreto-que-no-debe-verse")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageNotContaining("secreto-que-no-debe-verse")
        .hasMessageNotContaining(base64De("secreto-que-no-debe-verse"));
  }

  @Test
  void noRevelaLaClaveAlConvertirseEnTexto() {
    String descripcion = propiedadesCon(CLAVE_DE_32_BYTES).toString();

    assertThat(descripcion).doesNotContain(CLAVE_DE_32_BYTES).contains("(oculta)");
    // Los parámetros no son secretos: describirlos ayuda a diagnosticar.
    assertThat(descripcion).contains("digitos=6").contains("pasosDeTolerancia=1");
  }

  @Test
  void rechazaUnNumeroDeDigitosFueraDeLoRazonable() {
    assertThatThrownBy(
            () -> new PropiedadesDeSegundoFactor(CLAVE_DE_32_BYTES, 4, Duration.ofSeconds(30), 1))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> new PropiedadesDeSegundoFactor(CLAVE_DE_32_BYTES, 9, Duration.ofSeconds(30), 1))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rechazaUnPeriodoQueNoAvanza() {
    assertThatThrownBy(() -> new PropiedadesDeSegundoFactor(CLAVE_DE_32_BYTES, 6, Duration.ZERO, 1))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> new PropiedadesDeSegundoFactor(CLAVE_DE_32_BYTES, 6, Duration.ofSeconds(-30), 1))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new PropiedadesDeSegundoFactor(CLAVE_DE_32_BYTES, 6, null, 1))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rechazaUnaToleranciaQueAlargariaLaVidaDelCodigo() {
    assertThatThrownBy(
            () -> new PropiedadesDeSegundoFactor(CLAVE_DE_32_BYTES, 6, Duration.ofSeconds(30), -1))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> new PropiedadesDeSegundoFactor(CLAVE_DE_32_BYTES, 6, Duration.ofSeconds(30), 3))
        .isInstanceOf(IllegalStateException.class);
  }

  private static PropiedadesDeSegundoFactor propiedadesCon(String clave) {
    return new PropiedadesDeSegundoFactor(clave, 6, Duration.ofSeconds(30), 1);
  }

  private static String base64De(String valor) {
    return Base64.getEncoder()
        .encodeToString(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }
}
