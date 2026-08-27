package com.moica.comun.almacenamiento;

import com.moica.comun.error.ErrorDeAplicacion;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * Reglas que debe cumplir un documento del expediente antes de guardarse.
 *
 * <p>Se validan en el backend, nunca solo en el navegador: el archivo no puede llegar vacío, no
 * puede superar el máximo configurado, su {@code Content-Type} debe estar en la lista admitida y,
 * sobre todo, su firma real debe corresponder con lo declarado. Un PDF disfrazado de imagen —o al
 * revés— se rechaza aunque la cabecera sea admisible.
 */
@Component
public class ValidacionDeDocumento {

  private final PropiedadesDeDocumentos propiedades;

  public ValidacionDeDocumento(PropiedadesDeDocumentos propiedades) {
    this.propiedades = propiedades;
  }

  /**
   * Comprueba un documento recibido y devuelve su formato real.
   *
   * @param contenido bytes completos del archivo
   * @param tipoMimeDeclarado la cabecera {@code Content-Type} que envió el cliente
   * @throws ErrorDeAplicacion si el archivo está vacío, excede el máximo, declara un formato no
   *     admitido o su contenido no corresponde con lo declarado
   */
  public TipoDeDocumento validar(byte[] contenido, String tipoMimeDeclarado) {
    if (contenido == null || contenido.length == 0) {
      throw documentoNoAdmitido("El archivo llegó vacío. Adjunta un JPEG, un PNG o un PDF.");
    }
    long maximo = propiedades.tamanoMaximo().toBytes();
    if (contenido.length > maximo) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONTENT_TOO_LARGE,
          "DOCUMENTO_DEMASIADO_GRANDE",
          "El documento supera el máximo de "
              + enMegabytes(propiedades.tamanoMaximo())
              + " MB. Reduce su tamaño e inténtalo otra vez.");
    }

    Optional<TipoDeDocumento> declarado = TipoDeDocumento.porTipoMime(tipoMimeDeclarado);
    if (declarado.isEmpty()) {
      throw documentoNoAdmitido("Solo se admiten documentos JPEG, PNG o PDF.");
    }

    Optional<TipoDeDocumento> real = TipoDeDocumento.porFirma(contenido);
    if (real.isEmpty() || real.get() != declarado.get()) {
      // El mensaje no distingue a propósito entre «formato desconocido» y
      // «cabecera que miente»: a quien adjunta un documento legítimo le da
      // igual, y a quien prueba a colar otra cosa no hay que explicárselo.
      throw documentoNoAdmitido(
          "El contenido del archivo no corresponde con un documento JPEG, PNG o PDF.");
    }
    return real.get();
  }

  private static ErrorDeAplicacion documentoNoAdmitido(String mensaje) {
    return new ErrorDeAplicacion(HttpStatus.BAD_REQUEST, "DOCUMENTO_NO_ADMITIDO", mensaje);
  }

  private static long enMegabytes(DataSize tamano) {
    return tamano.toMegabytes();
  }
}
