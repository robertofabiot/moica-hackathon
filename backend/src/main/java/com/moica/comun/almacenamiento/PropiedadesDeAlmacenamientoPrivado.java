package com.moica.comun.almacenamiento;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión con el almacenamiento privado de expedientes (Cloudflare R2), toda por entorno.
 *
 * <p>Es un bucket **distinto** del público de P4 y con un token **distinto**, tal como fija {@code
 * Docs/Dev/Almacenamiento.md}: un fallo de configuración en el bucket de imágenes no puede exponer
 * un documento de identidad. Por eso estas cuatro variables no reutilizan ninguna de las cinco de
 * la superficie pública.
 *
 * <p>No hay URL pública que configurar, y esa ausencia es la propia decisión: el bucket privado no
 * tiene lectura anónima. Lo único que sale de él es una URL prefirmada de vida corta que se genera
 * en cada petición autorizada y no se guarda en ninguna parte.
 *
 * <p>Las cuatro van juntas: o están todas y el expediente funciona, o no está ninguna y la
 * aplicación arranca igualmente sin él. Una configuración a medias es un error de despliegue y
 * detiene el arranque.
 *
 * @param idCuenta identificador de la cuenta de Cloudflare, parte del endpoint S3 de R2
 * @param accessKeyId identificador del token de API, limitado al bucket privado
 * @param secretAccessKey secreto del token. Nunca se versiona: llega por {@code
 *     MOICA_R2_PRIVADO_SECRET_ACCESS_KEY}
 * @param bucketPrivado nombre del bucket ya aprovisionado, sin acceso público
 */
@ConfigurationProperties("moica.almacenamiento-privado")
public record PropiedadesDeAlmacenamientoPrivado(
    String idCuenta, String accessKeyId, String secretAccessKey, String bucketPrivado) {

  public PropiedadesDeAlmacenamientoPrivado {
    idCuenta = normalizar(idCuenta);
    accessKeyId = normalizar(accessKeyId);
    secretAccessKey = normalizar(secretAccessKey);
    bucketPrivado = normalizar(bucketPrivado);

    boolean algunaDefinida =
        !idCuenta.isEmpty()
            || !accessKeyId.isEmpty()
            || !secretAccessKey.isEmpty()
            || !bucketPrivado.isEmpty();

    if (algunaDefinida && !todasDefinidas(idCuenta, accessKeyId, secretAccessKey, bucketPrivado)) {
      // Los valores no se incluyen en el mensaje: uno de ellos es un secreto y
      // acabaría en los registros de arranque.
      throw new IllegalStateException(
          "El almacenamiento privado de expedientes está configurado a medias. Define las cuatro "
              + "variables MOICA_R2_PRIVADO_ID_CUENTA, MOICA_R2_PRIVADO_ACCESS_KEY_ID, "
              + "MOICA_R2_PRIVADO_SECRET_ACCESS_KEY y MOICA_R2_BUCKET_PRIVADO, o ninguna.");
    }
  }

  /** Si las cuatro variables están presentes y el almacenamiento privado puede usarse. */
  public boolean estaConfigurado() {
    return !secretAccessKey.isEmpty();
  }

  /**
   * Se redefine a propósito, por el mismo motivo que en la superficie pública: la representación
   * que genera el compilador para un record incluye todos sus componentes, y dos de ellos son las
   * mitades de una credencial.
   *
   * <p>El identificador del token se oculta igual que su secreto: nombra exactamente qué token usa
   * Moica contra el bucket privado, y publicarlo convertiría una fuga del secreto en una credencial
   * completa.
   */
  @Override
  public String toString() {
    return "PropiedadesDeAlmacenamientoPrivado[idCuenta="
        + idCuenta
        + ", accessKeyId=(oculto), secretAccessKey=(oculto), bucketPrivado="
        + bucketPrivado
        + "]";
  }

  private static boolean todasDefinidas(String... valores) {
    for (String valor : valores) {
      if (valor.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private static String normalizar(String valor) {
    return (valor == null) ? "" : valor.strip();
  }
}
