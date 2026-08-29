package com.moica.servicio.entity;

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
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Oferta concreta que un prestador publica en Moica.
 *
 * <p>Corresponde con la tabla {@code servicio_publicado} que crea la migración {@code V31}. Un
 * servicio pertenece a un perfil y a una sola subcategoría. El precio es opcional: nulo se presenta
 * como «A convenir». No se borra: se desactiva.
 */
@Entity
@Table(name = "servicio_publicado")
public class ServicioPublicado {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_servicio_publicado")
  private Long idServicioPublicado;

  @Column(name = "id_prestador", nullable = false)
  private Long idPrestador;

  @Column(name = "id_subcategoria_servicio", nullable = false)
  private Integer idSubcategoriaServicio;

  @Column(name = "nombre", nullable = false, length = 150)
  private String nombre;

  @Column(name = "descripcion", nullable = false)
  private String descripcion;

  @Column(name = "precio_referencia", precision = 12, scale = 2)
  private BigDecimal precioReferencia;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 30)
  private EstadoServicio estado;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private OffsetDateTime fechaCreacion;

  @Column(name = "fecha_actualizacion", nullable = false)
  private OffsetDateTime fechaActualizacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected ServicioPublicado() {}

  /**
   * Crea un servicio preparado: siempre nace {@link EstadoServicio#INACTIVO}.
   *
   * <p>La columna del diccionario admite {@code ACTIVO} por omisión; la aplicación no lo usa al
   * insertar. Activar es una decisión posterior, coordinada con el perfil.
   */
  public ServicioPublicado(
      Long idPrestador,
      Integer idSubcategoriaServicio,
      String nombre,
      String descripcion,
      BigDecimal precioReferencia) {
    this.idPrestador = idPrestador;
    this.idSubcategoriaServicio = idSubcategoriaServicio;
    this.nombre = nombre;
    this.descripcion = descripcion;
    this.precioReferencia = precioReferencia;
    this.estado = EstadoServicio.INACTIVO;
  }

  public void actualizar(
      Integer idSubcategoriaServicio,
      String nombre,
      String descripcion,
      BigDecimal precioReferencia) {
    this.idSubcategoriaServicio = idSubcategoriaServicio;
    this.nombre = nombre;
    this.descripcion = descripcion;
    this.precioReferencia = precioReferencia;
  }

  public void cambiarEstado(EstadoServicio estado) {
    this.estado = estado;
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

  public Long getIdServicioPublicado() {
    return idServicioPublicado;
  }

  public Long getIdPrestador() {
    return idPrestador;
  }

  public Integer getIdSubcategoriaServicio() {
    return idSubcategoriaServicio;
  }

  public String getNombre() {
    return nombre;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public BigDecimal getPrecioReferencia() {
    return precioReferencia;
  }

  public EstadoServicio getEstado() {
    return estado;
  }

  public OffsetDateTime getFechaCreacion() {
    return fechaCreacion;
  }

  public OffsetDateTime getFechaActualizacion() {
    return fechaActualizacion;
  }
}
