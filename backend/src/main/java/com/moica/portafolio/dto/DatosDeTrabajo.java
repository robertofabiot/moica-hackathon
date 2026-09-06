package com.moica.portafolio.dto;

import com.moica.portafolio.entity.TrabajoPortafolio;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Vista de un trabajo del portafolio con sus imágenes en orden. */
public record DatosDeTrabajo(
    Long idTrabajo,
    String titulo,
    String descripcion,
    LocalDate fechaRealizacion,
    short ordenVisualizacion,
    List<DatosDeImagenDeTrabajo> imagenes,
    OffsetDateTime fechaCreacion,
    OffsetDateTime fechaActualizacion) {

  public DatosDeTrabajo {
    imagenes = List.copyOf(imagenes);
  }

  public static DatosDeTrabajo de(
      TrabajoPortafolio trabajo, List<DatosDeImagenDeTrabajo> imagenes) {
    return new DatosDeTrabajo(
        trabajo.getIdTrabajo(),
        trabajo.getTitulo(),
        trabajo.getDescripcion(),
        trabajo.getFechaRealizacion(),
        trabajo.getOrdenVisualizacion(),
        imagenes,
        trabajo.getFechaCreacion(),
        trabajo.getFechaActualizacion());
  }
}
