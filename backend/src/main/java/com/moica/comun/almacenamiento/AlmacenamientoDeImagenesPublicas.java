package com.moica.comun.almacenamiento;

import java.util.Optional;

/**
 * Almacén de las imágenes públicas de Moica.
 *
 * <p>Es la única frontera con el proveedor de objetos: las capacidades de perfil y portafolio
 * hablan con esta interfaz y no conocen el SDK. Existe como interfaz, y no como clase, porque tiene
 * dos implementaciones reales: {@link AlmacenamientoR2} en producción y un doble en memoria en las
 * pruebas de integración, donde no hay bucket.
 *
 * <p>Todo lo que se guarda aquí es público por diseño. Los documentos privados de verificación
 * (P4V) usarán otra configuración y otro bucket, con acceso temporal; no se mezclan con esta
 * superficie.
 */
public interface AlmacenamientoDeImagenesPublicas {

  /**
   * Guarda un objeto y devuelve su URL pública.
   *
   * @param clave clave opaca bajo la que se guarda, generada por {@link ClavesDeImagen}
   * @param contenido bytes ya validados de la imagen
   * @param tipoMime tipo real del contenido, con el que se servirá el objeto
   * @throws com.moica.comun.error.ErrorDeAplicacion si el almacenamiento no está configurado o no
   *     responde
   */
  String guardar(String clave, byte[] contenido, String tipoMime);

  /**
   * Elimina un objeto. Eliminar una clave que ya no existe no es un error.
   *
   * @throws com.moica.comun.error.ErrorDeAplicacion si el almacenamiento no está configurado o no
   *     responde
   */
  void eliminar(String clave);

  /**
   * Recupera la clave de un objeto a partir de su URL pública, si pertenece a este almacén.
   *
   * <p>Existe porque la base de datos guarda únicamente la URL, tal como fija el diccionario: para
   * borrar o sustituir un objeto hay que volver de la URL a la clave.
   */
  Optional<String> claveDe(String urlPublica);
}
