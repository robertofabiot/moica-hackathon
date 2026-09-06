package com.moica.solicitud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Datos con los que un cliente envía una solicitud a un servicio ajeno.
 *
 * <p>La descripción y la ubicación son {@code TEXT} en el diccionario; el máximo de 3000 y 2000
 * caracteres es un límite de la aplicación, documentado en el contrato, igual que en los demás
 * textos libres de Moica.
 */
public record SolicitudDeContratacion(
    @NotNull @Positive Long idServicioPublicado,
    @NotBlank @Size(max = 3000) String descripcionNecesidad,
    @NotNull @Positive Integer idMunicipio,
    @NotBlank @Size(max = 2000) String indicacionUbicacion,
    LocalDate fechaPreferida) {

  public SolicitudDeContratacion {
    descripcionNecesidad = strip(descripcionNecesidad);
    indicacionUbicacion = strip(indicacionUbicacion);
  }

  private static String strip(String valor) {
    return (valor == null) ? null : valor.strip();
  }
}
