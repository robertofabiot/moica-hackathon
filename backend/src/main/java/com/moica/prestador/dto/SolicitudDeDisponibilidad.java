package com.moica.prestador.dto;

import com.moica.prestador.entity.EstadoDisponibilidad;
import jakarta.validation.constraints.NotNull;

/** El estado de disponibilidad que el prestador quiere dejar puesto. */
public record SolicitudDeDisponibilidad(@NotNull EstadoDisponibilidad disponibilidad) {}
