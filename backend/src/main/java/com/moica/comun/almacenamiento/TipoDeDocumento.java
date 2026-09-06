package com.moica.comun.almacenamiento;

import java.util.Optional;

/**
 * Formatos admitidos en el expediente documental de una verificación: JPEG, PNG y PDF.
 *
 * <p>Son los que fija {@code Docs/Core/prompt.md} §5, y no coinciden con los de la superficie
 * pública: aquí entra PDF —un certificado o una constancia suele serlo— y no entra WebP. Por eso
 * este dominio se declara aparte de {@link TipoDeImagen} en lugar de ampliarlo: las dos listas
 * cambian por motivos distintos y confundirlas dejaría un PDF colgando de un perfil público.
 *
 * <p>Cada formato conoce su firma binaria, porque la extensión y el {@code Content-Type} los
 * escribe el cliente y no demuestran nada: la decisión de admitir un archivo se toma mirando sus
 * primeros bytes.
 */
public enum TipoDeDocumento {
  JPEG("image/jpeg", "jpg"),
  PNG("image/png", "png"),
  PDF("application/pdf", "pdf");

  private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] FIRMA_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

  /** {@code %PDF-}, los cinco bytes con los que empieza todo PDF según su especificación. */
  private static final byte[] FIRMA_PDF = {0x25, 0x50, 0x44, 0x46, 0x2D};

  private final String tipoMime;
  private final String extension;

  TipoDeDocumento(String tipoMime, String extension) {
    this.tipoMime = tipoMime;
    this.extension = extension;
  }

  public String tipoMime() {
    return tipoMime;
  }

  public String extension() {
    return extension;
  }

  /** Resuelve el formato que declara la cabecera {@code Content-Type}, si es uno admitido. */
  public static Optional<TipoDeDocumento> porTipoMime(String tipoMime) {
    if (tipoMime == null) {
      return Optional.empty();
    }
    for (TipoDeDocumento tipo : values()) {
      if (tipo.tipoMime.equalsIgnoreCase(tipoMime.strip())) {
        return Optional.of(tipo);
      }
    }
    return Optional.empty();
  }

  /** Resuelve el formato real del contenido a partir de su firma binaria. */
  public static Optional<TipoDeDocumento> porFirma(byte[] contenido) {
    if (empiezaCon(contenido, FIRMA_JPEG)) {
      return Optional.of(JPEG);
    }
    if (empiezaCon(contenido, FIRMA_PNG)) {
      return Optional.of(PNG);
    }
    if (empiezaCon(contenido, FIRMA_PDF)) {
      return Optional.of(PDF);
    }
    return Optional.empty();
  }

  private static boolean empiezaCon(byte[] contenido, byte[] firma) {
    if (contenido == null || contenido.length < firma.length) {
      return false;
    }
    for (int posicion = 0; posicion < firma.length; posicion++) {
      if (contenido[posicion] != firma[posicion]) {
        return false;
      }
    }
    return true;
  }
}
