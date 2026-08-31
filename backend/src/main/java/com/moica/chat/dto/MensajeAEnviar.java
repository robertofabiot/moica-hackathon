package com.moica.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El texto que un participante envía al hilo de una solicitud.
 *
 * <p>Solo lleva el contenido: el remitente sale siempre de la sesión y nunca del cuerpo, de modo
 * que escribir en nombre de otra persona no es una petición que se pueda formular.
 *
 * <p>El contenido se recorta antes de validarlo, así que un mensaje de espacios queda vacío y
 * {@link NotBlank} lo rechaza. El diccionario modela {@code contenido} como {@code TEXT} sin
 * máximo; el tope de {@value #MAXIMO_CARACTERES} caracteres es un límite de la aplicación,
 * documentado en el contrato, igual que en los demás textos libres de Moica.
 */
public record MensajeAEnviar(
    @NotBlank @Size(max = MensajeAEnviar.MAXIMO_CARACTERES) String contenido) {

  public static final int MAXIMO_CARACTERES = 2000;

  public MensajeAEnviar {
    contenido = (contenido == null) ? null : contenido.strip();
  }
}
