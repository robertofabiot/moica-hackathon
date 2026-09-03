package com.moica.moderacion.entity;

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
 * Expediente que abre el reporte de un participante sobre el otro.
 *
 * <p>Corresponde con la tabla {@code caso_moderacion} que crea la migración {@code V50}. Concentra
 * lo vigente del caso; cada cambio queda además fotografiado en una versión de {@link
 * HistorialCaso}, creada en la misma transacción.
 *
 * <p>La solicitud y las dos personas se referencian por identificador, igual que en el resto del
 * proyecto: quien necesite sus nombres se los pide a la capacidad que los tiene.
 *
 * <p>Lo que el reporte declara —la solicitud, quién reporta, a quién, el motivo y la descripción—
 * es inmutable: son {@code updatable = false}. En el MVP un reporte no se edita ni se borra. Los
 * campos administrativos —responsable, medida, estado, resultado, resolución y sus fechas— nacen
 * como los deja la apertura y solo los mueve la revisión administrativa de P10A y P10B; esta clase
 * no ofrece ninguna forma de cambiarlos porque P9 no cambia ninguno.
 */
@Entity
@Table(name = "caso_moderacion")
public class CasoModeracion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_caso_moderacion")
  private Long idCasoModeracion;

  @Column(name = "id_solicitud_servicio", nullable = false, updatable = false)
  private Long idSolicitudServicio;

  @Column(name = "id_reportante", nullable = false, updatable = false)
  private Long idReportante;

  @Column(name = "id_reportado", nullable = false, updatable = false)
  private Long idReportado;

  @Column(name = "id_administrador_responsable")
  private Long idAdministradorResponsable;

  @Column(name = "id_medida_administrativa_actual")
  private Short idMedidaAdministrativaActual;

  @Column(name = "motivo", nullable = false, updatable = false, length = 120)
  private String motivo;

  @Column(name = "descripcion", nullable = false, updatable = false)
  private String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_actual", nullable = false, length = 30)
  private EstadoCasoModeracion estadoActual;

  @Enumerated(EnumType.STRING)
  @Column(name = "resultado_actual", length = 30)
  private ResultadoCasoModeracion resultadoActual;

  @Column(name = "resolucion_actual")
  private String resolucionActual;

  @Column(name = "fecha_fin_medida_actual")
  private OffsetDateTime fechaFinMedidaActual;

  @Column(name = "fecha_apertura", nullable = false, updatable = false)
  private OffsetDateTime fechaApertura;

  @Column(name = "fecha_cierre_actual")
  private OffsetDateTime fechaCierreActual;

  @Column(name = "fecha_actualizacion", nullable = false)
  private OffsetDateTime fechaActualizacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected CasoModeracion() {}

  /**
   * Abre un caso ya autorizado.
   *
   * <p>Ni el reportado ni el reportante llegan desde el navegador: el servicio los deriva de la
   * sesión y de la solicitud antes de construir esta entidad.
   *
   * <p>Nace {@link EstadoCasoModeracion#ABIERTO} y sin nada administrativo: reportar no asigna
   * responsable, no elige medida, no resuelve y no sanciona. El mismo instante sirve de apertura y
   * de última actualización, y es el que recibe también la primera versión histórica.
   */
  public CasoModeracion(
      Long idSolicitudServicio,
      Long idReportante,
      Long idReportado,
      String motivo,
      String descripcion,
      OffsetDateTime instante) {
    this.idSolicitudServicio = idSolicitudServicio;
    this.idReportante = idReportante;
    this.idReportado = idReportado;
    this.motivo = motivo;
    this.descripcion = descripcion;
    this.estadoActual = EstadoCasoModeracion.ABIERTO;
    this.fechaApertura = instante;
    this.fechaActualizacion = instante;
  }

  public Long getIdCasoModeracion() {
    return idCasoModeracion;
  }

  public Long getIdSolicitudServicio() {
    return idSolicitudServicio;
  }

  public Long getIdReportante() {
    return idReportante;
  }

  public Long getIdReportado() {
    return idReportado;
  }

  public Long getIdAdministradorResponsable() {
    return idAdministradorResponsable;
  }

  public Short getIdMedidaAdministrativaActual() {
    return idMedidaAdministrativaActual;
  }

  public String getMotivo() {
    return motivo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public EstadoCasoModeracion getEstadoActual() {
    return estadoActual;
  }

  public ResultadoCasoModeracion getResultadoActual() {
    return resultadoActual;
  }

  public String getResolucionActual() {
    return resolucionActual;
  }

  public OffsetDateTime getFechaFinMedidaActual() {
    return fechaFinMedidaActual;
  }

  public OffsetDateTime getFechaApertura() {
    return fechaApertura;
  }

  public OffsetDateTime getFechaCierreActual() {
    return fechaCierreActual;
  }

  public OffsetDateTime getFechaActualizacion() {
    return fechaActualizacion;
  }
}
