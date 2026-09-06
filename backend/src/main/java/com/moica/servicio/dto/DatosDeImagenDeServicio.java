package com.moica.servicio.dto;

import com.moica.servicio.entity.ImagenServicioPublicado;
import java.time.OffsetDateTime;

/** Vista de una imagen de un servicio publicado. */
public record DatosDeImagenDeServicio(
    Long idImagenServicioPublicado,
    String urlImagen,
    String textoAlternativo,
    short ordenVisualizacion,
    OffsetDateTime fechaCreacion) {

  public static DatosDeImagenDeServicio de(ImagenServicioPublicado imagen) {
    return new DatosDeImagenDeServicio(
        imagen.getIdImagenServicioPublicado(),
        imagen.getUrlImagen(),
        imagen.getTextoAlternativo(),
        imagen.getOrdenVisualizacion(),
        imagen.getFechaCreacion());
  }
}
