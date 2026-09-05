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
 * vigente. P10A añade ese cierre en {@link #cerrarVigencia(OffsetDateTime)} y la fotografía que lo
 * sustituye en {@link #siguienteDe}.
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

  /**
   * La fotografía que un evento administrativo deja del caso ya mutado.
   *
   * <p>Se construye <b>después</b> de aplicar el cambio sobre {@link CasoModeracion}, porque una
   * versión SCD2 retrata el estado resultante y no el anterior: responsable, estado, resultado,
   * resolución, medida y fecha de fin salen todos de la fila vigente. Por eso basta con el caso, el
   * evento y quién lo originó.
   *
   * <p>El número de versión lo calcula quien la crea a partir de la anterior, no esta clase: es la
   * transacción del servicio la que sabe cuál está cerrando, y {@code uq_historial_caso_version}
   * rechaza un duplicado si dos intentaran el mismo.
   *
   * <p>{@code estadoCuenta} es el estado real y vigente de la cuenta afectada en este instante.
   * P10A no lo cambia nunca: revisar y resolver un caso no sanciona a nadie. Se copia porque el
   * historial retrata también qué acceso tenía la persona cuando se tomó cada decisión.
   *
   * @param instante el mismo reloj con el que se cerró la versión anterior, para que los dos
   *     periodos se toquen sin superponerse
   */
  public static HistorialCaso siguienteDe(
      CasoModeracion caso,
      int numeroVersion,
      Long idAdministradorActor,
      TipoEventoHistorial tipoEvento,
      EstadoCuenta estadoCuentaAfectada,
      String detalleCambio,
      OffsetDateTime instante) {

    return fotografiar(
        caso,
        numeroVersion,
        TipoActorHistorial.ADMINISTRADOR,
        idAdministradorActor,
        tipoEvento,
        estadoCuentaAfectada,
        detalleCambio,
        instante);
  }

  /**
   * La fotografía de un evento que no originó ninguna persona.
   *
   * <p>Hoy solo lo produce {@link TipoEventoHistorial#MEDIDA_EXPIRADA}: cuando llega la fecha que
   * una persona administradora fijó, el plazo se cumple solo y nadie vuelve a decidir nada. Por eso
   * el actor es {@link TipoActorHistorial#SISTEMA} y queda sin identificador, como exige {@code
   * ck_historial_caso_actor}.
   *
   * <p>Que el actor sea el sistema no convierte la expiración en una sanción automática: la medida,
   * su severidad y su plazo los eligió una persona, y esta versión solo registra que el plazo se
   * agotó. Moica no selecciona, no recomienda y no escala medidas, según la definición 11.3.
   *
   * <p>{@code idAdministradorResponsable} sí se conserva: quien respondía por el caso lo sigue
   * haciendo aunque este evento concreto no lo originara nadie. El actor del evento y el
   * responsable de la versión son campos distintos a propósito.
   */
  public static HistorialCaso delSistemaDe(
      CasoModeracion caso,
      int numeroVersion,
      TipoEventoHistorial tipoEvento,
      EstadoCuenta estadoCuentaAfectada,
      String detalleCambio,
      OffsetDateTime instante) {

    return fotografiar(
        caso,
        numeroVersion,
        TipoActorHistorial.SISTEMA,
        null,
        tipoEvento,
        estadoCuentaAfectada,
        detalleCambio,
        instante);
  }

  private static HistorialCaso fotografiar(
      CasoModeracion caso,
      int numeroVersion,
      TipoActorHistorial tipoActor,
      Long idActor,
      TipoEventoHistorial tipoEvento,
      EstadoCuenta estadoCuentaAfectada,
      String detalleCambio,
      OffsetDateTime instante) {

    HistorialCaso version =
        new HistorialCaso(
            caso.getIdCasoModeracion(),
            caso.getIdReportado(),
            idActor,
            numeroVersion,
            tipoActor,
            tipoEvento,
            caso.getEstadoActual(),
            estadoCuentaAfectada,
            detalleCambio,
            instante);

    version.idAdministradorResponsable = caso.getIdAdministradorResponsable();
    version.idMedidaAdministrativa = caso.getIdMedidaAdministrativaActual();
    version.resultadoCaso = caso.getResultadoActual();
    version.resolucion = caso.getResolucionActual();
    version.fechaFinMedida = caso.getFechaFinMedidaActual();
    return version;
  }

  /**
   * Deja de ser la versión vigente.
   *
   * <p>El fin es exclusivo, así que recibe el mismo instante en el que empieza la versión que la
   * sustituye: los dos periodos se tocan sin solaparse y {@code ex_historial_caso_vigencia} los
   * admite. Cerrar y crear ocurren en la misma transacción; si una fallara, el caso se quedaría sin
   * versión vigente o con dos.
   *
   * @throws IllegalStateException si la versión ya estaba cerrada, porque cerrarla otra vez movería
   *     un periodo histórico que debe permanecer intacto
   */
  public void cerrarVigencia(OffsetDateTime instante) {
    if (!esVersionActual) {
      throw new IllegalStateException(
          "La versión " + idHistorialCaso + " ya estaba cerrada y no puede cerrarse otra vez");
    }
    this.fechaFinVigencia = instante;
    this.esVersionActual = false;
  }
}
