package com.moica.usuario.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * La política de contraseña D-SEC-02 y el resto de reglas del registro, comprobadas sobre el DTO.
 *
 * <p>Aquí se demuestra la regla en sí; que además se aplique al llegar por HTTP lo demuestra {@code
 * RegistroDeUsuarioIT}.
 */
class SolicitudDeRegistroTest {

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
  void aceptaUnRegistroCorrecto() {
    assertThat(errores(solicitud("Erving Miranda", "erving@moica.test", "Moica2026$segura")))
        .isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "MOICA2026$", // sin minúscula
        "moica2026$", // sin mayúscula
        "MoicaClave$", // sin número
        "Moica2026clave", // sin símbolo
        "Mo1$abc", // siete caracteres
      })
  void rechazaUnaContrasenaQueNoCumpleLaPolitica(String clave) {
    assertThat(camposConError(solicitud("Persona Válida", "persona@moica.test", clave)))
        .contains("clave");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Moica2026$segura", // símbolo y dígitos
        "aB1 espacio", // el espacio también es un símbolo
        "Ñoño2026$moica", // eñes y acentos cuentan como letras
        "8caracT$", // el mínimo exacto
      })
  void aceptaUnaContrasenaQueSiCumpleLaPolitica(String clave) {
    assertThat(camposConError(solicitud("Persona Válida", "persona@moica.test", clave)))
        .doesNotContain("clave");
  }

  @Test
  void aceptaElMaximoDeSetentaYDosCaracteres() {
    String clave = "Moica2026$" + "a".repeat(62);

    assertThat(clave).hasSize(72);
    assertThat(camposConError(solicitud("Persona Válida", "persona@moica.test", clave)))
        .doesNotContain("clave");
  }

  @Test
  void rechazaUnaContrasenaQueSuperaElMaximoDeCaracteres() {
    String clave = "Moica2026$" + "a".repeat(63);

    assertThat(clave).hasSize(73);
    assertThat(camposConError(solicitud("Persona Válida", "persona@moica.test", clave)))
        .contains("clave");
  }

  @Test
  void rechazaUnaContrasenaQueCabeEnCaracteresPeroNoEnLosBytesDeBcrypt() {
    String clave = "Añ1$".repeat(18);

    assertThat(clave).hasSize(72);
    assertThat(clave.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isGreaterThan(72);
    assertThat(camposConError(solicitud("Persona Válida", "persona@moica.test", clave)))
        .contains("clave");
  }

  @Test
  void normalizaElCorreoYElNombreAlConstruirLaSolicitud() {
    SolicitudDeRegistro solicitud =
        solicitud("  Erving Miranda  ", "  Erving@Moica.TEST  ", "Moica2026$segura");

    assertThat(solicitud.nombreCompleto()).isEqualTo("Erving Miranda");
    assertThat(solicitud.correoElectronico()).isEqualTo("erving@moica.test");
    assertThat(errores(solicitud)).isEmpty();
  }

  @Test
  void rechazaUnCorreoConFormatoInvalido() {
    assertThat(camposConError(solicitud("Persona Válida", "arroba-perdida", "Moica2026$segura")))
        .contains("correoElectronico");
  }

  @Test
  void rechazaLosDatosVaciosOAusentes() {
    assertThat(camposConError(solicitud("  ", "  ", "  ")))
        .contains("nombreCompleto", "correoElectronico", "clave");
    assertThat(camposConError(solicitud(null, null, null)))
        .contains("nombreCompleto", "correoElectronico", "clave");
  }

  @Test
  void rechazaUnNombreMasLargoQueLaColumna() {
    assertThat(camposConError(solicitud("N".repeat(121), "persona@moica.test", "Moica2026$segura")))
        .contains("nombreCompleto");
  }

  private static SolicitudDeRegistro solicitud(String nombre, String correo, String clave) {
    return new SolicitudDeRegistro(nombre, correo, clave);
  }

  private static Set<ConstraintViolation<SolicitudDeRegistro>> errores(
      SolicitudDeRegistro solicitud) {
    return validador.validate(solicitud);
  }

  private static String camposConError(SolicitudDeRegistro solicitud) {
    return errores(solicitud).stream()
        .map(error -> error.getPropertyPath().toString())
        .collect(Collectors.joining(","));
  }
}
