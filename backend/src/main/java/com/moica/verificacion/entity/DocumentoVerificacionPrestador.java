package com.moica.verificacion.entity;

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
 * Archivo privado que forma parte del expediente de una solicitud de verificación.
 *
 * <p>Corresponde con la tabla {@code documento_verificacion_prestador} que crea la migración {@code
 * V30}. La fila guarda **la clave opaca y los metadatos**: nunca el binario, nunca una URL pública
 * y nunca una URL prefirmada. El archivo vive en el bucket privado y solo se alcanza mediante un
 * acceso temporal que se firma en cada petición autorizada.
 *
 * <p>Un documento no se edita ni se sustituye después de enviarse: el expediente que revisó una
 * persona debe ser exactamente el que envió el prestador. Corregir algo significa presentar una
 * solicitud nueva, con su propio expediente.
 *
 * <p>{@link #getClaveAlmacenamiento()} es de uso interno del servidor. Ningún DTO de salida la
 * publica, tampoco al propietario del perfil.
 */
@Entity
@Table(name = "documento_verificacion_prestador")
public class DocumentoVerificacionPrestador {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_documento_verificacion")
  private Long idDocumentoVerificacion;

  @Column(name = "id_solicitud_verificacion", nullable = false, updatable = false)
  private Long idSolicitudVerificacion;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_documento", nullable = false, updatable = false, length = 30)
  private TipoDocumentoVerificacion tipoDocumento;

  @Column(name = "clave_almacenamiento", nullable = false, updatable = false, length = 300)
  private String claveAlmacenamiento;

  @Column(name = "nombre_original", nullable = false, updatable = false, length = 255)
  private String nombreOriginal;

  @Column(name = "tipo_mime", nullable = false, updatable = false, length = 100)
  private String tipoMime;

  @Column(name = "tamano_bytes", nullable = false, updatable = false)
  private Integer tamanoBytes;

  @Column(name = "fecha_carga", nullable = false, updatable = false)
  private OffsetDateTime fechaCarga;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected DocumentoVerificacionPrestador() {}

  /**
   * Registra un archivo ya validado y ya subido al almacenamiento privado.
   *
   * @param claveAlmacenamiento clave opaca con la que el bucket privado lo localiza
   * @param nombreOriginal nombre que eligió el prestador, ya saneado
   * @param tipoMime tipo real del contenido, comprobado contra su firma binaria
   */
  public DocumentoVerificacionPrestador(
      Long idSolicitudVerificacion,
      TipoDocumentoVerificacion tipoDocumento,
      String claveAlmacenamiento,
      String nombreOriginal,
      String tipoMime,
      int tamanoBytes,
      OffsetDateTime instante) {
    this.idSolicitudVerificacion = idSolicitudVerificacion;
    this.tipoDocumento = tipoDocumento;
    this.claveAlmacenamiento = claveAlmacenamiento;
    this.nombreOriginal = nombreOriginal;
    this.tipoMime = tipoMime;
    this.tamanoBytes = tamanoBytes;
    this.fechaCarga = instante;
  }

  public Long getIdDocumentoVerificacion() {
    return idDocumentoVerificacion;
  }

  public Long getIdSolicitudVerificacion() {
    return idSolicitudVerificacion;
  }

  public TipoDocumentoVerificacion getTipoDocumento() {
    return tipoDocumento;
  }

  public String getClaveAlmacenamiento() {
    return claveAlmacenamiento;
  }

  public String getNombreOriginal() {
    return nombreOriginal;
  }

  public String getTipoMime() {
    return tipoMime;
  }

  public Integer getTamanoBytes() {
    return tamanoBytes;
  }

  public OffsetDateTime getFechaCarga() {
    return fechaCarga;
  }

  /**
   * Se redefine a propósito: la representación por omisión de una entidad JPA no incluye campos,
   * pero una traza o un mensaje de error que imprima este objeto no debe poder revelar la clave del
   * archivo. El diccionario la clasifica como dato privado y los estándares prohíben registrarla.
   */
  @Override
  public String toString() {
    return "DocumentoVerificacionPrestador[idDocumentoVerificacion="
        + idDocumentoVerificacion
        + ", tipoDocumento="
        + tipoDocumento
        + ", claveAlmacenamiento=(oculta)]";
  }
}
