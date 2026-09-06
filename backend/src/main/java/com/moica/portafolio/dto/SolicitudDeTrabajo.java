package com.moica.portafolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Datos con los que se crea o se actualiza un trabajo del portafolio.
 *
 * <p>El máximo del título es el del diccionario (150 caracteres). La descripción es {@code TEXT}
 * sin tope en el modelo; el máximo de 3000 caracteres es un límite de la aplicación documentado en
 * el contrato de la API. La fecha de realización es opcional: solo se muestra si el prestador
 * quiere indicarla.
 */
public record SolicitudDeTrabajo(
    @NotBlank @Size(max = 150) String titulo,
    @NotBlank @Size(max = 3000) String descripcion,
    LocalDate fechaRealizacion) {

  public SolicitudDeTrabajo {
    titulo = strip(titulo);
    descripcion = strip(descripcion);
  }

  private static String strip(String valor) {
    return (valor == null) ? null : valor.strip();
  }
}
