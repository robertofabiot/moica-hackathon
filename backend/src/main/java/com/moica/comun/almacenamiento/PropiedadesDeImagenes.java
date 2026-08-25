package com.moica.comun.almacenamiento;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Límites de las imágenes públicas que sube un prestador.
 *
 * <p>El máximo inicial es de 5 MB por imagen y se ajusta con la variable de entorno {@code
 * MOICA_IMAGEN_TAMANO_MAXIMO}, sin tocar código. El tope de transporte de las peticiones multipart
 * es aparte y más alto (ver {@code application.properties}): así el rechazo por tamaño lo produce
 * esta regla, con su mensaje y su código, y no el contenedor.
 *
 * @param tamanoMaximo tamaño máximo admitido por imagen
 */
@ConfigurationProperties("moica.imagenes")
public record PropiedadesDeImagenes(DataSize tamanoMaximo) {

  public PropiedadesDeImagenes {
    if (tamanoMaximo == null || tamanoMaximo.isNegative() || tamanoMaximo.toBytes() == 0) {
      throw new IllegalStateException(
          "moica.imagenes.tamano-maximo debe ser positivo. Ajusta la variable de entorno "
              + "MOICA_IMAGEN_TAMANO_MAXIMO, por ejemplo a 5MB.");
    }
  }
}
