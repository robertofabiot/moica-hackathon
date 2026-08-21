package com.moica.usuario.entity;

/**
 * Estado operativo vigente de una cuenta.
 *
 * <p>Los valores son los del dominio {@code EstadoCuenta} del diccionario de datos y la restricción
 * {@code ck_usuario_estado_cuenta} los repite en PostgreSQL.
 *
 * <p>P2 solo persiste el estado: toda cuenta nueva nace {@link #ACTIVA}. Los efectos de los estados
 * restringidos y suspendidos sobre lo que cada cuenta puede hacer corresponden a la matriz de
 * permisos de P3 en adelante.
 */
public enum EstadoCuenta {
  /** La cuenta puede utilizar las funciones permitidas por su perfil. */
  ACTIVA,
  /** La cuenta conserva acceso, pero algunas funciones quedan limitadas hasta una fecha. */
  RESTRINGIDA_TEMPORAL,
  /** La cuenta queda bloqueada hasta una fecha determinada. */
  SUSPENDIDA_TEMPORAL,
  /** La cuenta queda bloqueada sin fecha automática de reactivación. */
  SUSPENDIDA_PERMANENTE
}
