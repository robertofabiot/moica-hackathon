package com.moica.auth.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * Los códigos del segundo factor.
 *
 * <p>Todo se comprueba con un reloj fijo. Una prueba que dependiera de la hora real tendría que
 * esperar treinta segundos para demostrar que un código caduca, y además fallaría de vez en cuando
 * si se ejecutara justo al cambiar de periodo.
 */
class AlgoritmoTotpTest {

  private static final Instant INSTANTE = Instant.parse("2026-08-24T15:00:00Z");
  private static final Duration PERIODO = Duration.ofSeconds(30);

  /**
   * Secretos fijos a propósito. Con un secreto aleatorio distinto en cada ejecución, dos códigos de
   * seis dígitos podrían coincidir por azar y la prueba fallaría una vez cada muchas miles: aquí lo
   * que se comprueba es el algoritmo, no la aleatoriedad, que tiene su propia prueba.
   */
  private static final String SECRETO = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

  private static final String OTRO_SECRETO = "MFRGGZDFMZTWQ2LKNNWG23TPOBYXE43U";

  private final AlgoritmoTotp totp = algoritmoCon(INSTANTE, 1);

  @Test
  void aceptaElCodigoDelPeriodoEnCurso() {
    assertThat(totp.esCodigoValido(SECRETO, totp.codigoPara(SECRETO, INSTANTE))).isTrue();
  }

  @Test
  void aceptaElCodigoDelPeriodoAnteriorYDelSiguienteDentroDeLaTolerancia() {
    assertThat(totp.esCodigoValido(SECRETO, totp.codigoPara(SECRETO, INSTANTE.minus(PERIODO))))
        .as("un teléfono con el reloj un poco atrasado debe poder entrar")
        .isTrue();
    assertThat(totp.esCodigoValido(SECRETO, totp.codigoPara(SECRETO, INSTANTE.plus(PERIODO))))
        .as("un teléfono con el reloj un poco adelantado también")
        .isTrue();
  }

  @Test
  void rechazaUnCodigoFueraDeLaTolerancia() {
    assertThat(
            totp.esCodigoValido(
                SECRETO, totp.codigoPara(SECRETO, INSTANTE.minus(PERIODO.multipliedBy(2)))))
        .isFalse();
    assertThat(
            totp.esCodigoValido(
                SECRETO, totp.codigoPara(SECRETO, INSTANTE.plus(PERIODO.multipliedBy(2)))))
        .isFalse();
    assertThat(
            totp.esCodigoValido(
                SECRETO, totp.codigoPara(SECRETO, INSTANTE.plus(Duration.ofHours(1)))))
        .isFalse();
  }

  @Test
  void sinToleranciaSoloValeElPeriodoEnCurso() {
    AlgoritmoTotp estricto = algoritmoCon(INSTANTE, 0);

    assertThat(estricto.esCodigoValido(SECRETO, estricto.codigoPara(SECRETO, INSTANTE))).isTrue();
    assertThat(
            estricto.esCodigoValido(SECRETO, estricto.codigoPara(SECRETO, INSTANTE.minus(PERIODO))))
        .isFalse();
  }

  @Test
  void rechazaElCodigoDeOtroSecreto() {
    assertThat(totp.esCodigoValido(SECRETO, totp.codigoPara(OTRO_SECRETO, INSTANTE))).isFalse();
  }

  @Test
  void rechazaUnCodigoVacioNuloOConOtraLongitud() {
    assertThat(totp.esCodigoValido(SECRETO, null)).isFalse();
    assertThat(totp.esCodigoValido(SECRETO, "")).isFalse();
    assertThat(totp.esCodigoValido(SECRETO, "12345")).isFalse();
    assertThat(totp.esCodigoValido(SECRETO, "1234567")).isFalse();
  }

  @Test
  void generaCodigosDeLosDigitosConfigurados() {
    assertThat(totp.codigoPara(SECRETO, INSTANTE)).hasSize(6).containsOnlyDigits();
    assertThat(totp.digitos()).isEqualTo(6);
    assertThat(totp.periodoEnSegundos()).isEqualTo(30);
  }

  @Test
  void generaSecretosDistintosYEnElAlfabetoQueEsperanLasAplicaciones() {
    String uno = totp.generarSecreto();
    String otro = totp.generarSecreto();

    assertThat(uno).isNotEqualTo(otro);
    // 160 bits en Base32 sin relleno son 32 caracteres de A-Z y 2-7.
    assertThat(uno).hasSize(32).matches("[A-Z2-7]+");
  }

  @Test
  void laUriDeConfiguracionAnunciaLosMismosParametrosQueUsaElServidor() {
    String uri = totp.uriDeConfiguracion("persona@moica.test", SECRETO);

    assertThat(uri)
        .startsWith("otpauth://totp/Moica%3Apersona%40moica.test?")
        .contains("secret=" + SECRETO)
        .contains("issuer=Moica")
        .contains("algorithm=SHA1")
        .contains("digits=6")
        .contains("period=30");
  }

  private static AlgoritmoTotp algoritmoCon(Instant instante, int pasosDeTolerancia) {
    String clave =
        Base64.getEncoder()
            .encodeToString("clave-de-pruebas-totp-de-moica!!".getBytes(StandardCharsets.UTF_8));

    return new AlgoritmoTotp(
        new PropiedadesDeSegundoFactor(clave, 6, PERIODO, pasosDeTolerancia),
        Clock.fixed(instante, ZoneOffset.UTC));
  }
}
