package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * La detección por firma binaria, que es la que decide si un archivo entra.
 *
 * <p>Las firmas de prueba son las reales de cada formato; los casos negativos cubren archivos
 * vacíos, truncados y contenedores RIFF que no son WebP.
 */
class TipoDeImagenTest {

  @Test
  void reconoceLasFirmasRealesDeLosTresFormatos() {
    assertThat(TipoDeImagen.porFirma(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}))
        .contains(TipoDeImagen.JPEG);
    assertThat(
            TipoDeImagen.porFirma(
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00}))
        .contains(TipoDeImagen.PNG);
    assertThat(TipoDeImagen.porFirma(contenidoWebp())).contains(TipoDeImagen.WEBP);
  }

  @Test
  void unRiffQueNoEsWebpNoSeAdmite() {
    byte[] wav = contenidoWebp();
    wav[8] = 'W';
    wav[9] = 'A';
    wav[10] = 'V';
    wav[11] = 'E';

    assertThat(TipoDeImagen.porFirma(wav)).isEmpty();
  }

  @Test
  void unContenidoVacioTruncadoODesconocidoNoSeAdmite() {
    assertThat(TipoDeImagen.porFirma(null)).isEmpty();
    assertThat(TipoDeImagen.porFirma(new byte[0])).isEmpty();
    assertThat(TipoDeImagen.porFirma(new byte[] {(byte) 0xFF, (byte) 0xD8})).isEmpty();
    assertThat(TipoDeImagen.porFirma("<svg xmlns='...'/>".getBytes())).isEmpty();
    assertThat(TipoDeImagen.porFirma("%PDF-1.7".getBytes())).isEmpty();
  }

  @Test
  void resuelveElTipoMimeDeclaradoSoloSiEstaAdmitido() {
    assertThat(TipoDeImagen.porTipoMime("image/jpeg")).contains(TipoDeImagen.JPEG);
    assertThat(TipoDeImagen.porTipoMime(" IMAGE/PNG ")).contains(TipoDeImagen.PNG);
    assertThat(TipoDeImagen.porTipoMime("image/webp")).contains(TipoDeImagen.WEBP);

    assertThat(TipoDeImagen.porTipoMime("image/svg+xml")).isEmpty();
    assertThat(TipoDeImagen.porTipoMime("application/pdf")).isEmpty();
    assertThat(TipoDeImagen.porTipoMime(null)).isEmpty();
  }

  @Test
  void cadaFormatoConoceSuTipoMimeYSuExtension() {
    assertThat(TipoDeImagen.JPEG.tipoMime()).isEqualTo("image/jpeg");
    assertThat(TipoDeImagen.JPEG.extension()).isEqualTo("jpg");
    assertThat(TipoDeImagen.WEBP.extension()).isEqualTo("webp");
  }

  private static byte[] contenidoWebp() {
    byte[] contenido = new byte[16];
    contenido[0] = 'R';
    contenido[1] = 'I';
    contenido[2] = 'F';
    contenido[3] = 'F';
    contenido[8] = 'W';
    contenido[9] = 'E';
    contenido[10] = 'B';
    contenido[11] = 'P';
    return contenido;
  }

  @Test
  void laFirmaWebpExigeLosDoceBytesDelContenedor() {
    assertThat(TipoDeImagen.porFirma(new byte[] {'R', 'I', 'F', 'F'}))
        .as("un RIFF truncado antes de la marca no se admite")
        .isEmpty();
  }
}
