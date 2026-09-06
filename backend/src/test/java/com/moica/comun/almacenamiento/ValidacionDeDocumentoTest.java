package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moica.comun.error.ErrorDeAplicacion;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;

/**
 * Las reglas de un documento del expediente, comprobadas sin red y sin base de datos.
 *
 * <p>La comprobación que importa es la última: un archivo cuyo interior no corresponde con su
 * cabecera se rechaza aunque la cabecera sea admisible.
 */
class ValidacionDeDocumentoTest {

  private final ValidacionDeDocumento validacion =
      new ValidacionDeDocumento(
          new PropiedadesDeDocumentos(DataSize.ofMegabytes(5), Duration.ofMinutes(5)));

  @Test
  void admiteLosTresFormatosCuandoLaFirmaCorresponde() {
    assertThat(validacion.validar(jpeg(), "image/jpeg")).isEqualTo(TipoDeDocumento.JPEG);
    assertThat(validacion.validar(png(), "image/png")).isEqualTo(TipoDeDocumento.PNG);
    assertThat(validacion.validar(pdf(), "application/pdf")).isEqualTo(TipoDeDocumento.PDF);
  }

  @Test
  void rechazaUnArchivoVacio() {
    assertThatThrownBy(() -> validacion.validar(new byte[0], "application/pdf"))
        .isInstanceOfSatisfying(ErrorDeAplicacion.class, ValidacionDeDocumentoTest::noAdmitido);
    assertThatThrownBy(() -> validacion.validar(null, "application/pdf"))
        .isInstanceOfSatisfying(ErrorDeAplicacion.class, ValidacionDeDocumentoTest::noAdmitido);
  }

  @Test
  void rechazaUnArchivoMayorQueElMaximoConfigurado() {
    byte[] enorme = Arrays.copyOf(pdf(), 5 * 1024 * 1024 + 1);

    assertThatThrownBy(() -> validacion.validar(enorme, "application/pdf"))
        .isInstanceOfSatisfying(
            ErrorDeAplicacion.class,
            error -> {
              assertThat(error.getEstado()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
              assertThat(error.getCodigo()).isEqualTo("DOCUMENTO_DEMASIADO_GRANDE");
            });
  }

  @Test
  void respetaUnMaximoConfiguradoPorDebajoDelTope() {
    ValidacionDeDocumento conUnMega =
        new ValidacionDeDocumento(
            new PropiedadesDeDocumentos(DataSize.ofMegabytes(1), Duration.ofMinutes(5)));
    byte[] deDosMegas = Arrays.copyOf(pdf(), 2 * 1024 * 1024);

    assertThatThrownBy(() -> conUnMega.validar(deDosMegas, "application/pdf"))
        .isInstanceOfSatisfying(
            ErrorDeAplicacion.class, error -> assertThat(error.getMessage()).contains("1 MB"));
  }

  @Test
  void rechazaUnFormatoQueNoEstaAdmitido() {
    byte[] webp = new byte[] {0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50};

    assertThatThrownBy(() -> validacion.validar(webp, "image/webp"))
        .isInstanceOfSatisfying(ErrorDeAplicacion.class, ValidacionDeDocumentoTest::noAdmitido);
  }

  @Test
  void rechazaUnaCabeceraQueNoCorrespondeConLaFirmaReal() {
    assertThatThrownBy(() -> validacion.validar(pdf(), "image/png"))
        .isInstanceOfSatisfying(ErrorDeAplicacion.class, ValidacionDeDocumentoTest::noAdmitido);
    assertThatThrownBy(() -> validacion.validar(png(), "application/pdf"))
        .isInstanceOfSatisfying(ErrorDeAplicacion.class, ValidacionDeDocumentoTest::noAdmitido);
  }

  @Test
  void elMensajeNoDistingueEntreFormatoDesconocidoYCabeceraQueMiente() {
    byte[] cualquiera = "no soy un documento".getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> validacion.validar(cualquiera, "application/pdf"))
        .isInstanceOfSatisfying(ErrorDeAplicacion.class, ValidacionDeDocumentoTest::noAdmitido);
  }

  private static void noAdmitido(ErrorDeAplicacion error) {
    assertThat(error.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(error.getCodigo()).isEqualTo("DOCUMENTO_NO_ADMITIDO");
  }

  private static byte[] jpeg() {
    return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x2A};
  }

  private static byte[] png() {
    return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x2A};
  }

  private static byte[] pdf() {
    return "%PDF-1.7 documento de prueba".getBytes(StandardCharsets.US_ASCII);
  }
}
