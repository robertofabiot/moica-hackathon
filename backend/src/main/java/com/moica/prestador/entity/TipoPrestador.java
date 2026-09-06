package com.moica.prestador.entity;

/**
 * Tipo de organización o modalidad bajo la que trabaja el prestador.
 *
 * <p>Los valores son los del dominio {@code TipoPrestador} del diccionario de datos y la
 * restricción {@code ck_perfil_prestador_tipo} los repite en PostgreSQL.
 *
 * <p>Es un dato descriptivo: se muestra en el perfil y no cambia permisos ni funciones en el MVP
 * (definición 5.1). Profesionales y freelancers entran en {@link #INDEPENDIENTE}.
 */
public enum TipoPrestador {
  /** Persona que ofrece servicios por cuenta propia. */
  INDEPENDIENTE,
  /** Negocio o iniciativa en proceso de consolidación. */
  EMPRENDIMIENTO,
  /** Pequeña o mediana empresa que ofrece servicios. */
  PYME
}
