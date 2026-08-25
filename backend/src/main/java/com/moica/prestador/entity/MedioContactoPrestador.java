package com.moica.prestador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Dato de contacto externo publicado por el prestador.
 *
 * <p>Corresponde con la tabla {@code medio_contacto_prestador} que crea la migración {@code V21}.
 * El contenido es una entrada libre —número, correo, usuario, enlace o indicación escrita— que
 * Moica no clasifica por plataforma (definición 5.4).
 *
 * <p>En P4 solo lo consulta y administra su propietario: para terceros permanece oculto hasta que
 * exista una solicitud aceptada, regla que aplicará el incremento de solicitudes.
 */
@Entity
@Table(name = "medio_contacto_prestador")
public class MedioContactoPrestador {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_medio_contacto_prestador")
  private Long idMedioContactoPrestador;

  @Column(name = "id_prestador", nullable = false)
  private Long idPrestador;

  @Column(name = "contenido", nullable = false, length = 500)
  private String contenido;

  @Column(name = "orden_visualizacion", nullable = false)
  private short ordenVisualizacion;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private OffsetDateTime fechaCreacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected MedioContactoPrestador() {}

  public MedioContactoPrestador(Long idPrestador, String contenido, short ordenVisualizacion) {
    this.idPrestador = idPrestador;
    this.contenido = contenido;
    this.ordenVisualizacion = ordenVisualizacion;
  }

  public void cambiarContenido(String contenido) {
    this.contenido = contenido;
  }

  public void cambiarOrdenVisualizacion(short ordenVisualizacion) {
    this.ordenVisualizacion = ordenVisualizacion;
  }

  @PrePersist
  void registrarInstanteDeCreacion() {
    this.fechaCreacion = OffsetDateTime.now();
  }

  public Long getIdMedioContactoPrestador() {
    return idMedioContactoPrestador;
  }

  public Long getIdPrestador() {
    return idPrestador;
  }

  public String getContenido() {
    return contenido;
  }

  public short getOrdenVisualizacion() {
    return ordenVisualizacion;
  }

  public OffsetDateTime getFechaCreacion() {
    return fechaCreacion;
  }
}
