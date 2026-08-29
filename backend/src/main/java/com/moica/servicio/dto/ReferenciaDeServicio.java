package com.moica.servicio.dto;

import com.moica.servicio.entity.EstadoServicio;
import com.moica.servicio.entity.ServicioPublicado;

/**
 * Lo mínimo que otra capacidad necesita de un servicio publicado.
 *
 * <p>Existe para que {@code solicitud} pueda validar elegibilidad y mostrar el nombre sin tocar el
 * repositorio ni la entidad de {@code servicio}.
 */
public record ReferenciaDeServicio(
    Long idServicioPublicado, Long idPrestador, String nombre, EstadoServicio estado) {

  public static ReferenciaDeServicio de(ServicioPublicado servicio) {
    return new ReferenciaDeServicio(
        servicio.getIdServicioPublicado(),
        servicio.getIdPrestador(),
        servicio.getNombre(),
        servicio.getEstado());
  }
}
