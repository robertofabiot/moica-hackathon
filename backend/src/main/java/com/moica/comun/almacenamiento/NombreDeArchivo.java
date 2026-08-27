package com.moica.comun.almacenamiento;

/**
 * Saneamiento del nombre con el que llegó un archivo.
 *
 * <p>El nombre original se conserva porque le sirve al prestador para reconocer qué subió y al
 * administrador para leer el expediente. Pero llega escrito por el cliente, así que antes de
 * guardarlo se le quita todo lo que pueda hacer daño donde después se muestre o se escriba:
 *
 * <ul>
 *   <li>Las rutas. {@code ../../etc/passwd} o {@code C:\Users\...} se reducen al último segmento,
 *       de modo que el valor guardado nunca puede leerse como una ruta.
 *   <li>Los caracteres de control, incluidos el nulo y los saltos de línea, que partirían una línea
 *       de registro en dos y permitirían inyectar una entrada falsa.
 *   <li>Los caracteres que Windows no admite en un nombre de archivo, para que descargarlo o
 *       guardarlo no dependa del sistema de quien lo abra.
 * </ul>
 *
 * <p>Lo que queda se recorta a 255 caracteres, que es lo que admite la columna {@code
 * nombre_original}. La clave con la que se guarda el objeto **no** se deriva de aquí: es opaca y la
 * genera {@link ClavesDeDocumento}.
 */
public final class NombreDeArchivo {

  /** Cuando no queda nada utilizable, el expediente muestra esto en lugar de un hueco. */
  public static final String NOMBRE_POR_OMISION = "documento";

  private static final int LARGO_MAXIMO = 255;

  private NombreDeArchivo() {}

  /** El nombre listo para persistirse: sin ruta, sin control y de largo acotado. */
  public static String saneado(String nombreOriginal) {
    if (nombreOriginal == null) {
      return NOMBRE_POR_OMISION;
    }

    String ultimoSegmento = nombreOriginal;
    int separador = Math.max(ultimoSegmento.lastIndexOf('/'), ultimoSegmento.lastIndexOf('\\'));
    if (separador >= 0) {
      ultimoSegmento = ultimoSegmento.substring(separador + 1);
    }

    StringBuilder limpio = new StringBuilder(ultimoSegmento.length());
    for (int posicion = 0; posicion < ultimoSegmento.length(); posicion++) {
      char caracter = ultimoSegmento.charAt(posicion);
      if (esAdmisible(caracter)) {
        limpio.append(caracter);
      }
    }

    // Un nombre compuesto solo de puntos («..») seguiría leyéndose como una
    // referencia a un directorio; se descarta igual que si estuviera vacío.
    String saneado = limpio.toString().strip().replaceAll("\\s+", " ");
    if (saneado.isEmpty() || saneado.chars().allMatch(caracter -> caracter == '.')) {
      return NOMBRE_POR_OMISION;
    }
    return saneado.length() > LARGO_MAXIMO ? saneado.substring(0, LARGO_MAXIMO) : saneado;
  }

  private static boolean esAdmisible(char caracter) {
    if (caracter < ' ' || caracter == '\u007F') {
      return false;
    }
    return "<>:\"|?*".indexOf(caracter) < 0;
  }
}
