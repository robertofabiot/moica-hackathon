package com.moica.moderacion.entity;

/**
 * Decisión con la que una persona administradora cierra la investigación de un caso.
 *
 * <p>Los valores son los del dominio {@code ResultadoCasoModeracion} del diccionario de datos y las
 * restricciones {@code ck_caso_moderacion_resultado} y {@code ck_historial_caso_resultado_caso} los
 * repiten en PostgreSQL.
 *
 * <p>P9 nunca lo escribe: un caso recién abierto no tiene resultado, y registrarlo es una acción
 * administrativa de P10A. Existe aquí porque la columna existe y las entidades la mapean.
 */
public enum ResultadoCasoModeracion {
  /** La investigación confirmó que el caso amerita una decisión administrativa. */
  PROCEDENTE,
  /** La investigación concluyó que el caso no amerita una medida administrativa. */
  DESESTIMADO
}
