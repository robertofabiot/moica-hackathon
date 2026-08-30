package com.moica.solicitud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Interés de un cliente en un servicio publicado.
 *
 * <p>Corresponde con la tabla {@code solicitud_servicio} que crea la migración {@code V40}. El
 * cliente, el servicio y el municipio se referencian por identificador, no mediante asociaciones
 * JPA: la capacidad {@code solicitud} pregunta a los servicios de las demás en lugar de navegar su
 * modelo de persistencia.
 *
 * <p>No se elimina nunca. El estado vigente vive aquí; cada transición también queda en {@link
 * CambioEstadoSolicitud}. Ambos se escriben en la misma transacción y con el mismo instante.
 */
@Entity
@Table(name = "solicitud_servicio")
public class SolicitudServicio {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_solicitud_servicio")
  private Long idSolicitudServicio;

  @Column(name = "id_cliente", nullable = false, updatable = false)
  private Long idCliente;

  @Column(name = "id_servicio_publicado", nullable = false, updatable = false)
  private Long idServicioPublicado;

  @Column(name = "id_municipio", nullable = false, updatable = false)
  private Integer idMunicipio;

  @Column(name = "descripcion_necesidad", nullable = false, updatable = false)
  private String descripcionNecesidad;

  @Column(name = "indicacion_ubicacion", nullable = false, updatable = false)
  private String indicacionUbicacion;

  @Column(name = "fecha_preferida", updatable = false)
  private LocalDate fechaPreferida;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_actual", nullable = false, length = 30)
  private EstadoSolicitud estadoActual;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private OffsetDateTime fechaCreacion;

  @Column(name = "fecha_actualizacion", nullable = false)
  private OffsetDateTime fechaActualizacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected SolicitudServicio() {}

  /**
   * Registra el envío. Nace {@link EstadoSolicitud#PENDIENTE}. El instante llega desde fuera para
   * que el cambio inicial del historial use exactamente el mismo valor.
   */
  public SolicitudServicio(
      Long idCliente,
      Long idServicioPublicado,
      Integer idMunicipio,
      String descripcionNecesidad,
      String indicacionUbicacion,
      LocalDate fechaPreferida,
      OffsetDateTime instante) {
    this.idCliente = idCliente;
    this.idServicioPublicado = idServicioPublicado;
    this.idMunicipio = idMunicipio;
    this.descripcionNecesidad = descripcionNecesidad;
    this.indicacionUbicacion = indicacionUbicacion;
    this.fechaPreferida = fechaPreferida;
    this.estadoActual = EstadoSolicitud.PENDIENTE;
    this.fechaCreacion = instante;
    this.fechaActualizacion = instante;
  }

  /** Aplica una transición ya decidida, con el mismo instante que el historial. */
  public void cambiarEstado(EstadoSolicitud estadoNuevo, OffsetDateTime instante) {
    this.estadoActual = estadoNuevo;
    this.fechaActualizacion = instante;
  }

  public Long getIdSolicitudServicio() {
    return idSolicitudServicio;
  }

  public Long getIdCliente() {
    return idCliente;
  }

  public Long getIdServicioPublicado() {
    return idServicioPublicado;
  }

  public Integer getIdMunicipio() {
    return idMunicipio;
  }

  public String getDescripcionNecesidad() {
    return descripcionNecesidad;
  }

  public String getIndicacionUbicacion() {
    return indicacionUbicacion;
  }

  public LocalDate getFechaPreferida() {
    return fechaPreferida;
  }

  public EstadoSolicitud getEstadoActual() {
    return estadoActual;
  }

  public OffsetDateTime getFechaCreacion() {
    return fechaCreacion;
  }

  public OffsetDateTime getFechaActualizacion() {
    return fechaActualizacion;
  }
}
