package com.moica.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Categoría general del catálogo de servicios.
 *
 * <p>Corresponde con la tabla {@code categoria_servicio} que crea la migración {@code V31}. Es un
 * catálogo de solo lectura para la aplicación: sus filas llegan por migraciones versionadas, como
 * la {@code V90} de demostración, y ningún endpoint las crea ni las edita.
 */
@Entity
@Table(name = "categoria_servicio")
public class CategoriaServicio {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_categoria_servicio")
  private Short idCategoriaServicio;

  @Column(name = "nombre", nullable = false, length = 100)
  private String nombre;

  @Column(name = "descripcion")
  private String descripcion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected CategoriaServicio() {}

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
