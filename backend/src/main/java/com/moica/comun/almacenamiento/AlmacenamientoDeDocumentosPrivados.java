package com.moica.comun.almacenamiento;

import java.net.URI;

/**
 * Almacén de los documentos privados del expediente de verificación.
 *
 * <p>Es la única frontera con el proveedor de objetos para esta superficie: la capacidad {@code
 * verificacion} habla con esta interfaz y no conoce el SDK. Existe como interfaz, y no como clase,
 * porque tiene dos implementaciones reales: {@link AlmacenamientoPrivadoR2} en producción y un
 * doble en memoria en las pruebas de integración, donde no hay bucket.
 *
 * <p>Se declara aparte de {@link AlmacenamientoDeImagenesPublicas} y **no** la amplía: son dos
 * buckets distintos con dos credenciales distintas, y sus operaciones no coinciden. Aquí no existe
 * «URL pública» ni {@code claveDe}; existe, en cambio, un acceso temporal que allí sería un
 * agujero.
 *
 * <p>Nada de lo que se guarda aquí es público. La base de datos conserva la clave opaca y los
 * metadatos; el binario vive solo en el bucket privado.
 */
public interface AlmacenamientoDeDocumentosPrivados {

  /**
   * Guarda un documento bajo una clave opaca.
   *
   * <p>No devuelve nada: a diferencia del almacén público, aquí no hay URL que persistir. Lo único
   * que la base de datos guarda es la clave con la que se pidió guardarlo.
   *
   * @param clave clave opaca generada por {@link ClavesDeDocumento}
   * @param contenido bytes ya validados del documento
   * @param tipoMime tipo real del contenido, comprobado contra su firma binaria
   * @throws com.moica.comun.error.ErrorDeAplicacion si el almacenamiento no está configurado o no
   *     responde
   */
  void guardar(String clave, byte[] contenido, String tipoMime);

  /**
   * Elimina un documento. Eliminar una clave que ya no existe no es un error.
   *
   * <p>Solo se usa como compensación de un envío que no llegó a persistirse. Los documentos de una
   * solicitud ya resuelta no se borran: son evidencia histórica.
   *
   * @throws com.moica.comun.error.ErrorDeAplicacion si el almacenamiento no está configurado o no
   *     responde
   */
  void eliminar(String clave);

  /**
   * Genera un acceso de lectura de vida corta para un administrador ya autorizado.
   *
   * <p>La URL se firma en el momento, caduca sola y no se guarda en ninguna parte: ni en la base de
   * datos, ni en un registro, ni en el cuerpo de una respuesta corriente. La autorización se
   * comprueba antes de llegar aquí, en cada petición.
   *
   * @throws com.moica.comun.error.ErrorDeAplicacion si el almacenamiento no está configurado o no
   *     responde
   */
  URI accesoTemporalDeLectura(String clave);
}
