package com.moica.moderacion.entity;

/**
 * Etapa vigente del proceso de moderación de un caso.
 *
 * <p>Los valores son los del dominio {@code EstadoCasoModeracion} del diccionario de datos y las
 * restricciones {@code ck_caso_moderacion_estado} y {@code ck_historial_caso_estado_caso} los
 * repiten en PostgreSQL.
 *
 * <p>El estado describe en qué punto va la revisión; el resultado describe la decisión. Son cosas
 * distintas y por eso viajan en columnas distintas.
 *
 * <p>P9 solo produce {@link #ABIERTO}: reportar abre el expediente y nada más. Las transiciones a
 * los demás estados son administrativas y llegan en P10A.
 */
public enum EstadoCasoModeracion {
  /** El caso fue recibido y espera asignación o revisión. */
  ABIERTO,
  /** Un administrador analiza los hechos y antecedentes del caso. */
  EN_REVISION,
  /** El caso posee una resolución y un resultado vigentes. */
  CERRADO,
  /** Una decisión previa dejó de ser definitiva y el caso volvió a revisión. */
  REABIERTO
}
