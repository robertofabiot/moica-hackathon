package com.moica.auth.entity;

/**
 * Estado vigente del segundo factor de una cuenta.
 *
 * <p>Los valores son los del dominio {@code EstadoSegundoFactor} del diccionario de datos y la
 * restricción {@code ck_segundo_factor_usuario_estado} los repite en PostgreSQL.
 */
public enum EstadoSegundoFactor {
  /** El secreto fue generado pero la cuenta aún no confirma un código válido. */
  PENDIENTE_ACTIVACION,
  /** La cuenta debe presentar un código temporal válido para completar el inicio de sesión. */
  ACTIVO,
  /** La cuenta suspendió el segundo factor y vuelve a autenticarse solo con contraseña. */
  DESACTIVADO
}
