package com.moica.comun.almacenamiento;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Límites de los documentos del expediente de verificación.
 *
 * <p>El máximo por archivo es de 5 MB y se ajusta con {@code MOICA_DOCUMENTO_TAMANO_MAXIMO}, pero
 * solo hacia abajo: la restricción {@code ck_documento_verificacion_tamano} de PostgreSQL fija ese
 * tope y una configuración por encima admitiría archivos que la base rechazaría después.
 *
 * <p>El acceso temporal dura {@code PT5M} por omisión. Se limita a una hora porque un enlace que
 * vive horas deja de ser temporal: cualquiera que lo copie del historial del navegador o de un
 * registro intermedio seguiría abriendo un documento de identidad.
 *
 * @param tamanoMaximo tamaño máximo admitido por documento
 * @param duracionUrlTemporal cuánto vale la URL prefirmada que abre un administrador
 */
@ConfigurationProperties("moica.documentos")
public record PropiedadesDeDocumentos(DataSize tamanoMaximo, Duration duracionUrlTemporal) {

  /** Tope absoluto del diccionario de datos: 5 MB, en bytes. */
  public static final long TOPE_DE_TAMANO_EN_BYTES = 5L * 1024 * 1024;

  /** Más allá de esto un acceso «temporal» deja de serlo. */
  public static final Duration DURACION_MAXIMA = Duration.ofHours(1);

  public PropiedadesDeDocumentos {
    if (tamanoMaximo == null || tamanoMaximo.isNegative() || tamanoMaximo.toBytes() == 0) {
      throw new IllegalStateException(
          "moica.documentos.tamano-maximo debe ser positivo. Ajusta la variable de entorno "
              + "MOICA_DOCUMENTO_TAMANO_MAXIMO, por ejemplo a 5MB.");
    }
    if (tamanoMaximo.toBytes() > TOPE_DE_TAMANO_EN_BYTES) {
      throw new IllegalStateException(
          "moica.documentos.tamano-maximo no puede superar los 5MB: es el tope que impone la "
              + "restricción ck_documento_verificacion_tamano de PostgreSQL. Ajusta "
              + "MOICA_DOCUMENTO_TAMANO_MAXIMO a 5MB o menos.");
    }
    if (duracionUrlTemporal == null
        || duracionUrlTemporal.isNegative()
        || duracionUrlTemporal.isZero()) {
      throw new IllegalStateException(
          "moica.documentos.duracion-url-temporal debe ser positiva. Ajusta la variable de entorno "
              + "MOICA_DOCUMENTO_URL_TEMPORAL_DURACION, por ejemplo a PT5M.");
    }
    if (duracionUrlTemporal.compareTo(DURACION_MAXIMA) > 0) {
      throw new IllegalStateException(
          "moica.documentos.duracion-url-temporal no puede superar una hora: un acceso temporal "
              + "más largo deja de serlo. Ajusta MOICA_DOCUMENTO_URL_TEMPORAL_DURACION, por "
              + "ejemplo a PT5M.");
    }
  }
}
