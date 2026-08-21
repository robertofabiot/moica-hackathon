package com.moica.auth.entity;

/**
 * Causa por la que una sesión dejó de ser válida antes de expirar.
 *
 * <p>Los valores son los del dominio {@code MotivoRevocacionSesion} del diccionario de datos y la
 * restricción {@code ck_sesion_motivo_revocacion} los repite en PostgreSQL.
 *
 * <p>P2 solo produce {@link #CIERRE_VOLUNTARIO}. El cambio de credenciales y las medidas
 * administrativas revocan sesiones en P3 y en P10B.
 */
public enum MotivoRevocacionSesion {
  /** La persona cerró la sesión desde la aplicación. */
  CIERRE_VOLUNTARIO,
  /** La sesión se invalidó tras modificar la contraseña o el segundo factor. */
  CAMBIO_CREDENCIALES,
  /** La sesión se invalidó por una medida administrativa aplicada a la cuenta. */
  MEDIDA_ADMINISTRATIVA
}
