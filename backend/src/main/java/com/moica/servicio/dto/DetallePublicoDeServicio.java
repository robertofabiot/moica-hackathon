package com.moica.servicio.dto;

import com.moica.calificacion.dto.ReputacionPorRol;
import com.moica.catalogo.dto.ClasificacionDeServicio;
import com.moica.prestador.dto.DatosPublicosDePrestador;
import com.moica.servicio.entity.ServicioPublicado;
import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle público de un servicio visible.
 *
 * <p>{@code admiteContratacion} avisa si hoy se podría solicitar. No revela contactos. {@code
 * precioReferencia} nulo se presenta como «A convenir» en la interfaz, no en este campo.
 *
 * <p>{@code reputacionPrestador} es el agregado de quien publica el servicio, con su promedio, su
 * cantidad y su desglose por estrellas. No lleva comentarios ni identidades de quienes calificaron:
 * en público solo sale el agregado.
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
    DatosPublicosDePrestador prestador,
    ReputacionPorRol reputacionPrestador) {

  public DetallePublicoDeServicio {
    imagenes = List.copyOf(imagenes);
  }

  public static DetallePublicoDeServicio de(
      ServicioPublicado servicio,
      ClasificacionDeServicio clasificacion,
      List<DatosDeImagenDeServicio> imagenes,
      DatosPublicosDePrestador prestador,
      ReputacionPorRol reputacionPrestador) {
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
        prestador,
        reputacionPrestador);
  }
}
