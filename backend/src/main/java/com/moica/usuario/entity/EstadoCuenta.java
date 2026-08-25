package com.moica.usuario.entity;

/**
 * Estado operativo vigente de una cuenta.
 *
 * <p>Los valores son los del dominio {@code EstadoCuenta} del diccionario de datos y la restricción
 * {@code ck_usuario_estado_cuenta} los repite en PostgreSQL.
 *
 * <p>El estado es una <em>proyección operativa</em>: quien lo cambia es la moderación (P10B), que
 * además conserva la evidencia en el historial del caso. Aquí solo se lee.
 */
public enum EstadoCuenta {
  /** La cuenta puede utilizar las funciones permitidas por su perfil. */
  ACTIVA,
  /** La cuenta conserva acceso, pero algunas funciones quedan limitadas hasta una fecha. */
  RESTRINGIDA_TEMPORAL,
  /** La cuenta queda bloqueada hasta una fecha determinada. */
  SUSPENDIDA_TEMPORAL,
  /** La cuenta queda bloqueada sin fecha automática de reactivación. */
  SUSPENDIDA_PERMANENTE;

  /**
   * Indica si el estado impide usar la aplicación.
   *
   * <p>Las dos suspensiones bloquean la cuenta, así que su sesión no sirve para nada salvo
   * consultarla y cerrarla. {@link #RESTRINGIDA_TEMPORAL} no bloquea: limita funciones concretas, y
   * ninguna de las que existen hoy —cambiar la propia contraseña y administrar el propio segundo
   * factor— tiene sentido limitar, porque son precisamente las que protegen la cuenta.
   *
   * <p>La expiración automática de una suspensión temporal no se resuelve aquí: la proyección la
   * actualiza la moderación cuando la medida vence (P10B). Este método lee el estado vigente, no lo
   * recalcula.
   */
  public boolean bloqueaElAcceso() {
    return this == SUSPENDIDA_TEMPORAL || this == SUSPENDIDA_PERMANENTE;
  }
}
