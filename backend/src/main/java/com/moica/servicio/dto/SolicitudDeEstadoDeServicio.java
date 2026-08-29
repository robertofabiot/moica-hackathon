package com.moica.servicio.dto;

import com.moica.servicio.entity.EstadoServicio;
import jakarta.validation.constraints.NotNull;

/** El estado que el prestador quiere dejar en un servicio propio. */
public record SolicitudDeEstadoDeServicio(@NotNull EstadoServicio estado) {}
