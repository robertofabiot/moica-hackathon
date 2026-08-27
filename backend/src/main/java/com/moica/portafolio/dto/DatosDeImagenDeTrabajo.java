package com.moica.portafolio.dto;

import com.moica.portafolio.entity.ImagenTrabajoPortafolio;
import java.time.OffsetDateTime;

/** Vista de una imagen de un trabajo del portafolio. */
public record DatosDeImagenDeTrabajo(
    Long idImagenTrabajoPortafolio,
    String urlImagen,
    String textoAlternativo,
    short ordenVisualizacion,
    OffsetDateTime fechaCreacion) {

  public static DatosDeImagenDeTrabajo de(ImagenTrabajoPortafolio imagen) {
    return new DatosDeImagenDeTrabajo(
        imagen.getIdImagenTrabajoPortafolio(),
        imagen.getUrlImagen(),
        imagen.getTextoAlternativo(),
        imagen.getOrdenVisualizacion(),
        imagen.getFechaCreacion());
  }
}
