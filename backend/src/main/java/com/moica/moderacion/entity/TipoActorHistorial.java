package com.moica.moderacion.entity;

/**
 * Quién originó el evento que produjo una versión del historial de un caso.
 *
 * <p>Los valores son los del dominio {@code TipoActorHistorial} del diccionario de datos y la
 * restricción {@code ck_historial_caso_tipo_actor} los repite en PostgreSQL, junto con la
 * correspondencia que exige: {@link #SISTEMA} deja el actor nulo y los otros dos lo identifican.
 *
 * <p>P9 solo produce {@link #USUARIO}: quien abre un caso es la persona que reporta.
 */
public enum TipoActorHistorial {
  /** El evento fue originado por un usuario no administrativo, como al reportar. */
  USUARIO,
  /** El evento fue originado por una cuenta con permisos administrativos. */
  ADMINISTRADOR,
  /** El evento fue producido automáticamente, por ejemplo al vencer una medida temporal. */
  SISTEMA
}
