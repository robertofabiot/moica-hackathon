package com.moica.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Permisos administrativos concedidos a una cuenta.
 *
 * <p>Corresponde con la tabla {@code administrador} que crea la migración {@code V11}. Es una
 * especialización 0..1 de {@link Usuario}: comparte su clave primaria, así que una cuenta no puede
 * tener dos veces el rol y perderlo equivale a borrar la fila.
 *
 * <p>El rol no se solicita ni se concede desde la API: lo asigna el arranque a partir de {@code
 * MOICA_ADMIN_CORREO}, con una inserción directa ({@code AdministradorRepository#concederSiFalta})
 * y la fecha por omisión de la columna. Por eso la entidad es de solo lectura.
 */
@Entity
@Table(name = "administrador")
public class Administrador {

  @Id
  @Column(name = "id_administrador")
  private Long idAdministrador;

  @Column(name = "fecha_asignacion", nullable = false, updatable = false)
  private OffsetDateTime fechaAsignacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected Administrador() {}

  public Long getIdAdministrador() {
    return idAdministrador;
  }

  public OffsetDateTime getFechaAsignacion() {
    return fechaAsignacion;
  }
}
