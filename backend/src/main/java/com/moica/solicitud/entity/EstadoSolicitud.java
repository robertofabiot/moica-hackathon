package com.moica.solicitud.entity;

/**
 * Estados de una solicitud de servicio, tal como los define el dominio {@code EstadoSolicitud}.
 *
 * <p>{@code RECHAZADA}, {@code CANCELADA} y {@code COMPLETADA} son definitivos: no se reabren.
 */
public enum EstadoSolicitud {
  PENDIENTE,
  ACEPTADA,
  RECHAZADA,
  CANCELADA,
  COMPLETADA;

  public boolean esDefinitivo() {
    return this == RECHAZADA || this == CANCELADA || this == COMPLETADA;
  }
}
