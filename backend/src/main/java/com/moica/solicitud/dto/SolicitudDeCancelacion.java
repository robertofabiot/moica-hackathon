package com.moica.solicitud.dto;

import jakarta.validation.constraints.Size;

/**
 * Motivo opcional al cancelar.
 *
 * <p>Cuando la solicitud está {@code PENDIENTE} no se exige. Cuando está {@code ACEPTADA} el
 * servicio lo vuelve obligatorio: un motivo en blanco no alcanza.
 */
public record SolicitudDeCancelacion(@Size(max = 2000) String motivo) {

  public SolicitudDeCancelacion {
    motivo = motivo == null ? null : motivo.strip();
    if (motivo != null && motivo.isEmpty()) {
      motivo = null;
    }
  }
}
