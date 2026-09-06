package com.moica.comun.almacenamiento;

import com.moica.comun.error.ErrorDeAplicacion;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * Reglas que debe cumplir una imagen pública antes de guardarse.
 *
 * <p>Se validan en el backend, nunca solo en el navegador: el tamaño contra el máximo configurado,
 * el {@code Content-Type} declarado contra la lista admitida y, sobre todo, la firma real del
 * contenido contra lo declarado. Un archivo cuyo interior no corresponde con su cabecera se rechaza
 * aunque la cabecera sea admisible.
 */
@Component
public class ValidacionDeImagen {

  private final PropiedadesDeImagenes propiedades;

  public ValidacionDeImagen(PropiedadesDeImagenes propiedades) {
    this.propiedades = propiedades;
  }

  /**
   * Comprueba una imagen recibida y devuelve su formato real.
   *
   * @param contenido bytes completos del archivo
   * @param tipoMimeDeclarado la cabecera {@code Content-Type} que envió el cliente
   * @throws ErrorDeAplicacion si el archivo está vacío, excede el máximo, declara un formato no
   *     admitido o su contenido no corresponde con lo declarado
   */
  public TipoDeImagen validar(byte[] contenido, String tipoMimeDeclarado) {
    if (contenido == null || contenido.length == 0) {
      throw imagenNoAdmitida("El archivo llegó vacío. Elige una imagen JPEG, PNG o WebP.");
    }
    long maximo = propiedades.tamanoMaximo().toBytes();
    if (contenido.length > maximo) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONTENT_TOO_LARGE,
          "IMAGEN_DEMASIADO_GRANDE",
          "La imagen supera el máximo de "
              + enMegabytes(propiedades.tamanoMaximo())
              + " MB. Reduce su tamaño e inténtalo otra vez.");
    }

    Optional<TipoDeImagen> declarado = TipoDeImagen.porTipoMime(tipoMimeDeclarado);
    if (declarado.isEmpty()) {
      throw imagenNoAdmitida("Solo se admiten imágenes JPEG, PNG o WebP.");
    }

    Optional<TipoDeImagen> real = TipoDeImagen.porFirma(contenido);
    if (real.isEmpty() || real.get() != declarado.get()) {
      // El mensaje no distingue a propósito entre «formato desconocido» y
      // «cabecera que miente»: a quien sube una imagen legítima le da igual, y
      // a quien prueba a colar otra cosa no hay que explicárselo.
      throw imagenNoAdmitida(
          "El contenido del archivo no corresponde con una imagen JPEG, PNG o WebP.");
    }
    return real.get();
  }

  private static ErrorDeAplicacion imagenNoAdmitida(String mensaje) {
    return new ErrorDeAplicacion(HttpStatus.BAD_REQUEST, "IMAGEN_NO_ADMITIDA", mensaje);
  }

  private static long enMegabytes(DataSize tamano) {
    return tamano.toMegabytes();
  }
}
