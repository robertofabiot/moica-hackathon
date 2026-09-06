package com.moica.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Un mensaje de texto del hilo de una solicitud de servicio.
 *
 * <p>Corresponde con la tabla {@code mensaje_solicitud} que crea la migración {@code V41}. No
 * existe una entidad contenedora {@code Conversacion}: el MVP admite un único hilo entre los dos
 * participantes y su estado se deriva de la solicitud, así que el mensaje cuelga directamente de
 * {@code SolicitudServicio}.
 *
 * <p>El remitente se referencia por identificador, igual que en el resto de la capacidad: quien
 * necesita su nombre se lo pide a la capacidad {@code usuario}.
 *
 * <p>No se edita ni se borra. Todos sus campos son {@code updatable = false} a propósito.
 */
@Entity
@Table(name = "mensaje_solicitud")
public class MensajeSolicitud {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_mensaje_solicitud")
  private Long idMensajeSolicitud;

  @Column(name = "id_solicitud_servicio", nullable = false, updatable = false)
  private Long idSolicitudServicio;

  @Column(name = "id_remitente", nullable = false, updatable = false)
  private Long idRemitente;

  @Column(name = "contenido", nullable = false, updatable = false)
  private String contenido;

  @Column(name = "fecha_envio", nullable = false, updatable = false)
  private OffsetDateTime fechaEnvio;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected MensajeSolicitud() {}

  /**
   * Registra un mensaje ya autorizado.
   *
   * <p>El instante llega desde fuera para que el servicio pueda usar exactamente el mismo valor que
   * acaba de comprobar contra el estado de la solicitud.
   */
  public MensajeSolicitud(
      Long idSolicitudServicio, Long idRemitente, String contenido, OffsetDateTime instante) {
    this.idSolicitudServicio = idSolicitudServicio;
    this.idRemitente = idRemitente;
    this.contenido = contenido;
    this.fechaEnvio = instante;
  }

  public Long getIdMensajeSolicitud() {
    return idMensajeSolicitud;
  }

  public Long getIdSolicitudServicio() {
    return idSolicitudServicio;
  }

  public Long getIdRemitente() {
    return idRemitente;
  }

  public String getContenido() {
    return contenido;
  }

  public OffsetDateTime getFechaEnvio() {
    return fechaEnvio;
  }
}
