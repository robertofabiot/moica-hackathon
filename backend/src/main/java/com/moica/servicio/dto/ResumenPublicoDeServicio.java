package com.moica.servicio.dto;

import com.moica.calificacion.dto.ReputacionPorRol;
import com.moica.catalogo.dto.ClasificacionDeServicio;
import com.moica.prestador.dto.DatosPublicosDePrestador;
import com.moica.servicio.entity.ServicioPublicado;
import java.math.BigDecimal;
import java.util.List;

/**
 * Tarjeta pública de un servicio visible en el descubrimiento.
 *
 * <p>{@code reputacionPrestador} es del prestador que publica, no de este servicio: no existe una
 * reputación por servicio. Todas las tarjetas de un mismo prestador llevan por tanto el mismo
 * agregado, y su promedio es nulo mientras nadie lo haya calificado.
 */
public record ResumenPublicoDeServicio(
    Long idServicioPublicado,
    String nombre,
    String descripcion,
    BigDecimal precioReferencia,
    Short idCategoriaServicio,
    String nombreCategoria,
    Integer idSubcategoriaServicio,
    String nombreSubcategoria,
    DatosDeImagenDeServicio imagenPrincipal,
    DatosPublicosDePrestador prestador,
    ReputacionPorRol reputacionPrestador) {

  public static ResumenPublicoDeServicio de(
      ServicioPublicado servicio,
      ClasificacionDeServicio clasificacion,
      List<DatosDeImagenDeServicio> imagenes,
      DatosPublicosDePrestador prestador,
      ReputacionPorRol reputacionPrestador) {
    return new ResumenPublicoDeServicio(
        servicio.getIdServicioPublicado(),
        servicio.getNombre(),
        servicio.getDescripcion(),
        servicio.getPrecioReferencia(),
        clasificacion.idCategoriaServicio(),
        clasificacion.nombreCategoria(),
        clasificacion.idSubcategoriaServicio(),
        clasificacion.nombreSubcategoria(),
        imagenes.isEmpty() ? null : imagenes.get(0),
        prestador,
        reputacionPrestador);
  }
}
