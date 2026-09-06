package com.moica.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Segundo factor TOTP registrado por una cuenta.
 *
 * <p>Corresponde con la tabla {@code segundo_factor_usuario} que crea la migración {@code V11}. Es
 * una especialización 0..1 de la cuenta: comparte su clave primaria, de modo que la propia clave
 * garantiza el «como máximo un segundo factor por cuenta» del diccionario.
 *
 * <p>La cuenta se referencia por identificador y no mediante una asociación JPA, igual que en
 * {@link Sesion}: la capacidad {@code auth} pregunta por el usuario al servicio de la capacidad
 * {@code usuario} en lugar de navegar su modelo de persistencia.
 *
 * <p>{@link #getSecretoCifrado()} devuelve exactamente lo que hay en la columna: el secreto ya
 * cifrado. La entidad no descifra nada y no existe ningún camino que exponga el valor en claro
 * desde aquí.
 */
@Entity
@Table(name = "segundo_factor_usuario")
public class SegundoFactorUsuario {

  @Id
  @Column(name = "id_usuario")
  private Long idUsuario;

  @Column(name = "secreto_totp", nullable = false, length = 255)
  private String secretoCifrado;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_segundo_factor", nullable = false, length = 30)
  private EstadoSegundoFactor estadoSegundoFactor;

  @Column(name = "fecha_activacion")
  private OffsetDateTime fechaActivacion;

  @Column(name = "fecha_ultima_verificacion")
  private OffsetDateTime fechaUltimaVerificacion;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private OffsetDateTime fechaCreacion;

  @Column(name = "fecha_actualizacion", nullable = false)
  private OffsetDateTime fechaActualizacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected SegundoFactorUsuario() {}

  /**
   * Registra un segundo factor todavía sin confirmar.
   *
   * @param idUsuario cuenta propietaria; es también la clave primaria de esta fila
   * @param secretoCifrado secreto TOTP ya cifrado, nunca el valor en claro
   */
  public SegundoFactorUsuario(Long idUsuario, String secretoCifrado) {
    this.idUsuario = idUsuario;
    this.secretoCifrado = secretoCifrado;
    this.estadoSegundoFactor = EstadoSegundoFactor.PENDIENTE_ACTIVACION;
  }

  @PrePersist
  void registrarInstanteDeCreacion() {
    OffsetDateTime ahora = OffsetDateTime.now();
    this.fechaCreacion = ahora;
    this.fechaActualizacion = ahora;
  }

  @PreUpdate
  void registrarInstanteDeActualizacion() {
    this.fechaActualizacion = OffsetDateTime.now();
  }

  /**
   * Vuelve a empezar la activación con un secreto nuevo.
   *
   * <p>Es lo que ocurre cuando alguien abandona una activación a medias y la reintenta, y también
   * cuando reactiva el segundo factor después de haberlo desactivado: en los dos casos el secreto
   * anterior deja de valer, tal como exige el plan.
   */
  public void reiniciarActivacion(String secretoCifrado) {
    this.secretoCifrado = secretoCifrado;
    this.estadoSegundoFactor = EstadoSegundoFactor.PENDIENTE_ACTIVACION;
    this.fechaActivacion = null;
    this.fechaUltimaVerificacion = null;
  }

  /** Da por confirmado el segundo factor tras el primer código válido. */
  public void activar(OffsetDateTime instante) {
    this.estadoSegundoFactor = EstadoSegundoFactor.ACTIVO;
    this.fechaActivacion = instante;
    this.fechaUltimaVerificacion = instante;
  }

  /**
   * Suspende el segundo factor.
   *
   * <p>La fecha de activación se conserva: documenta que llegó a estar activo y la restricción de
   * la base de datos solo la exige mientras el estado sea {@link EstadoSegundoFactor#ACTIVO}.
   */
  public void desactivar() {
    this.estadoSegundoFactor = EstadoSegundoFactor.DESACTIVADO;
  }

  /** Deja constancia de la última verificación correcta de un código. */
  public void registrarVerificacion(OffsetDateTime instante) {
    this.fechaUltimaVerificacion = instante;
  }

  public boolean estaActivo() {
    return estadoSegundoFactor == EstadoSegundoFactor.ACTIVO;
  }

  public boolean estaPendienteDeActivacion() {
    return estadoSegundoFactor == EstadoSegundoFactor.PENDIENTE_ACTIVACION;
  }

  public Long getIdUsuario() {
    return idUsuario;
  }

  public String getSecretoCifrado() {
    return secretoCifrado;
  }

  public EstadoSegundoFactor getEstadoSegundoFactor() {
    return estadoSegundoFactor;
  }

  public OffsetDateTime getFechaActivacion() {
    return fechaActivacion;
  }

  public OffsetDateTime getFechaUltimaVerificacion() {
    return fechaUltimaVerificacion;
  }

  public OffsetDateTime getFechaCreacion() {
    return fechaCreacion;
  }

  public OffsetDateTime getFechaActualizacion() {
    return fechaActualizacion;
  }
}
