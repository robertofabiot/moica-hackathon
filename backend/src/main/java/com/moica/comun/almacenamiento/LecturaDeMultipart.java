package com.moica.comun.almacenamiento;

import com.moica.comun.error.ErrorDeAplicacion;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

/** Lectura de un archivo multipart con el fallo de lectura traducido al error uniforme. */
public final class LecturaDeMultipart {

  private LecturaDeMultipart() {}

  /**
   * Los bytes completos del archivo recibido.
   *
   * @throws ErrorDeAplicacion si el flujo del navegador se cortó a media subida; es un problema de
   *     la petición, no del servidor
   */
  public static byte[] contenidoDe(MultipartFile archivo) {
    try {
      return archivo.getBytes();
    } catch (IOException fallo) {
      throw new ErrorDeAplicacion(
          HttpStatus.BAD_REQUEST,
          "SOLICITUD_INVALIDA",
          "No pudimos leer el archivo enviado. Inténtalo otra vez.");
    }
  }
}
