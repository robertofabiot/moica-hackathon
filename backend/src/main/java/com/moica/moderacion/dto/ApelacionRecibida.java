package com.moica.moderacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lo que la persona sancionada hizo llegar por el canal externo de soporte.
 *
 * <p>La apelación <b>no se presenta dentro de Moica</b>: la decisión D-MOD-04 y la definición 11.5
 * dejan fuera del MVP cualquier formulario, buzón o adjunto para el usuario. Lo que existe es esta
 * ruta administrativa, con la que una persona registra en el expediente lo que recibió fuera.
 *
 * <p>Por eso solo viaja el relato. No hay archivos, ni remitente, ni fecha de recepción declarada:
 * quién quedó afectado ya está en el caso y cuándo se registró lo fija el propio historial.
 */
public record ApelacionRecibida(
    @NotBlank(message = "Escribe lo que la persona expuso en su apelación.") @Size(max = 3000, message = "El relato no puede pasar de 3000 caracteres.") String relato) {

  public ApelacionRecibida {
    relato = relato == null ? null : relato.strip();
  }
}
