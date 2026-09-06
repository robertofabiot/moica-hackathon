package com.moica.servicio.dto;

import com.moica.catalogo.dto.ClasificacionDeServicio;
import com.moica.servicio.entity.EstadoServicio;
import com.moica.servicio.entity.ServicioPublicado;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Vista del servicio que recibe su propietario, con clasificación e imágenes. */
public record DatosDeServicioPublicado(
    Long idServicioPublicado,
    String nombre,
    String descripcion,
    BigDecimal precioReferencia,
    EstadoServicio estado,
    Short idCategoriaServicio,
    String nombreCategoria,
    Integer idSubcategoriaServicio,
    String nombreSubcategoria,
    List<DatosDeImagenDeServicio> imagenes,
    OffsetDateTime fechaCreacion,
    OffsetDateTime fechaActualizacion) {

  public DatosDeServicioPublicado {
    imagenes = List.copyOf(imagenes);
  }

  public static DatosDeServicioPublicado de(
      ServicioPublicado servicio,
      ClasificacionDeServicio clasificacion,
      List<DatosDeImagenDeServicio> imagenes) {
    return new DatosDeServicioPublicado(
        servicio.getIdServicioPublicado(),
        servicio.getNombre(),
        servicio.getDescripcion(),
        servicio.getPrecioReferencia(),
        servicio.getEstado(),
        clasificacion.idCategoriaServicio(),
        clasificacion.nombreCategoria(),
        clasificacion.idSubcategoriaServicio(),
        clasificacion.nombreSubcategoria(),
        imagenes,
        servicio.getFechaCreacion(),
        servicio.getFechaActualizacion());
  }
}
