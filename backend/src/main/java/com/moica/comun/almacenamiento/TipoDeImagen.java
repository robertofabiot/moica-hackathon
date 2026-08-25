package com.moica.comun.almacenamiento;

import java.util.Optional;

/**
 * Formatos de imagen admitidos en la superficie pública: JPEG, PNG y WebP.
 *
 * <p>Cada formato conoce su firma binaria, porque la extensión y el {@code Content-Type} los
 * escribe el cliente y no demuestran nada: la decisión de admitir un archivo se toma mirando sus
 * primeros bytes. SVG y PDF quedan fuera a propósito; un SVG puede ejecutar script en el navegador
 * de quien lo mire.
 */
public enum TipoDeImagen {
  JPEG("image/jpeg", "jpg"),
  PNG("image/png", "png"),
  WEBP("image/webp", "webp");

  private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] FIRMA_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] FIRMA_RIFF = {0x52, 0x49, 0x46, 0x46};
  private static final byte[] MARCA_WEBP = {0x57, 0x45, 0x42, 0x50};

  private final String tipoMime;
  private final String extension;

  TipoDeImagen(String tipoMime, String extension) {
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
  public static Optional<TipoDeImagen> porTipoMime(String tipoMime) {
    if (tipoMime == null) {
      return Optional.empty();
    }
    for (TipoDeImagen tipo : values()) {
      if (tipo.tipoMime.equalsIgnoreCase(tipoMime.strip())) {
        return Optional.of(tipo);
      }
    }
    return Optional.empty();
  }

  /** Resuelve el formato real del contenido a partir de su firma binaria. */
  public static Optional<TipoDeImagen> porFirma(byte[] contenido) {
    if (empiezaCon(contenido, FIRMA_JPEG, 0)) {
      return Optional.of(JPEG);
    }
    if (empiezaCon(contenido, FIRMA_PNG, 0)) {
      return Optional.of(PNG);
    }
    // WebP es un contenedor RIFF: `RIFF` en los bytes 0-3 y `WEBP` en los 8-11.
    if (empiezaCon(contenido, FIRMA_RIFF, 0) && empiezaCon(contenido, MARCA_WEBP, 8)) {
      return Optional.of(WEBP);
    }
    return Optional.empty();
  }

  private static boolean empiezaCon(byte[] contenido, byte[] firma, int desde) {
    if (contenido == null || contenido.length < desde + firma.length) {
      return false;
    }
    for (int posicion = 0; posicion < firma.length; posicion++) {
      if (contenido[desde + posicion] != firma[posicion]) {
        return false;
      }
    }
    return true;
  }
}
