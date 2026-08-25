package com.moica.prestador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Una entrada libre de contacto: número, correo, usuario, enlace o indicación escrita.
 *
 * <p>El máximo de 500 caracteres es el del diccionario. Moica no valida el formato a propósito: la
 * definición 5.4 los deja libres y sin clasificación por plataforma.
 */
public record SolicitudDeMedioContacto(@NotBlank @Size(max = 500) String contenido) {

  public SolicitudDeMedioContacto {
    contenido = (contenido == null) ? null : contenido.strip();
  }
}
