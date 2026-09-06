package com.moica.comun.almacenamiento;

import java.util.UUID;

/**
 * Claves opacas para los objetos del almacén público.
 *
 * <p>La clave nunca sale del nombre original del archivo: un nombre elegido por el cliente puede
 * llevar rutas, caracteres de control o datos personales, y además sería adivinable. Se usa un UUID
 * aleatorio —generado con aleatoriedad criptográfica— y la extensión del formato real detectado, no
 * la que dijera el archivo.
 *
 * <p>Los prefijos separan las superficies: las imágenes de perfil, las de trabajos y las de
 * servicios conviven en el mismo bucket público pero no se confunden entre sí.
 */
public final class ClavesDeImagen {

  /** Prefijo de las imágenes de perfil de prestador. */
  public static final String PREFIJO_PERFILES = "perfiles/";

  /** Prefijo de las imágenes de trabajos del portafolio. */
  public static final String PREFIJO_TRABAJOS = "trabajos/";

  /** Prefijo de las imágenes de servicios publicados. */
  public static final String PREFIJO_SERVICIOS = "servicios/";

  private ClavesDeImagen() {}

  /** Una clave nueva, imposible de adivinar y sin rastro del nombre original. */
  public static String nueva(String prefijo, TipoDeImagen tipo) {
    String aleatorio = UUID.randomUUID().toString().replace("-", "");
    return prefijo + aleatorio + "." + tipo.extension();
  }
}
