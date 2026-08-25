package com.moica.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Municipio del catálogo territorial.
 *
 * <p>Corresponde con la tabla {@code municipio} que crea la migración {@code V20}. Como {@link
 * Departamento}, es un catálogo de solo lectura para la aplicación.
 *
 * <p>La pertenencia al departamento se guarda como identificador plano, igual que hace el resto del
 * código con sus claves foráneas: la restricción vive en PostgreSQL y aquí solo se declara la
 * correspondencia.
 */
@Entity
@Table(name = "municipio")
public class Municipio {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_municipio")
  private Integer idMunicipio;

  @Column(name = "id_departamento", nullable = false)
  private Short idDepartamento;

  @Column(name = "nombre", nullable = false, length = 100)
  private String nombre;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected Municipio() {}

  public Integer getIdMunicipio() {
    return idMunicipio;
  }

  public Short getIdDepartamento() {
    return idDepartamento;
  }

  public String getNombre() {
    return nombre;
  }
}
