package com.moica.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Sesión abierta por una cuenta al iniciar sesión.
 *
 * <p>Corresponde con la tabla {@code sesion} que crea la migración {@code V10}. Es la unidad
 * revocable de la autenticación: el JWT entregado al navegador solo transporta {@link
 * #getIdentificadorToken()} en su claim {@code jti}, de modo que sin esta fila vigente el token no
 * concede acceso.
 *
 * <p>La cuenta se referencia por identificador y no mediante una asociación JPA: la capacidad
 * {@code auth} pregunta por el usuario al servicio de la capacidad {@code usuario} en lugar de
 * navegar su modelo de persistencia. La integridad la garantiza la clave foránea {@code
 * fk_sesion_usuario}.
 */
@Entity
@Table(name = "sesion")
public class Sesion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_sesion")
  private Long idSesion;

  @Column(name = "id_usuario", nullable = false)
  private Long idUsuario;

  @Column(name = "identificador_token", nullable = false, length = 64)
  private String identificadorToken;

  @Column(name = "segundo_factor_verificado", nullable = false)
  private boolean segundoFactorVerificado;

  @Column(name = "fecha_inicio", nullable = false)
  private OffsetDateTime fechaInicio;

  @Column(name = "fecha_expiracion", nullable = false)
  private OffsetDateTime fechaExpiracion;

  @Column(name = "fecha_revocacion")
  private OffsetDateTime fechaRevocacion;

  @Enumerated(EnumType.STRING)
  @Column(name = "motivo_revocacion", length = 30)
  private MotivoRevocacionSesion motivoRevocacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected Sesion() {}

  /**
   * Abre una sesión para una cuenta.
   *
   * @param idUsuario cuenta que inicia la sesión
   * @param identificadorToken identificador aleatorio y único que viajará en el {@code jti} del JWT
   * @param fechaInicio instante en que se abre la sesión
   * @param fechaExpiracion instante a partir del cual deja de ser válida; posterior al de inicio
   */
  public Sesion(
      Long idUsuario,
      String identificadorToken,
      OffsetDateTime fechaInicio,
      OffsetDateTime fechaExpiracion) {
    this.idUsuario = idUsuario;
    this.identificadorToken = identificadorToken;
    this.fechaInicio = fechaInicio;
    this.fechaExpiracion = fechaExpiracion;
    // El segundo factor se verifica en P3: toda sesión nace sin verificarlo.
    this.segundoFactorVerificado = false;
  }

  /**
   * Indica si la sesión sigue concediendo acceso en un instante dado.
   *
   * <p>Una sesión revocada deja de valer aunque su fecha de expiración siga en el futuro.
   */
  public boolean estaVigente(OffsetDateTime instante) {
    return fechaRevocacion == null && instante.isBefore(fechaExpiracion);
  }

  /** Invalida la sesión antes de que expire, conservando el instante y el motivo. */
  public void revocar(OffsetDateTime instante, MotivoRevocacionSesion motivo) {
    this.fechaRevocacion = instante;
    this.motivoRevocacion = motivo;
  }

  public Long getIdSesion() {
    return idSesion;
  }

  public Long getIdUsuario() {
    return idUsuario;
  }

  public String getIdentificadorToken() {
    return identificadorToken;
  }

  public boolean isSegundoFactorVerificado() {
    return segundoFactorVerificado;
  }

  public OffsetDateTime getFechaInicio() {
    return fechaInicio;
  }

  public OffsetDateTime getFechaExpiracion() {
    return fechaExpiracion;
  }

  public OffsetDateTime getFechaRevocacion() {
    return fechaRevocacion;
  }

  public MotivoRevocacionSesion getMotivoRevocacion() {
    return motivoRevocacion;
  }
}
