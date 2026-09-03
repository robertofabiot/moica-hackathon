package com.moica.moderacion.dto;

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
 * Validación y normalización de un reporte, comprobadas sobre el DTO.
 *
 * <p>Que además se apliquen al llegar por HTTP lo demuestra {@code ReporteDeParticipanteIT}, y que
 * PostgreSQL sostenga el ancho del motivo por su cuenta, {@code EsquemaDeCasosDeModeracionIT}.
 */
class ReporteAPresentarTest {

  private static final String MOTIVO = "Trato irrespetuoso";
  private static final String DESCRIPCION = "Usó insultos y no terminó el trabajo acordado.";

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
  void aceptaUnMotivoYUnaDescripcionCorrientes() {
    assertThat(errores(new ReporteAPresentar(MOTIVO, DESCRIPCION))).isEmpty();
  }

  @Test
  void exigeLosDosTextos() {
    assertThat(errores(new ReporteAPresentar(null, DESCRIPCION))).isNotEmpty();
    assertThat(errores(new ReporteAPresentar(MOTIVO, null))).isNotEmpty();
    assertThat(errores(new ReporteAPresentar("", DESCRIPCION))).isNotEmpty();
    assertThat(errores(new ReporteAPresentar(MOTIVO, ""))).isNotEmpty();
  }

  @Test
  void unTextoDeSoloEspaciosNoEsUnTexto() {
    assertThat(errores(new ReporteAPresentar("   ", DESCRIPCION))).isNotEmpty();
    assertThat(errores(new ReporteAPresentar(MOTIVO, "\n\t "))).isNotEmpty();
  }

  @Test
  void recortaLosEspaciosDeLosDosTextos() {
    ReporteAPresentar reporte = new ReporteAPresentar("  " + MOTIVO + " ", "\n" + DESCRIPCION);

    assertThat(reporte.motivo()).isEqualTo(MOTIVO);
    assertThat(reporte.descripcion()).isEqualTo(DESCRIPCION);
  }

  @Test
  void elMotivoRespetaElAnchoDeLaColumna() {
    String enElTope = "a".repeat(ReporteAPresentar.MAXIMO_MOTIVO);

    assertThat(ReporteAPresentar.MAXIMO_MOTIVO).isEqualTo(120);
    assertThat(errores(new ReporteAPresentar(enElTope, DESCRIPCION))).isEmpty();
    assertThat(errores(new ReporteAPresentar(enElTope + "a", DESCRIPCION))).isNotEmpty();
  }

  @Test
  void laDescripcionRespetaElTopeDeLaAplicacion() {
    String enElTope = "a".repeat(ReporteAPresentar.MAXIMO_DESCRIPCION);

    assertThat(ReporteAPresentar.MAXIMO_DESCRIPCION).isEqualTo(3000);
    assertThat(errores(new ReporteAPresentar(MOTIVO, enElTope))).isEmpty();
    assertThat(errores(new ReporteAPresentar(MOTIVO, enElTope + "a"))).isNotEmpty();
  }

  @Test
  void losEspaciosExterioresNoCuentanParaElTope() {
    // El recorte ocurre antes de validar: un texto del máximo con espacios
    // alrededor sigue siendo válido.
    String conEspacios = "  " + "a".repeat(ReporteAPresentar.MAXIMO_MOTIVO) + " ";

    assertThat(errores(new ReporteAPresentar(conEspacios, DESCRIPCION))).isEmpty();
  }

  private static Set<ConstraintViolation<ReporteAPresentar>> errores(ReporteAPresentar reporte) {
    return validador.validate(reporte);
  }
}
