package com.moica.portafolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Imagen asociada con un trabajo del portafolio.
 *
 * <p>Corresponde con la tabla {@code imagen_trabajo_portafolio} que crea la migración {@code V22}.
 * La fila guarda la URL pública del objeto y su texto alternativo; el binario vive en el almacén de
 * objetos, nunca en PostgreSQL.
 */
@Entity
@Table(name = "imagen_trabajo_portafolio")
public class ImagenTrabajoPortafolio {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_imagen_trabajo_portafolio")
  private Long idImagenTrabajoPortafolio;

  @Column(name = "id_trabajo", nullable = false)
  private Long idTrabajo;

  @Column(name = "url_imagen", nullable = false, length = 500)
  private String urlImagen;

  /** Descripción breve para accesibilidad; puede faltar. */
  @Column(name = "texto_alternativo", length = 200)
  private String textoAlternativo;

  @Column(name = "orden_visualizacion", nullable = false)
  private short ordenVisualizacion;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private OffsetDateTime fechaCreacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected ImagenTrabajoPortafolio() {}

  public ImagenTrabajoPortafolio(
      Long idTrabajo, String urlImagen, String textoAlternativo, short ordenVisualizacion) {
    this.idTrabajo = idTrabajo;
    this.urlImagen = urlImagen;
    this.textoAlternativo = textoAlternativo;
    this.ordenVisualizacion = ordenVisualizacion;
  }

  public void cambiarTextoAlternativo(String textoAlternativo) {
    this.textoAlternativo = textoAlternativo;
  }

  public void cambiarOrdenVisualizacion(short ordenVisualizacion) {
    this.ordenVisualizacion = ordenVisualizacion;
  }

  @PrePersist
  void registrarInstanteDeCreacion() {
    this.fechaCreacion = OffsetDateTime.now();
  }

  public Long getIdImagenTrabajoPortafolio() {
    return idImagenTrabajoPortafolio;
  }

  public Long getIdTrabajo() {
    return idTrabajo;
  }

  public String getUrlImagen() {
    return urlImagen;
  }

  public String getTextoAlternativo() {
    return textoAlternativo;
  }

  public short getOrdenVisualizacion() {
    return ordenVisualizacion;
  }

  public OffsetDateTime getFechaCreacion() {
    return fechaCreacion;
  }
}
