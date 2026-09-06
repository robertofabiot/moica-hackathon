package com.moica.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Departamento del catálogo territorial.
 *
 * <p>Corresponde con la tabla {@code departamento} que crea la migración {@code V20}. Es un
 * catálogo de solo lectura para la aplicación: sus filas llegan por migraciones versionadas, como
 * la {@code V23} que habilita Managua, y ningún endpoint las crea ni las edita.
 */
@Entity
@Table(name = "departamento")
public class Departamento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_departamento")
  private Short idDepartamento;

  @Column(name = "nombre", nullable = false, length = 80)
  private String nombre;

  /** Si Moica opera en el departamento. En el MVP únicamente Managua está habilitado. */
  @Column(name = "habilitado", nullable = false)
  private boolean habilitado;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected Departamento() {}

  public Short getIdDepartamento() {
    return idDepartamento;
  }

  public String getNombre() {
    return nombre;
  }

  public boolean isHabilitado() {
    return habilitado;
  }
}
