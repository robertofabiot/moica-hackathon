package com.moica.servicio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos con los que se crea o se actualiza un servicio propio.
 *
 * <p>El máximo del nombre es el del diccionario (150 caracteres). La descripción es {@code TEXT}
 * sin tope en el modelo; el máximo de 3000 caracteres es un límite de la aplicación documentado en
 * el contrato. El precio es opcional y, si existe, mayor que cero con dos decimales.
 */
public record SolicitudDeServicio(
    @NotBlank @Size(max = 150) String nombre,
    @NotBlank @Size(max = 3000) String descripcion,
    @NotNull @Positive Integer idSubcategoriaServicio,
    @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal precioReferencia) {

  public SolicitudDeServicio {
    nombre = strip(nombre);
    descripcion = strip(descripcion);
  }

  private static String strip(String valor) {
    return (valor == null) ? null : valor.strip();
  }
}
