package com.moica.solicitud.entity;

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
 * Una transición del historial de una solicitud de servicio.
 *
 * <p>Corresponde con la tabla {@code cambio_estado_solicitud} que crea la migración {@code V40}. El
 * registro inicial lleva {@code estadoAnterior} nulo y {@code estadoNuevo} {@code PENDIENTE}. No se
 * edita ni se borra.
 */
@Entity
@Table(name = "cambio_estado_solicitud")
public class CambioEstadoSolicitud {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_cambio_estado_solicitud")
  private Long idCambioEstadoSolicitud;

  @Column(name = "id_solicitud_servicio", nullable = false, updatable = false)
  private Long idSolicitudServicio;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_anterior", updatable = false, length = 30)
  private EstadoSolicitud estadoAnterior;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_nuevo", nullable = false, updatable = false, length = 30)
  private EstadoSolicitud estadoNuevo;

  @Column(name = "id_actor", nullable = false, updatable = false)
  private Long idActor;

  @Column(name = "motivo", updatable = false)
  private String motivo;

  @Column(name = "fecha_cambio", nullable = false, updatable = false)
  private OffsetDateTime fechaCambio;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected CambioEstadoSolicitud() {}

  public CambioEstadoSolicitud(
      Long idSolicitudServicio,
      EstadoSolicitud estadoAnterior,
      EstadoSolicitud estadoNuevo,
      Long idActor,
      String motivo,
      OffsetDateTime fechaCambio) {
    this.idSolicitudServicio = idSolicitudServicio;
    this.estadoAnterior = estadoAnterior;
    this.estadoNuevo = estadoNuevo;
    this.idActor = idActor;
    this.motivo = motivo;
    this.fechaCambio = fechaCambio;
  }

  /** El cambio con el que nace una solicitud: de nada a {@code PENDIENTE}. */
  public static CambioEstadoSolicitud inicial(
      Long idSolicitudServicio, Long idActor, OffsetDateTime instante) {
    return new CambioEstadoSolicitud(
        idSolicitudServicio, null, EstadoSolicitud.PENDIENTE, idActor, null, instante);
  }

  public Long getIdCambioEstadoSolicitud() {
    return idCambioEstadoSolicitud;
  }

  public Long getIdSolicitudServicio() {
    return idSolicitudServicio;
  }

  public EstadoSolicitud getEstadoAnterior() {
    return estadoAnterior;
  }

  public EstadoSolicitud getEstadoNuevo() {
    return estadoNuevo;
  }

  public Long getIdActor() {
    return idActor;
  }

  public String getMotivo() {
    return motivo;
  }

  public OffsetDateTime getFechaCambio() {
    return fechaCambio;
  }
}
