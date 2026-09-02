package com.moica.moderacion.entity;

import com.moica.usuario.entity.EstadoCuenta;
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
 * Versión SCD Tipo 2 de un caso de moderación.
 *
 * <p>Corresponde con la tabla {@code historial_caso} que crea la migración {@code V50}. Cada fila
 * es la fotografía completa del caso y del estado de la cuenta afectada durante un periodo, no solo
 * el campo que cambió: así una decisión anterior puede reconstruirse tal como se tomó.
 *
 * <p>La vigencia es un intervalo semiabierto {@code [inicio, fin)}. La versión actual deja el fin
 * nulo; cuando un evento posterior la sustituya, se cerrará con el mismo instante en que empiece la
 * siguiente, sin que los dos periodos se superpongan. La restricción {@code
 * ex_historial_caso_vigencia} de {@code V51} lo sostiene en PostgreSQL.
 *
 * <p>Todo lo histórico es {@code updatable = false}. Los dos únicos campos que llegan a cambiar son
 * {@code fechaFinVigencia} y {@code esVersionActual}, y solo una vez: al dejar de ser la versión
 * vigente. P9 no ejecuta ese cierre —abre el caso y crea su primera versión, y nada más—, así que
 * esta clase todavía no ofrece la operación que lo hará en P10A.
 */
@Entity
@Table(name = "historial_caso")
public class HistorialCaso {

  /** Número de la versión que nace junto con el caso. */
  public static final int VERSION_INICIAL = 1;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_historial_caso")
  private Long idHistorialCaso;

  @Column(name = "id_caso_moderacion", nullable = false, updatable = false)
  private Long idCasoModeracion;

  @Column(name = "id_usuario_afectado", nullable = false, updatable = false)
  private Long idUsuarioAfectado;

  @Column(name = "id_actor", updatable = false)
  private Long idActor;

  @Column(name = "id_administrador_responsable", updatable = false)
  private Long idAdministradorResponsable;

  @Column(name = "id_medida_administrativa", updatable = false)
  private Short idMedidaAdministrativa;

  @Column(name = "numero_version", nullable = false, updatable = false)
  private int numeroVersion;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_actor", nullable = false, updatable = false, length = 30)
  private TipoActorHistorial tipoActor;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_evento", nullable = false, updatable = false, length = 30)
  private TipoEventoHistorial tipoEvento;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_caso", nullable = false, updatable = false, length = 30)
  private EstadoCasoModeracion estadoCaso;

  @Enumerated(EnumType.STRING)
  @Column(name = "resultado_caso", updatable = false, length = 30)
  private ResultadoCasoModeracion resultadoCaso;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_cuenta", nullable = false, updatable = false, length = 30)
  private EstadoCuenta estadoCuenta;

  @Column(name = "resolucion", updatable = false)
  private String resolucion;

  @Column(name = "fecha_fin_medida", updatable = false)
  private OffsetDateTime fechaFinMedida;

  @Column(name = "detalle_cambio", nullable = false, updatable = false)
  private String detalleCambio;

  @Column(name = "fecha_inicio_vigencia", nullable = false, updatable = false)
  private OffsetDateTime fechaInicioVigencia;

  @Column(name = "fecha_fin_vigencia")
  private OffsetDateTime fechaFinVigencia;

  @Column(name = "es_version_actual", nullable = false)
  private boolean esVersionActual;

  @Column(name = "fecha_registro", nullable = false, updatable = false)
  private OffsetDateTime fechaRegistro;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected HistorialCaso() {}

  private HistorialCaso(
      Long idCasoModeracion,
      Long idUsuarioAfectado,
      Long idActor,
      int numeroVersion,
      TipoActorHistorial tipoActor,
      TipoEventoHistorial tipoEvento,
      EstadoCasoModeracion estadoCaso,
      EstadoCuenta estadoCuenta,
      String detalleCambio,
      OffsetDateTime instante) {
    this.idCasoModeracion = idCasoModeracion;
    this.idUsuarioAfectado = idUsuarioAfectado;
    this.idActor = idActor;
    this.numeroVersion = numeroVersion;
    this.tipoActor = tipoActor;
    this.tipoEvento = tipoEvento;
    this.estadoCaso = estadoCaso;
    this.estadoCuenta = estadoCuenta;
    this.detalleCambio = detalleCambio;
    this.fechaInicioVigencia = instante;
    this.esVersionActual = true;
    this.fechaRegistro = instante;
  }

  /**
   * La versión con la que nace un caso, creada en la misma transacción que lo abre.
   *
   * <p>Fija todo lo que la apertura significa y nada más: la versión es la primera, el evento es
   * {@link TipoEventoHistorial#CASO_ABIERTO}, el actor es la persona que reportó, la cuenta
   * afectada es la reportada y el caso queda {@link EstadoCasoModeracion#ABIERTO}. Responsable,
   * medida, resultado, resolución y fecha de fin de medida quedan nulos porque reportar no asigna,
   * no sanciona y no resuelve.
   *
   * <p>{@code estadoCuenta} es el estado real y vigente de la cuenta reportada en este instante, no
   * una consecuencia del reporte: el historial fotografía lo que había, y abrir un caso no cambia
   * ninguna cuenta.
   *
   * @param instante el mismo reloj con el que se abre el caso, para que ambas fechas coincidan
   */
  public static HistorialCaso deAperturaDe(
      CasoModeracion caso,
      EstadoCuenta estadoCuentaReportada,
      String detalleCambio,
      OffsetDateTime instante) {

    return new HistorialCaso(
        caso.getIdCasoModeracion(),
        caso.getIdReportado(),
        caso.getIdReportante(),
        VERSION_INICIAL,
        TipoActorHistorial.USUARIO,
        TipoEventoHistorial.CASO_ABIERTO,
        EstadoCasoModeracion.ABIERTO,
        estadoCuentaReportada,
        detalleCambio,
        instante);
  }

  public Long getIdHistorialCaso() {
    return idHistorialCaso;
  }

  public Long getIdCasoModeracion() {
    return idCasoModeracion;
  }

  public Long getIdUsuarioAfectado() {
    return idUsuarioAfectado;
  }

  public Long getIdActor() {
    return idActor;
  }

  public Long getIdAdministradorResponsable() {
    return idAdministradorResponsable;
  }

  public Short getIdMedidaAdministrativa() {
    return idMedidaAdministrativa;
  }

  public int getNumeroVersion() {
    return numeroVersion;
  }

  public TipoActorHistorial getTipoActor() {
    return tipoActor;
  }

  public TipoEventoHistorial getTipoEvento() {
    return tipoEvento;
  }

  public EstadoCasoModeracion getEstadoCaso() {
    return estadoCaso;
  }

  public ResultadoCasoModeracion getResultadoCaso() {
    return resultadoCaso;
  }

  public EstadoCuenta getEstadoCuenta() {
    return estadoCuenta;
  }

  public String getResolucion() {
    return resolucion;
  }

  public OffsetDateTime getFechaFinMedida() {
    return fechaFinMedida;
  }

  public String getDetalleCambio() {
    return detalleCambio;
  }

  public OffsetDateTime getFechaInicioVigencia() {
    return fechaInicioVigencia;
  }

  public OffsetDateTime getFechaFinVigencia() {
    return fechaFinVigencia;
  }

  public boolean isEsVersionActual() {
    return esVersionActual;
  }

  public OffsetDateTime getFechaRegistro() {
    return fechaRegistro;
  }
}
