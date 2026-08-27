package com.moica.comun.almacenamiento;

import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión con el almacenamiento de objetos público (Cloudflare R2), toda por entorno.
 *
 * <p>Las cinco variables van juntas: o están todas y el almacenamiento funciona, o no está ninguna
 * y la aplicación arranca igualmente sin él. Arrancar sin credenciales es deliberado: el resto de
 * Moica no depende de las imágenes, y exigir un bucket para desarrollar el inicio de sesión sería
 * un peaje sin beneficio. Una configuración a medias, en cambio, es un error de despliegue y se
 * rechaza en el arranque.
 *
 * <p>La URL pública se configura aparte del endpoint de la API de R2 porque son cosas distintas: el
 * endpoint es donde el backend escribe con sus credenciales, y la URL pública es donde los
 * navegadores leen sin ninguna. Admite tanto el subdominio {@code r2.dev} de desarrollo como un
 * dominio propio, sin que el código dependa de cuál sea.
 *
 * @param idCuenta identificador de la cuenta de Cloudflare, parte del endpoint S3 de R2
 * @param accessKeyId identificador del token de API con permisos solo sobre los objetos del bucket
 * @param secretAccessKey secreto del token. Nunca se versiona: llega por {@code
 *     MOICA_R2_SECRET_ACCESS_KEY}
 * @param bucketPublico nombre del bucket ya aprovisionado para las imágenes públicas
 * @param urlPublicaBase origen HTTPS desde el que se sirven los objetos del bucket
 */
@ConfigurationProperties("moica.almacenamiento")
public record PropiedadesDeAlmacenamiento(
    String idCuenta,
    String accessKeyId,
    String secretAccessKey,
    String bucketPublico,
    String urlPublicaBase) {

  public PropiedadesDeAlmacenamiento {
    idCuenta = normalizar(idCuenta);
    accessKeyId = normalizar(accessKeyId);
    secretAccessKey = normalizar(secretAccessKey);
    bucketPublico = normalizar(bucketPublico);
    urlPublicaBase = sinBarraFinal(normalizar(urlPublicaBase));

    boolean algunaDefinida =
        !idCuenta.isEmpty()
            || !accessKeyId.isEmpty()
            || !secretAccessKey.isEmpty()
            || !bucketPublico.isEmpty()
            || !urlPublicaBase.isEmpty();

    if (algunaDefinida
        && !todasDefinidas(idCuenta, accessKeyId, secretAccessKey, bucketPublico, urlPublicaBase)) {
      // Los valores no se incluyen en el mensaje: uno de ellos es un secreto y
      // acabaría en los registros de arranque.
      throw new IllegalStateException(
          "El almacenamiento de imágenes está configurado a medias. Define las cinco variables "
              + "MOICA_R2_ID_CUENTA, MOICA_R2_ACCESS_KEY_ID, MOICA_R2_SECRET_ACCESS_KEY, "
              + "MOICA_R2_BUCKET_PUBLICO y MOICA_R2_URL_PUBLICA_BASE, o ninguna.");
    }
    if (!urlPublicaBase.isEmpty()
        && !urlPublicaBase.toLowerCase(Locale.ROOT).startsWith("https://")) {
      throw new IllegalStateException(
          "moica.almacenamiento.url-publica-base debe empezar por https://. Las imágenes públicas "
              + "se sirven siempre cifradas, tanto desde r2.dev como desde un dominio propio.");
    }
  }

  /** Si las cinco variables están presentes y el almacenamiento puede usarse. */
  public boolean estaConfigurado() {
    return !secretAccessKey.isEmpty();
  }

  /** Dirección pública desde la que un navegador lee el objeto guardado bajo la clave. */
  public String urlPublicaDe(String clave) {
    return urlPublicaBase + "/" + clave;
  }

  /**
   * Recupera la clave de un objeto a partir de su URL pública.
   *
   * <p>Devuelve vacío si la URL no pertenece a la base configurada: pasa con datos anteriores a un
   * cambio de dominio, y en ese caso el objeto antiguo no puede localizarse para borrarlo.
   */
  public Optional<String> claveDe(String urlPublica) {
    String prefijo = urlPublicaBase + "/";
    if (urlPublica == null || urlPublicaBase.isEmpty() || !urlPublica.startsWith(prefijo)) {
      return Optional.empty();
    }
    String clave = urlPublica.substring(prefijo.length());
    return clave.isEmpty() ? Optional.empty() : Optional.of(clave);
  }

  /**
   * Se redefine a propósito: la representación que genera el compilador para un record incluye
   * todos sus componentes, y dos de ellos son las mitades de una credencial.
   *
   * <p>El identificador del token se oculta igual que su secreto. No es el secreto, pero es
   * material de credencial: nombra exactamente qué token usa Moica contra R2, y publicarlo en un
   * registro o en el mensaje de una excepción convierte una fuga del secreto en una credencial
   * completa y le ahorra la mitad del trabajo a quien la busque. Lo que no es credencial —cuenta,
   * bucket y base pública— sigue a la vista, porque es lo que hace útil esta representación al
   * diagnosticar un despliegue.
   */
  @Override
  public String toString() {
    return "PropiedadesDeAlmacenamiento[idCuenta="
        + idCuenta
        + ", accessKeyId=(oculto), secretAccessKey=(oculto), bucketPublico="
        + bucketPublico
        + ", urlPublicaBase="
        + urlPublicaBase
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

  private static String sinBarraFinal(String valor) {
    return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
  }
}
