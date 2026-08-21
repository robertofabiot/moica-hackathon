package com.moica.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Cuenta registrada en Moica.
 *
 * <p>Corresponde con la tabla {@code usuario} que crea la migración {@code V10}. La unicidad del
 * correo, los valores admitidos de {@link EstadoCuenta} y los valores por omisión los impone esa
 * migración; aquí solo se declara la correspondencia.
 *
 * <p>El correo llega ya normalizado desde el servicio, de modo que dos cuentas no puedan
 * diferenciarse únicamente por mayúsculas o espacios exteriores.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_usuario")
  private Long idUsuario;

  @Column(name = "nombre_completo", nullable = false, length = 120)
  private String nombreCompleto;

  @Column(name = "correo_electronico", nullable = false, length = 254)
  private String correoElectronico;

  /** Resultado del algoritmo de hash. Nunca la contraseña original ni un valor reversible. */
  @Column(name = "clave_hash", nullable = false, length = 255)
  private String claveHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_cuenta", nullable = false, length = 30)
  private EstadoCuenta estadoCuenta;

  @Column(name = "fecha_fin_estado_cuenta")
  private OffsetDateTime fechaFinEstadoCuenta;

  @Column(name = "fecha_registro", nullable = false, updatable = false)
  private OffsetDateTime fechaRegistro;

  @Column(name = "fecha_actualizacion", nullable = false)
  private OffsetDateTime fechaActualizacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected Usuario() {}

  /**
   * Crea una cuenta nueva, que siempre nace {@link EstadoCuenta#ACTIVA}.
   *
   * @param nombreCompleto nombre de la persona propietaria de la cuenta
   * @param correoElectronico correo ya normalizado
   * @param claveHash hash de la contraseña, nunca la contraseña
   */
  public Usuario(String nombreCompleto, String correoElectronico, String claveHash) {
    this.nombreCompleto = nombreCompleto;
    this.correoElectronico = correoElectronico;
    this.claveHash = claveHash;
    this.estadoCuenta = EstadoCuenta.ACTIVA;
  }

  @PrePersist
  void registrarInstanteDeCreacion() {
    OffsetDateTime ahora = OffsetDateTime.now();
    this.fechaRegistro = ahora;
    this.fechaActualizacion = ahora;
  }

  @PreUpdate
  void registrarInstanteDeActualizacion() {
    this.fechaActualizacion = OffsetDateTime.now();
  }

  public Long getIdUsuario() {
    return idUsuario;
  }

  public String getNombreCompleto() {
    return nombreCompleto;
  }

  public String getCorreoElectronico() {
    return correoElectronico;
  }

  public String getClaveHash() {
    return claveHash;
  }

  public EstadoCuenta getEstadoCuenta() {
    return estadoCuenta;
  }

  public OffsetDateTime getFechaFinEstadoCuenta() {
    return fechaFinEstadoCuenta;
  }

  public OffsetDateTime getFechaRegistro() {
    return fechaRegistro;
  }

  public OffsetDateTime getFechaActualizacion() {
    return fechaActualizacion;
  }
}
