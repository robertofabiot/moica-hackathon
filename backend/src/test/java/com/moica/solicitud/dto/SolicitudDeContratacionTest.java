package com.moica.solicitud.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Validación del envío de una solicitud, comprobada sobre el DTO.
 *
 * <p>Que además se aplique al llegar por HTTP lo demuestra {@code SolicitudServicioIT}.
 */
class SolicitudDeContratacionTest {

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
  void aceptaUnaSolicitudCorrecta() {
    assertThat(errores(pedido(10L, "Se fugará el lavamanos.", 3, "Portón verde", LocalDate.now())))
        .isEmpty();
  }

  @Test
  void admiteUnaFechaPreferidaAusente() {
    assertThat(errores(pedido(10L, "Se fugará el lavamanos.", 3, "Portón verde", null))).isEmpty();
  }

  @Test
  void recortaLosTextosAlConstruir() {
    SolicitudDeContratacion pedido =
        pedido(10L, "  Se fugará el lavamanos.  ", 3, "  Portón verde  ", null);

    assertThat(pedido.descripcionNecesidad()).isEqualTo("Se fugará el lavamanos.");
    assertThat(pedido.indicacionUbicacion()).isEqualTo("Portón verde");
  }

  @Test
  void rechazaLosDatosVaciosOAusentes() {
    assertThat(camposConError(pedido(null, "  ", null, "  ", null)))
        .contains(
            "idServicioPublicado", "descripcionNecesidad", "idMunicipio", "indicacionUbicacion");
  }

  @Test
  void rechazaTextosQueSuperanElMaximoDeLaAplicacion() {
    assertThat(camposConError(pedido(10L, "N".repeat(3001), 3, "Portón verde", null)))
        .contains("descripcionNecesidad");
    assertThat(camposConError(pedido(10L, "Se fugará el lavamanos.", 3, "U".repeat(2001), null)))
        .contains("indicacionUbicacion");
  }

  private static SolicitudDeContratacion pedido(
      Long idServicio,
      String descripcion,
      Integer idMunicipio,
      String ubicacion,
      LocalDate fechaPreferida) {
    return new SolicitudDeContratacion(
        idServicio, descripcion, idMunicipio, ubicacion, fechaPreferida);
  }

  private static Set<ConstraintViolation<SolicitudDeContratacion>> errores(
      SolicitudDeContratacion pedido) {
    return validador.validate(pedido);
  }

  private static String camposConError(SolicitudDeContratacion pedido) {
    return errores(pedido).stream()
        .map(error -> error.getPropertyPath().toString())
        .collect(Collectors.joining(","));
  }
}
