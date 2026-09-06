package com.moica.portafolio.dto;

import com.moica.comun.error.ErrorDeAplicacion;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;

/**
 * El texto alternativo que describe una imagen para accesibilidad.
 *
 * <p>Puede venir vacío o nulo: quitar la descripción es una edición válida. El máximo de 200
 * caracteres es el del diccionario.
 */
public record SolicitudDeTextoAlternativo(@Size(max = MAXIMO_CARACTERES) String textoAlternativo) {

  public static final int MAXIMO_CARACTERES = 200;

  public SolicitudDeTextoAlternativo {
    if (textoAlternativo != null) {
      textoAlternativo = textoAlternativo.strip();
      if (textoAlternativo.isEmpty()) {
        textoAlternativo = null;
      }
    }
  }

  /**
   * Aplica el mismo máximo que Bean Validation, para cuando el texto llega como campo de un
   * formulario multipart y no como JSON validado con {@code @Valid}.
   */
  public SolicitudDeTextoAlternativo exigirValida() {
    if (textoAlternativo != null && textoAlternativo.length() > MAXIMO_CARACTERES) {
      throw new ErrorDeAplicacion(
          HttpStatus.BAD_REQUEST,
          "VALIDACION",
          "El texto alternativo debe tener como máximo " + MAXIMO_CARACTERES + " caracteres.");
    }
    return this;
  }
}
