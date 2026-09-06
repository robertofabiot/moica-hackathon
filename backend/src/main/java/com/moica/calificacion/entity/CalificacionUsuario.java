package com.moica.calificacion.entity;

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
 * Valoración que un participante emite sobre el otro tras completar una solicitud.
 *
 * <p>Corresponde con la tabla {@code calificacion_usuario} que crea la migración {@code V42}. No
 * existe una entidad {@code Reputacion}: la definición 10 establece que el promedio y la cantidad
 * se calculan a partir de estas filas, separados por rol.
 *
 * <p>La solicitud y las dos personas se referencian por identificador, igual que en el resto del
 * proyecto: quien necesite sus nombres se los pide a la capacidad que los tiene.
 *
 * <p>No se edita ni se borra. Todos sus campos son {@code updatable = false} a propósito: una
 * calificación es evidencia de una relación que ocurrió, y corregirla a posteriori cambiaría una
 * reputación ya publicada.
 */
@Entity
@Table(name = "calificacion_usuario")
public class CalificacionUsuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_calificacion_usuario")
  private Long idCalificacionUsuario;

  @Column(name = "id_solicitud_servicio", nullable = false, updatable = false)
  private Long idSolicitudServicio;

  @Column(name = "id_calificador", nullable = false, updatable = false)
  private Long idCalificador;

  @Column(name = "id_calificado", nullable = false, updatable = false)
  private Long idCalificado;

  @Enumerated(EnumType.STRING)
  @Column(name = "rol_calificado", nullable = false, updatable = false, length = 30)
  private RolCalificado rolCalificado;

  @Column(name = "puntuacion", nullable = false, updatable = false)
  private short puntuacion;

  @Column(name = "comentario", updatable = false)
  private String comentario;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private OffsetDateTime fechaCreacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected CalificacionUsuario() {}

  /**
   * Registra una calificación ya autorizada.
   *
   * <p>Ni el calificado ni el rol llegan desde el navegador: el servicio los deriva de la solicitud
   * antes de construir esta entidad. El comentario llega ya normalizado, {@code null} cuando venía
   * vacío o solo con espacios.
   */
  public CalificacionUsuario(
      Long idSolicitudServicio,
      Long idCalificador,
      Long idCalificado,
      RolCalificado rolCalificado,
      short puntuacion,
      String comentario,
      OffsetDateTime instante) {
    this.idSolicitudServicio = idSolicitudServicio;
    this.idCalificador = idCalificador;
    this.idCalificado = idCalificado;
    this.rolCalificado = rolCalificado;
    this.puntuacion = puntuacion;
    this.comentario = comentario;
    this.fechaCreacion = instante;
  }

  public Long getIdCalificacionUsuario() {
    return idCalificacionUsuario;
  }

  public Long getIdSolicitudServicio() {
    return idSolicitudServicio;
  }

  public Long getIdCalificador() {
    return idCalificador;
  }

  public Long getIdCalificado() {
    return idCalificado;
  }

  public RolCalificado getRolCalificado() {
    return rolCalificado;
  }

  public short getPuntuacion() {
    return puntuacion;
  }

  public String getComentario() {
    return comentario;
  }

  public OffsetDateTime getFechaCreacion() {
    return fechaCreacion;
  }
}
