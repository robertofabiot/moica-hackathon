package com.moica.calificacion.dto;

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
 * Validación y normalización de una calificación, comprobadas sobre el DTO.
 *
 * <p>Que además se apliquen al llegar por HTTP lo demuestra {@code CalificacionDeSolicitudIT}, y
 * que PostgreSQL sostenga el rango por su cuenta, {@code EsquemaDeCalificacionesIT}.
 */
class CalificacionAEmitirTest {

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
  void aceptaLasCincoPuntuacionesDelDominio() {
    for (short puntuacion = 1; puntuacion <= 5; puntuacion++) {
      assertThat(errores(new CalificacionAEmitir(puntuacion, null))).isEmpty();
    }
  }

  @Test
  void rechazaUnaPuntuacionFueraDelRangoOAusente() {
    assertThat(errores(new CalificacionAEmitir((short) 0, null))).isNotEmpty();
    assertThat(errores(new CalificacionAEmitir((short) 6, null))).isNotEmpty();
    assertThat(errores(new CalificacionAEmitir((short) -1, null))).isNotEmpty();
    assertThat(errores(new CalificacionAEmitir(null, "Con comentario pero sin estrellas")))
        .isNotEmpty();
  }

  @Test
  void elComentarioEsOpcional() {
    assertThat(new CalificacionAEmitir((short) 5, null).comentario()).isNull();
    assertThat(errores(new CalificacionAEmitir((short) 5, null))).isEmpty();
  }

  @Test
  void recortaElComentarioYDejaEnNuloElQueSoloTraeEspacios() {
    assertThat(new CalificacionAEmitir((short) 5, "  Muy puntual.  ").comentario())
        .isEqualTo("Muy puntual.");
    assertThat(new CalificacionAEmitir((short) 5, "").comentario()).isNull();
    assertThat(new CalificacionAEmitir((short) 5, "     ").comentario()).isNull();
    assertThat(new CalificacionAEmitir((short) 5, "\n\t ").comentario()).isNull();
  }

  @Test
  void rechazaUnComentarioQueSuperaElTopeDeLaAplicacion() {
    String enElTope = "a".repeat(CalificacionAEmitir.MAXIMO_CARACTERES);

    assertThat(errores(new CalificacionAEmitir((short) 5, enElTope))).isEmpty();
    assertThat(errores(new CalificacionAEmitir((short) 5, enElTope + "a"))).isNotEmpty();
  }

  @Test
  void losEspaciosExterioresNoCuentanParaElTope() {
    // El recorte ocurre antes de validar: un comentario del máximo con espacios
    // alrededor sigue siendo válido.
    String conEspacios = "  " + "a".repeat(CalificacionAEmitir.MAXIMO_CARACTERES) + " ";

    assertThat(errores(new CalificacionAEmitir((short) 5, conEspacios))).isEmpty();
  }

  private static Set<ConstraintViolation<CalificacionAEmitir>> errores(
      CalificacionAEmitir calificacion) {
    return validador.validate(calificacion);
  }
}
