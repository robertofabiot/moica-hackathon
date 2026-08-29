package com.moica.catalogo.dto;

import com.moica.catalogo.entity.CategoriaServicio;
import java.util.List;

/**
 * Categoría de servicios con sus subcategorías, tal como la consume un filtro o un formulario.
 *
 * <p>La taxonomía de demostración no se presenta como exhaustiva: es un catálogo ampliable.
 */
public record DatosDeCategoriaServicio(
    Short idCategoriaServicio,
    String nombre,
    String descripcion,
    List<DatosDeSubcategoriaServicio> subcategorias) {

  public DatosDeCategoriaServicio {
    subcategorias = List.copyOf(subcategorias);
  }

  public static DatosDeCategoriaServicio de(
      CategoriaServicio categoria, List<DatosDeSubcategoriaServicio> subcategorias) {
    return new DatosDeCategoriaServicio(
        categoria.getIdCategoriaServicio(),
        categoria.getNombre(),
        categoria.getDescripcion(),
        subcategorias);
  }
}
