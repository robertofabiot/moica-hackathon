package com.moica.verificacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El motivo con el que un administrador cierra una solicitud en negativo.
 *
 * <p>Es obligatorio y no puede quedar en blanco: lo exige la definición del producto para todo
 * rechazo y toda revocación, y la restricción {@code ck_solicitud_verificacion_observacion} lo
 * vuelve a exigir en PostgreSQL. Sin motivo, el prestador no sabría qué corregir ni por qué perdió
 * su insignia.
 *
 * <p>La columna es {@code TEXT} y no impone tope; el máximo de 1000 caracteres es un límite de la
 * aplicación, documentado en el contrato de la API, para que un motivo siga siendo un motivo y no
 * un expediente paralelo.
 */
public record SolicitudDeResolucion(@NotBlank @Size(max = 1000) String observacion) {

  public SolicitudDeResolucion {
    observacion = (observacion == null) ? null : observacion.strip();
  }
}
