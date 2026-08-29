package com.moica.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Subcategoría que pertenece a una categoría y clasifica un servicio publicado.
 *
 * <p>Corresponde con la tabla {@code subcategoria_servicio} que crea la migración {@code V31}. El
 * nombre no se repite dentro de la misma categoría.
 */
@Entity
@Table(name = "subcategoria_servicio")
public class SubcategoriaServicio {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_subcategoria_servicio")
  private Integer idSubcategoriaServicio;

  @Column(name = "id_categoria_servicio", nullable = false)
  private Short idCategoriaServicio;

  @Column(name = "nombre", nullable = false, length = 100)
  private String nombre;

  @Column(name = "descripcion")
  private String descripcion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected SubcategoriaServicio() {}

  public Integer getIdSubcategoriaServicio() {
    return idSubcategoriaServicio;
  }

  public Short getIdCategoriaServicio() {
    return idCategoriaServicio;
  }

  public String getNombre() {
    return nombre;
  }

  public String getDescripcion() {
    return descripcion;
  }
}
