package com.moica.catalogo.dto;

import com.moica.catalogo.entity.SubcategoriaServicio;

/** Subcategoría lista para un selector o para describir un servicio. */
public record DatosDeSubcategoriaServicio(
    Integer idSubcategoriaServicio, String nombre, String descripcion) {

  public static DatosDeSubcategoriaServicio de(SubcategoriaServicio subcategoria) {
    return new DatosDeSubcategoriaServicio(
        subcategoria.getIdSubcategoriaServicio(),
        subcategoria.getNombre(),
        subcategoria.getDescripcion());
  }
}
