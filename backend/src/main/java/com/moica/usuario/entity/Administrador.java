package com.moica.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
 * MOICA_ADMIN_CORREO}. Por eso la entidad no expone ninguna operación de cambio.
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

  /**
   * Concede los permisos administrativos a una cuenta existente.
   *
   * @param idUsuario cuenta que los recibe; es también la clave primaria de esta fila
   */
  public Administrador(Long idUsuario) {
    this.idAdministrador = idUsuario;
  }

  @PrePersist
  void registrarInstanteDeAsignacion() {
    this.fechaAsignacion = OffsetDateTime.now();
  }

  public Long getIdAdministrador() {
    return idAdministrador;
  }

  public OffsetDateTime getFechaAsignacion() {
    return fechaAsignacion;
  }
}
