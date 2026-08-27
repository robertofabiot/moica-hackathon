package com.moica.prestador.entity;

/**
 * Si el prestador acepta nuevas solicitudes.
 *
 * <p>Los valores son los del dominio {@code EstadoDisponibilidad} del diccionario de datos y la
 * restricción {@code ck_perfil_prestador_disponibilidad} los repite en PostgreSQL.
 *
 * <p>En P4 solo se administra; su efecto sobre servicios y solicitudes llega con esos incrementos.
 */
public enum EstadoDisponibilidad {
  /** El prestador acepta nuevas solicitudes. */
  DISPONIBLE,
  /** El prestador no acepta nuevas solicitudes. */
  NO_DISPONIBLE
}
