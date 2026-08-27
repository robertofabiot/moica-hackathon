package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Los tres formatos del expediente y sus firmas binarias.
 *
 * <p>Lo que se comprueba no es la lista sino que la decisión se toma mirando los bytes: la
 * extensión y el {@code Content-Type} los escribe el cliente y no demuestran nada.
 */
class TipoDeDocumentoTest {

  @Test
  void reconoceLosTresTiposMimeAdmitidos() {
    assertThat(TipoDeDocumento.porTipoMime("image/jpeg")).contains(TipoDeDocumento.JPEG);
    assertThat(TipoDeDocumento.porTipoMime("image/png")).contains(TipoDeDocumento.PNG);
    assertThat(TipoDeDocumento.porTipoMime("application/pdf")).contains(TipoDeDocumento.PDF);
  }

  @Test
  void admiteEspaciosYMayusculasEnElTipoDeclarado() {
    assertThat(TipoDeDocumento.porTipoMime("  Application/PDF ")).contains(TipoDeDocumento.PDF);
  }

  @Test
  void noAdmiteWebpAunqueSeaImagenValidaEnLaSuperficiePublica() {
    assertThat(TipoDeDocumento.porTipoMime("image/webp")).isEmpty();
    assertThat(TipoDeDocumento.porTipoMime("image/svg+xml")).isEmpty();
    assertThat(TipoDeDocumento.porTipoMime(null)).isEmpty();
  }

  @Test
  void reconoceCadaFormatoPorSuFirmaReal() {
    assertThat(TipoDeDocumento.porFirma(jpeg())).contains(TipoDeDocumento.JPEG);
    assertThat(TipoDeDocumento.porFirma(png())).contains(TipoDeDocumento.PNG);
    assertThat(TipoDeDocumento.porFirma(pdf())).contains(TipoDeDocumento.PDF);
  }

  @Test
  void unContenidoQueNoEmpiezaPorNingunaFirmaNoSeReconoce() {
    assertThat(TipoDeDocumento.porFirma("texto cualquiera".getBytes(StandardCharsets.UTF_8)))
        .isEmpty();
    assertThat(TipoDeDocumento.porFirma(new byte[0])).isEmpty();
    assertThat(TipoDeDocumento.porFirma(null)).isEmpty();
  }

  @Test
  void unContenidoMasCortoQueLaFirmaNoSeConfundeConElla() {
    assertThat(TipoDeDocumento.porFirma(new byte[] {(byte) 0x89, 0x50})).isEmpty();
  }

  private static byte[] jpeg() {
    return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
  }

  private static byte[] png() {
    return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
  }

  private static byte[] pdf() {
    return "%PDF-1.7 contenido".getBytes(StandardCharsets.US_ASCII);
  }
}
