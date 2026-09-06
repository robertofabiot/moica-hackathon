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
 * campos administrativos —responsable, estado, resultado, resolución y sus fechas— nacen como los
 * deja la apertura y los mueve la revisión administrativa: P10A añade las tres mutaciones que
 * siguen. La medida y su fecha de fin siguen sin mutador porque aplicarlas es P10B.
 *
 * <p>Ninguna de las tres decide si puede ejecutarse: la autorización, la transición válida y el
 * cierre de la versión histórica anterior son responsabilidad del servicio, que las envuelve en una
 * sola transacción. Aquí solo se deja el estado vigente coherente con lo que la base exige.
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

  /**
   * Deja el caso a cargo de una persona administradora.
   *
   * <p>Sirve tanto para la primera asignación como para una reasignación posterior: el efecto sobre
   * la fila vigente es el mismo y lo que distingue una de otra queda en el historial.
   *
   * <p>No toca el estado. Asignar es decir quién responde por el caso, no empezar a revisarlo.
   */
  public void asignarResponsable(Long idAdministradorResponsable, OffsetDateTime instante) {
    this.idAdministradorResponsable = idAdministradorResponsable;
    this.fechaActualizacion = instante;
  }

  /**
   * Pone el caso en revisión.
   *
   * <p>Llega desde {@link EstadoCasoModeracion#ABIERTO} o desde {@link
   * EstadoCasoModeracion#REABIERTO}; en los dos casos el expediente vuelve al análisis y deja de
   * tener una decisión vigente, así que resultado, resolución y fecha de cierre quedan nulos. Es lo
   * que exige {@code ck_caso_moderacion_cierre}: solo un caso cerrado los lleva.
   */
  public void iniciarRevision(OffsetDateTime instante) {
    this.estadoActual = EstadoCasoModeracion.EN_REVISION;
    this.resultadoActual = null;
    this.resolucionActual = null;
    this.fechaCierreActual = null;
    this.fechaActualizacion = instante;
  }

  /**
   * Cierra el caso con la decisión de quien lo revisó.
   *
   * <p>Resultado, resolución y fecha de cierre entran juntos porque {@code
   * ck_caso_moderacion_cierre} los exige a la vez: un caso cerrado sin decisión no diría nada, y
   * una decisión sin cierre no sería vigente.
   *
   * <p>Cerrar no aplica ninguna medida ni cambia ninguna cuenta. {@link
   * ResultadoCasoModeracion#PROCEDENTE} dice que el caso amerita una decisión administrativa, no
   * que Moica ya la haya tomado: elegir y aplicar la medida es P10B y siempre lo hace una persona.
   */
  public void cerrar(
      ResultadoCasoModeracion resultado, String resolucion, OffsetDateTime instante) {
    this.estadoActual = EstadoCasoModeracion.CERRADO;
    this.resultadoActual = resultado;
    this.resolucionActual = resolucion;
    this.fechaCierreActual = instante;
    this.fechaActualizacion = instante;
  }

  /**
   * Deja constancia de que este expediente sostiene una medida sobre la cuenta reportada.
   *
   * <p>La medida vive en el caso, no en la cuenta: la cuenta solo conserva la <em>proyección</em>
   * de su efecto en {@code estadoCuenta}. Así el expediente sigue diciendo por qué la persona quedó
   * como quedó, que es lo que una apelación necesita revisar.
   *
   * <p>No toca el estado del caso ni su resolución: aplicar una medida es la consecuencia de una
   * resolución ya registrada, no otra resolución.
   *
   * @param fechaFin cuándo termina la medida, o nulo si no termina sola. Cuando llega, {@code
   *     MedidasDeCasoService} la expira; hasta entonces solo se levanta revocándola
   */
  public void aplicarMedida(
      Short idMedidaAdministrativa, OffsetDateTime fechaFin, OffsetDateTime instante) {
    this.idMedidaAdministrativaActual = idMedidaAdministrativa;
    this.fechaFinMedidaActual = fechaFin;
    this.fechaActualizacion = instante;
  }

  /**
   * Retira la medida que este expediente sostenía.
   *
   * <p>Es lo mismo revocarla a mano que verla expirar: en los dos casos el caso deja de sostener
   * una sanción vigente y lo que distingue una cosa de la otra es el evento que queda en el
   * historial. Por eso hay un solo mutador y no dos.
   *
   * <p>La medida y su fecha quedan nulas también en la fila, no marcadas de ninguna forma: la
   * evidencia de que existió está en las versiones del historial, que sí la conservan.
   */
  public void retirarMedida(OffsetDateTime instante) {
    this.idMedidaAdministrativaActual = null;
    this.fechaFinMedidaActual = null;
    this.fechaActualizacion = instante;
  }

  /**
   * Reabre un caso cerrado porque una apelación aceptada lo justificó.
   *
   * <p>Resultado, resolución y fecha de cierre quedan nulos porque {@code
   * ck_caso_moderacion_cierre} los exige solo en {@link EstadoCasoModeracion#CERRADO}: una decisión
   * que dejó de ser definitiva no puede seguir figurando como vigente. <b>No se pierde</b>: la
   * versión del historial que la registró la conserva íntegra, y por eso reabrir crea una versión
   * nueva en lugar de reescribir la anterior.
   *
   * <p>La medida sí sobrevive. Reabrir el expediente no levanta la sanción: si quien revisa decide
   * levantarla, la revoca, y eso es otra decisión con su propio evento.
   */
  public void reabrir(OffsetDateTime instante) {
    this.estadoActual = EstadoCasoModeracion.REABIERTO;
    this.resultadoActual = null;
    this.resolucionActual = null;
    this.fechaCierreActual = null;
    this.fechaActualizacion = instante;
  }
}
