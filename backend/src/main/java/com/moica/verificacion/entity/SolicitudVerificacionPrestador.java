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
import java.util.Objects;

/**
 * Intento registrado de un perfil para obtener o renovar un nivel de verificación.
 *
 * <p>Corresponde con la tabla {@code solicitud_verificacion_prestador} que crea la migración {@code
 * V30}. El perfil y el administrador se referencian por identificador y no mediante asociaciones
 * JPA, igual que hace el resto del backend: la capacidad {@code verificacion} pregunta a los
 * servicios de {@code prestador} y {@code usuario} en lugar de navegar su modelo de persistencia.
 *
 * <p>Una solicitud resuelta **no se elimina nunca**: junto con sus documentos es la evidencia de
 * qué se revisó y quién lo decidió. Rechazar no borra nada; reenviar crea una fila nueva.
 *
 * <p>{@code idAdministradorRevisor}, {@code observacionResolucion} y {@code fechaResolucion}
 * describen siempre la **decisión vigente**. Al revocar una aprobación se sustituyen por los de la
 * revocación: el modelo aprobado tiene una sola resolución por fila, y quién aprobó antes queda en
 * el historial de la aplicación, no en columnas que el diccionario no define.
 *
 * <p>Las transiciones no se comprueban aquí: dependen de quién hace la petición y del nivel vigente
 * del perfil, así que las decide {@code RevisionDeVerificacionService}, igual que {@code
 * SegundoFactorService} decide las del segundo factor.
 */
@Entity
@Table(name = "solicitud_verificacion_prestador")
public class SolicitudVerificacionPrestador {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_solicitud_verificacion")
  private Long idSolicitudVerificacion;

  @Column(name = "id_prestador", nullable = false, updatable = false)
  private Long idPrestador;

  @Column(name = "id_administrador_revisor")
  private Long idAdministradorRevisor;

  @Enumerated(EnumType.STRING)
  @Column(name = "nivel_solicitado", nullable = false, updatable = false, length = 30)
  private NivelVerificacionSolicitado nivelSolicitado;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_solicitud", nullable = false, length = 30)
  private EstadoSolicitudVerificacion estadoSolicitud;

  @Column(name = "observacion_resolucion")
  private String observacionResolucion;

  @Column(name = "fecha_solicitud", nullable = false, updatable = false)
  private OffsetDateTime fechaSolicitud;

  @Column(name = "fecha_inicio_revision")
  private OffsetDateTime fechaInicioRevision;

  @Column(name = "fecha_resolucion")
  private OffsetDateTime fechaResolucion;

  @Column(name = "fecha_actualizacion", nullable = false)
  private OffsetDateTime fechaActualizacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected SolicitudVerificacionPrestador() {}

  /**
   * Registra el envío de un expediente.
   *
   * <p>Nace {@link EstadoSolicitudVerificacion#PENDIENTE} y sin revisor. El instante llega desde
   * fuera, del reloj de la aplicación, para que una prueba pueda situarse donde le convenga.
   */
  public SolicitudVerificacionPrestador(
      Long idPrestador, NivelVerificacionSolicitado nivelSolicitado, OffsetDateTime instante) {
    this.idPrestador = idPrestador;
    this.nivelSolicitado = nivelSolicitado;
    this.estadoSolicitud = EstadoSolicitudVerificacion.PENDIENTE;
    this.fechaSolicitud = instante;
    this.fechaActualizacion = instante;
  }

  /** Un administrador toma la solicitud y pasa a analizarla. */
  public void tomar(Long idAdministrador, OffsetDateTime instante) {
    this.idAdministradorRevisor = idAdministrador;
    this.estadoSolicitud = EstadoSolicitudVerificacion.EN_REVISION;
    this.fechaInicioRevision = instante;
    this.fechaActualizacion = instante;
  }

  /** La documentación se acepta y el perfil alcanza el nivel solicitado. */
  public void aprobar(Long idAdministrador, OffsetDateTime instante) {
    resolver(EstadoSolicitudVerificacion.APROBADA, idAdministrador, null, instante);
  }

  /** La documentación no se acepta. El motivo es obligatorio y lo verá el prestador. */
  public void rechazar(Long idAdministrador, String motivo, OffsetDateTime instante) {
    resolver(EstadoSolicitudVerificacion.RECHAZADA, idAdministrador, motivo, instante);
  }

  /** Una verificación ya concedida queda sin efecto. El motivo es obligatorio. */
  public void revocar(Long idAdministrador, String motivo, OffsetDateTime instante) {
    resolver(EstadoSolicitudVerificacion.REVOCADA, idAdministrador, motivo, instante);
  }

  /** Si sigue esperando una decisión; es lo que impide enviar otra solicitud del mismo nivel. */
  public boolean estaAbierta() {
    return estadoSolicitud.esAbierto();
  }

  /** Si el sujeto indicado es el administrador que tiene asignada esta revisión. */
  public boolean laRevisa(Long idAdministrador) {
    return Objects.equals(idAdministradorRevisor, idAdministrador);
  }

  /** Si pertenece al perfil indicado. Lo que impide ver o resolver el expediente de otro. */
  public boolean perteneceA(Long idPrestador) {
    return Objects.equals(this.idPrestador, idPrestador);
  }

  private void resolver(
      EstadoSolicitudVerificacion estado,
      Long idAdministrador,
      String observacion,
      OffsetDateTime instante) {
    this.idAdministradorRevisor = idAdministrador;
    this.estadoSolicitud = estado;
    this.observacionResolucion = observacion;
    this.fechaResolucion = instante;
    this.fechaActualizacion = instante;
  }

  public Long getIdSolicitudVerificacion() {
    return idSolicitudVerificacion;
  }

  public Long getIdPrestador() {
    return idPrestador;
  }

  public Long getIdAdministradorRevisor() {
    return idAdministradorRevisor;
  }

  public NivelVerificacionSolicitado getNivelSolicitado() {
    return nivelSolicitado;
  }

  public EstadoSolicitudVerificacion getEstadoSolicitud() {
    return estadoSolicitud;
  }

  public String getObservacionResolucion() {
    return observacionResolucion;
  }

  public OffsetDateTime getFechaSolicitud() {
    return fechaSolicitud;
  }

  public OffsetDateTime getFechaInicioRevision() {
    return fechaInicioRevision;
  }

  public OffsetDateTime getFechaResolucion() {
    return fechaResolucion;
  }

  public OffsetDateTime getFechaActualizacion() {
    return fechaActualizacion;
  }
}
