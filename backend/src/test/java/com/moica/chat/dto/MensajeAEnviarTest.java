package com.moica.chat.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Validación de un mensaje del chat, comprobada sobre el DTO.
 *
 * <p>Que además se aplique al llegar por HTTP lo demuestra {@code ChatDeSolicitudIT}.
 */
class MensajeAEnviarTest {

  private static ValidatorFactory fabrica;
  private static Validator validador;

  @BeforeAll
  static void prepararValidador() {
    fabrica = Validation.buildDefaultValidatorFactory();
    validador = fabrica.getValidator();
  }

  @AfterAll
  static void cerrarValidador() {
    fabrica.close();
  }

  @Test
  void aceptaUnMensajeConTexto() {
    assertThat(errores(new MensajeAEnviar("¿A qué hora llega?"))).isEmpty();
  }

  @Test
  void recortaElContenidoAlConstruir() {
    assertThat(new MensajeAEnviar("  ¿A qué hora llega?  ").contenido())
        .isEqualTo("¿A qué hora llega?");
  }

  @Test
  void rechazaUnMensajeVacioAusenteOSoloDeEspacios() {
    assertThat(errores(new MensajeAEnviar(""))).isNotEmpty();
    assertThat(errores(new MensajeAEnviar(null))).isNotEmpty();
    assertThat(errores(new MensajeAEnviar("     "))).isNotEmpty();
    assertThat(errores(new MensajeAEnviar("\n\t "))).isNotEmpty();
  }

  @Test
  void rechazaUnMensajeQueSuperaElTopeDeLaAplicacion() {
    assertThat(errores(new MensajeAEnviar("a".repeat(MensajeAEnviar.MAXIMO_CARACTERES)))).isEmpty();
    assertThat(errores(new MensajeAEnviar("a".repeat(MensajeAEnviar.MAXIMO_CARACTERES + 1))))
        .isNotEmpty();
  }

  @Test
  void losEspaciosExterioresNoCuentanParaElTope() {
    // El recorte ocurre antes de validar: un mensaje del máximo con espacios
    // alrededor sigue siendo válido.
    assertThat(
            errores(new MensajeAEnviar("  " + "a".repeat(MensajeAEnviar.MAXIMO_CARACTERES) + " ")))
        .isEmpty();
  }

  private static Set<ConstraintViolation<MensajeAEnviar>> errores(MensajeAEnviar mensaje) {
    return validador.validate(mensaje);
  }
}
