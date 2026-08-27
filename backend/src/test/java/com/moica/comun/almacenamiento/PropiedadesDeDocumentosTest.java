package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

/**
 * Los dos límites del expediente y sus topes.
 *
 * <p>El de tamaño solo puede bajarse: por encima de 5 MB la aplicación aceptaría archivos que la
 * restricción {@code ck_documento_verificacion_tamano} rechazaría después, y el fallo aparecería al
 * guardar en lugar de al validar.
 */
class PropiedadesDeDocumentosTest {

  @Test
  void aceptaLosValoresPorOmision() {
    PropiedadesDeDocumentos propiedades =
        new PropiedadesDeDocumentos(DataSize.ofMegabytes(5), Duration.ofMinutes(5));

    assertThat(propiedades.tamanoMaximo()).isEqualTo(DataSize.ofMegabytes(5));
    assertThat(propiedades.duracionUrlTemporal()).isEqualTo(Duration.ofMinutes(5));
  }

  @Test
  void admiteUnMaximoPorDebajoDelTope() {
    assertThatCode(
            () -> new PropiedadesDeDocumentos(DataSize.ofMegabytes(2), Duration.ofMinutes(5)))
        .doesNotThrowAnyException();
  }

  @Test
  void rechazaUnMaximoNuloCeroONegativo() {
    assertThatThrownBy(() -> new PropiedadesDeDocumentos(null, Duration.ofMinutes(5)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MOICA_DOCUMENTO_TAMANO_MAXIMO");
    assertThatThrownBy(
            () -> new PropiedadesDeDocumentos(DataSize.ofBytes(0), Duration.ofMinutes(5)))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> new PropiedadesDeDocumentos(DataSize.ofBytes(-1), Duration.ofMinutes(5)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rechazaUnMaximoPorEncimaDelTopeDelDiccionario() {
    assertThatThrownBy(
            () -> new PropiedadesDeDocumentos(DataSize.ofMegabytes(6), Duration.ofMinutes(5)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("5MB");
  }

  @Test
  void rechazaUnaDuracionNulaCeroONegativa() {
    assertThatThrownBy(() -> new PropiedadesDeDocumentos(DataSize.ofMegabytes(5), null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MOICA_DOCUMENTO_URL_TEMPORAL_DURACION");
    assertThatThrownBy(() -> new PropiedadesDeDocumentos(DataSize.ofMegabytes(5), Duration.ZERO))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> new PropiedadesDeDocumentos(DataSize.ofMegabytes(5), Duration.ofMinutes(-1)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rechazaUnAccesoTemporalQueDuraMasDeUnaHora() {
    assertThatThrownBy(
            () -> new PropiedadesDeDocumentos(DataSize.ofMegabytes(5), Duration.ofHours(2)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("PT5M");
  }
}
