package com.moica.comun.almacenamiento;

import java.util.UUID;

/**
 * Claves opacas para los documentos del expediente privado.
 *
 * <p>Mismo criterio que en la superficie pública y por razones más fuertes: la clave nunca sale del
 * nombre original del archivo —que puede llevar rutas, caracteres de control o el número de cédula
 * de quien lo subió— ni del identificador del prestador, porque una clave derivada de un
 * identificador secuencial se puede enumerar. Se usa un UUID aleatorio, generado con aleatoriedad
 * criptográfica, y la extensión del formato real detectado.
 *
 * <p>El prefijo es exclusivo del expediente. Aunque el bucket privado no comparte nada con el
 * público, separar la superficie dentro de él permite reconocer de un vistazo qué es cada objeto y
 * aplicar reglas de retención distintas más adelante.
 */
public final class ClavesDeDocumento {

  /** Prefijo de los documentos del expediente de verificación. */
  public static final String PREFIJO_EXPEDIENTES = "expedientes/";

  private ClavesDeDocumento() {}

  /** Una clave nueva, imposible de adivinar y sin rastro del nombre original ni de la cuenta. */
  public static String nueva(TipoDeDocumento tipo) {
    String aleatorio = UUID.randomUUID().toString().replace("-", "");
    return PREFIJO_EXPEDIENTES + aleatorio + "." + tipo.extension();
  }
}
