package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moica.comun.error.ErrorDeAplicacion;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;

/**
 * Las reglas de admisión de una imagen: tamaño, formato declarado y firma real.
 *
 * <p>El caso central es la cabecera que miente: un {@code Content-Type} admisible con un contenido
 * que no corresponde se rechaza igual que un formato desconocido.
 */
class ValidacionDeImagenTest {

  private final ValidacionDeImagen validacion =
      new ValidacionDeImagen(new PropiedadesDeImagenes(DataSize.ofKilobytes(64)));

  private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x2A};
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x2A};

  @Test
  void aceptaUnaImagenCuyaFirmaCorrespondeConLoDeclarado() {
    assertThat(validacion.validar(PNG, "image/png")).isEqualTo(TipoDeImagen.PNG);
    assertThat(validacion.validar(JPEG, "image/jpeg")).isEqualTo(TipoDeImagen.JPEG);
  }

  @Test
  void rechazaUnArchivoVacio() {
    assertThatThrownBy(() -> validacion.validar(new byte[0], "image/png"))
        .isInstanceOfSatisfying(
            ErrorDeAplicacion.class,
            error -> assertThat(error.getCodigo()).isEqualTo("IMAGEN_NO_ADMITIDA"));
  }

  @Test
  void rechazaUnArchivoMayorQueElMaximoConSuPropioCodigo() {
    byte[] grande = Arrays.copyOf(PNG, (int) DataSize.ofKilobytes(64).toBytes() + 1);

    assertThatThrownBy(() -> validacion.validar(grande, "image/png"))
        .isInstanceOfSatisfying(
            ErrorDeAplicacion.class,
            error -> {
              assertThat(error.getCodigo()).isEqualTo("IMAGEN_DEMASIADO_GRANDE");
              assertThat(error.getEstado()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
            });
  }

  @Test
  void rechazaUnTipoDeclaradoQueNoEstaAdmitido() {
    assertThatThrownBy(() -> validacion.validar(PNG, "image/svg+xml"))
        .isInstanceOfSatisfying(
            ErrorDeAplicacion.class,
            error -> assertThat(error.getCodigo()).isEqualTo("IMAGEN_NO_ADMITIDA"));
    assertThatThrownBy(() -> validacion.validar(PNG, null)).isInstanceOf(ErrorDeAplicacion.class);
  }

  @Test
  void rechazaUnaCabeceraAdmisibleCuyoContenidoNoCorresponde() {
    assertThatThrownBy(() -> validacion.validar(JPEG, "image/png"))
        .as("la cabecera dice PNG pero la firma es JPEG")
        .isInstanceOfSatisfying(
            ErrorDeAplicacion.class,
            error -> assertThat(error.getCodigo()).isEqualTo("IMAGEN_NO_ADMITIDA"));

    assertThatThrownBy(() -> validacion.validar("%PDF-1.7".getBytes(), "image/png"))
        .as("un PDF disfrazado de PNG tampoco entra")
        .isInstanceOf(ErrorDeAplicacion.class);
  }
}
