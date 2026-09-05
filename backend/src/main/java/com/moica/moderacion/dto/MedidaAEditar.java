package com.moica.moderacion.dto;

import com.moica.usuario.entity.EstadoCuenta;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Los datos reescribibles de una medida del catálogo.
 *
 * <p>No lleva código ni estado de habilitación: el primero es inmutable y el segundo se cambia por
 * su propio recurso, para que habilitar o deshabilitar no dependa de reenviar la ficha entera.
 *
 * <p>Editar afecta a lo que venga, no a lo aplicado: cada decisión pasada conserva en su versión
 * del historial el estado de cuenta y la fecha que realmente se le impusieron.
 */
public record MedidaAEditar(
    @NotBlank(message = "Escribe el nombre de la medida.") @Size(max = 100, message = "El nombre no puede pasar de 100 caracteres.") String nombre,
    @Size(max = 2000, message = "La descripción no puede pasar de 2000 caracteres.") String descripcion,
    @Min(value = 1, message = "El nivel de severidad empieza en 1.") @Max(value = 100, message = "El nivel de severidad no pasa de 100.") short nivelSeveridad,
    EstadoCuenta estadoCuentaResultante,
    boolean requiereFechaFin) {

  public MedidaAEditar {
    nombre = nombre == null ? null : nombre.strip();
    descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
  }
}
