package com.moica.servicio.dto;

import com.moica.catalogo.dto.ClasificacionDeServicio;
import com.moica.prestador.dto.DatosPublicosDePrestador;
import com.moica.servicio.entity.ServicioPublicado;
import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle público de un servicio visible.
 *
 * <p>{@code admiteContratacion} avisa si hoy se podría solicitar; las solicitudes llegan en P6.
 * {@code precioReferencia} nulo se presenta como «A convenir» en la interfaz, no en este campo.
 */
public record DetallePublicoDeServicio(
    Long idServicioPublicado,
    String nombre,
    String descripcion,
    BigDecimal precioReferencia,
    Short idCategoriaServicio,
    String nombreCategoria,
    Integer idSubcategoriaServicio,
    String nombreSubcategoria,
    List<DatosDeImagenDeServicio> imagenes,
    boolean admiteContratacion,
    DatosPublicosDePrestador prestador) {

  public DetallePublicoDeServicio {
    imagenes = List.copyOf(imagenes);
  }

  public static DetallePublicoDeServicio de(
      ServicioPublicado servicio,
      ClasificacionDeServicio clasificacion,
      List<DatosDeImagenDeServicio> imagenes,
      DatosPublicosDePrestador prestador) {
    return new DetallePublicoDeServicio(
        servicio.getIdServicioPublicado(),
        servicio.getNombre(),
        servicio.getDescripcion(),
        servicio.getPrecioReferencia(),
        clasificacion.idCategoriaServicio(),
        clasificacion.nombreCategoria(),
        clasificacion.idSubcategoriaServicio(),
        clasificacion.nombreSubcategoria(),
        imagenes,
        true,
        prestador);
  }
}
