package com.moica.catalogo.dto;

import com.moica.catalogo.entity.CategoriaServicio;
import com.moica.catalogo.entity.SubcategoriaServicio;

/**
 * Categoría y subcategoría de un servicio ya clasificado.
 *
 * <p>Lo usa la capacidad {@code servicio} para validar la subcategoría elegida y para mostrarla sin
 * repetir la consulta al catálogo.
 */
public record ClasificacionDeServicio(
    Short idCategoriaServicio,
    String nombreCategoria,
    Integer idSubcategoriaServicio,
    String nombreSubcategoria) {

  public static ClasificacionDeServicio de(
      CategoriaServicio categoria, SubcategoriaServicio subcategoria) {
    return new ClasificacionDeServicio(
        categoria.getIdCategoriaServicio(),
        categoria.getNombre(),
        subcategoria.getIdSubcategoriaServicio(),
        subcategoria.getNombre());
  }
}
