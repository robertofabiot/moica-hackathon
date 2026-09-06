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

/**
 * Una sanción del catálogo que una persona administradora puede aplicar a una cuenta.
 *
 * <p>Corresponde con la tabla {@code medida_administrativa} que crea la migración {@code V50}. P9
 * la dejó vacía a propósito porque el caso y su historial la referencian; P10B la llena y la
 * gobierna.
 *
 * <p>El catálogo describe la sanción; no la decide. {@link #nivelSeveridad} ordena las medidas para
 * quien elige y nada más: no activa ninguna regla, no escala por reincidencia y no recomienda,
 * según la definición 11.3 y la decisión D-MOD-01. Quien decide es siempre una persona.
 *
 * <p>Dos campos gobiernan lo que ocurre al aplicarla:
 *
 * <ul>
 *   <li>{@link #estadoCuentaResultante} es el estado operativo en el que queda la cuenta. Puede ser
 *       nulo: una advertencia queda registrada en el expediente sin tocar el acceso.
 *   <li>{@link #requiereFechaFin} obliga a que quien la aplique indique cuándo termina. Es lo que
 *       distingue una medida temporal, que el sistema puede expirar sola cuando llegue el plazo que
 *       una persona fijó, de una permanente, que solo se levanta revocándola.
 * </ul>
 *
 * <p><b>Nunca se borra.</b> Una medida referenciada por un caso o por una versión del historial es
 * parte de la evidencia de una decisión pasada, y por eso todas sus claves foráneas son {@code
 * RESTRICT}. «Eliminar» desde el negocio significa {@link #cambiarHabilitacion(boolean)
 * deshabilitar}: la medida deja de ofrecerse para aplicaciones nuevas y sigue describiendo
 * correctamente las decisiones anteriores.
 */
@Entity
@Table(name = "medida_administrativa")
public class MedidaAdministrativa {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_medida_administrativa")
  private Short idMedidaAdministrativa;

  /**
   * Identificador estable con el que se nombra la medida fuera de la base.
   *
   * <p>Es {@code updatable = false} a propósito: el nombre y la descripción pueden reescribirse
   * para explicar mejor la misma sanción, pero un código que cambia dejaría de identificar la
   * medida que las decisiones anteriores citaron.
   */
  @Column(name = "codigo", nullable = false, updatable = false, length = 50)
  private String codigo;

  @Column(name = "nombre", nullable = false, length = 100)
  private String nombre;

  @Column(name = "descripcion")
  private String descripcion;

  @Column(name = "nivel_severidad", nullable = false)
  private short nivelSeveridad;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_cuenta_resultante", length = 30)
  private EstadoCuenta estadoCuentaResultante;

  @Column(name = "requiere_fecha_fin", nullable = false)
  private boolean requiereFechaFin;

  @Column(name = "habilitada", nullable = false)
  private boolean habilitada;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected MedidaAdministrativa() {}

  /**
   * Crea una medida del catálogo, ya validada por el servicio.
   *
   * <p>Nace habilitada: se crea para poder usarla. Deshabilitarla es una decisión posterior.
   */
  public MedidaAdministrativa(
      String codigo,
      String nombre,
      String descripcion,
      short nivelSeveridad,
      EstadoCuenta estadoCuentaResultante,
      boolean requiereFechaFin) {

    this.codigo = codigo;
    this.nombre = nombre;
    this.descripcion = descripcion;
    this.nivelSeveridad = nivelSeveridad;
    this.estadoCuentaResultante = estadoCuentaResultante;
    this.requiereFechaFin = requiereFechaFin;
    this.habilitada = true;
  }

  public Short getIdMedidaAdministrativa() {
    return idMedidaAdministrativa;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getNombre() {
    return nombre;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public short getNivelSeveridad() {
    return nivelSeveridad;
  }

  public EstadoCuenta getEstadoCuentaResultante() {
    return estadoCuentaResultante;
  }

  public boolean isRequiereFechaFin() {
    return requiereFechaFin;
  }

  public boolean isHabilitada() {
    return habilitada;
  }

  /**
   * Reescribe la medida sin cambiar su código.
   *
   * <p>Editar afecta a las aplicaciones futuras, no a las pasadas: las decisiones ya tomadas
   * conservan en su versión del historial el estado de cuenta y la fecha de fin que realmente se
   * les aplicó, así que ninguna se reescribe por cambiar aquí una descripción o una severidad.
   */
  public void editar(
      String nombre,
      String descripcion,
      short nivelSeveridad,
      EstadoCuenta estadoCuentaResultante,
      boolean requiereFechaFin) {

    this.nombre = nombre;
    this.descripcion = descripcion;
    this.nivelSeveridad = nivelSeveridad;
    this.estadoCuentaResultante = estadoCuentaResultante;
    this.requiereFechaFin = requiereFechaFin;
  }

  /**
   * Habilita o deshabilita la medida.
   *
   * <p>Es lo que el negocio llama «eliminar» cuando la medida ya tiene historia. Una deshabilitada
   * deja de ofrecerse para aplicaciones nuevas y conserva intacto todo lo demás: las medidas ya
   * aplicadas siguen vigentes y las versiones que la citan la siguen describiendo.
   */
  public void cambiarHabilitacion(boolean habilitada) {
    this.habilitada = habilitada;
  }
}
