package com.moica.servicio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Imagen asociada con un servicio publicado.
 *
 * <p>Corresponde con la tabla {@code imagen_servicio_publicado} que crea la migración {@code V31}.
 * La fila guarda la URL pública del objeto y su texto alternativo; el binario vive en el almacén de
 * objetos, nunca en PostgreSQL.
 */
@Entity
@Table(name = "imagen_servicio_publicado")
public class ImagenServicioPublicado {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_imagen_servicio_publicado")
  private Long idImagenServicioPublicado;

  @Column(name = "id_servicio_publicado", nullable = false)
  private Long idServicioPublicado;

  @Column(name = "url_imagen", nullable = false, length = 500)
  private String urlImagen;

  @Column(name = "texto_alternativo", length = 200)
  private String textoAlternativo;

  @Column(name = "orden_visualizacion", nullable = false)
  private short ordenVisualizacion;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private OffsetDateTime fechaCreacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected ImagenServicioPublicado() {}

  public ImagenServicioPublicado(
      Long idServicioPublicado,
      String urlImagen,
      String textoAlternativo,
      short ordenVisualizacion) {
    this.idServicioPublicado = idServicioPublicado;
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

  public Long getIdImagenServicioPublicado() {
    return idImagenServicioPublicado;
  }

  public Long getIdServicioPublicado() {
    return idServicioPublicado;
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
