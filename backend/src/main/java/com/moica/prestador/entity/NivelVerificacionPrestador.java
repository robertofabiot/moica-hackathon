package com.moica.prestador.entity;

/**
 * Nivel de verificación documental vigente del perfil.
 *
 * <p>Los valores son los del dominio {@code NivelVerificacionPrestador} del diccionario de datos y
 * la restricción {@code ck_perfil_prestador_nivel_verificacion} los repite en PostgreSQL.
 *
 * <p>Es una <em>proyección</em> que solo actualizará el flujo de verificación documental (P4V) al
 * aprobar o revocar una solicitud. Ningún endpoint de P4 permite que el propietario lo cambie: aquí
 * solo se lee. Sin {@link #VERIFICADO_BASICO} el perfil no aparece en ninguna superficie pública.
 */
public enum NivelVerificacionPrestador {
  /** Estado inicial de todo perfil recién creado; el perfil es privado. */
  SIN_VERIFICAR,
  /** Una persona administradora aprobó la documentación oficial de identidad. */
  VERIFICADO_BASICO,
  /** Sobre la básica, se aprobó documentación profesional, técnica o comercial. */
  PROFESIONAL_VERIFICADO
}
