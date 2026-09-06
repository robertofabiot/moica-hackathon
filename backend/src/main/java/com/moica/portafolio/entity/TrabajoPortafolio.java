package com.moica.portafolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Trabajo anterior que el prestador muestra en su portafolio.
 *
 * <p>Corresponde con la tabla {@code trabajo_portafolio} que crea la migración {@code V22}. El
 * portafolio lo administra manualmente el prestador: no existe ningún mecanismo que lo alimente con
 * servicios completados, y cada trabajo cuelga directamente del perfil porque el portafolio no
 * tiene atributos propios (definición 5.5).
 */
@Entity
@Table(name = "trabajo_portafolio")
public class TrabajoPortafolio {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_trabajo")
  private Long idTrabajo;

  @Column(name = "id_prestador", nullable = false)
  private Long idPrestador;

  @Column(name = "titulo", nullable = false, length = 150)
  private String titulo;

  @Column(name = "descripcion", nullable = false)
  private String descripcion;

  /** Solo cuando el prestador desea mostrarla. */
  @Column(name = "fecha_realizacion")
  private LocalDate fechaRealizacion;

  @Column(name = "orden_visualizacion", nullable = false)
  private short ordenVisualizacion;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private OffsetDateTime fechaCreacion;

  @Column(name = "fecha_actualizacion", nullable = false)
  private OffsetDateTime fechaActualizacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected TrabajoPortafolio() {}

  public TrabajoPortafolio(
      Long idPrestador,
      String titulo,
      String descripcion,
      LocalDate fechaRealizacion,
      short ordenVisualizacion) {
    this.idPrestador = idPrestador;
    this.titulo = titulo;
    this.descripcion = descripcion;
    this.fechaRealizacion = fechaRealizacion;
    this.ordenVisualizacion = ordenVisualizacion;
  }

  public void actualizar(String titulo, String descripcion, LocalDate fechaRealizacion) {
    this.titulo = titulo;
    this.descripcion = descripcion;
    this.fechaRealizacion = fechaRealizacion;
  }

  public void cambiarOrdenVisualizacion(short ordenVisualizacion) {
    this.ordenVisualizacion = ordenVisualizacion;
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

  public Long getIdTrabajo() {
    return idTrabajo;
  }

  public Long getIdPrestador() {
    return idPrestador;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public LocalDate getFechaRealizacion() {
    return fechaRealizacion;
  }

  public short getOrdenVisualizacion() {
    return ordenVisualizacion;
  }

  public OffsetDateTime getFechaCreacion() {
    return fechaCreacion;
  }

  public OffsetDateTime getFechaActualizacion() {
    return fechaActualizacion;
  }
}
