package com.moica.moderacion.entity;

/**
 * Decisión con la que una persona administradora cierra la investigación de un caso.
 *
 * <p>Los valores son los del dominio {@code ResultadoCasoModeracion} del diccionario de datos y las
 * restricciones {@code ck_caso_moderacion_resultado} y {@code ck_historial_caso_resultado_caso} los
 * repiten en PostgreSQL.
 *
 * <p>Un caso recién abierto no tiene resultado: lo escribe quien lo cierra, y solo al cerrarlo.
 * Ninguno de los dos valores aplica una medida ni cambia el estado de una cuenta; {@link
 * #PROCEDENTE} dice que el caso amerita una decisión, no que Moica ya la haya tomado.
 */
public enum ResultadoCasoModeracion {
  /** La investigación confirmó que el caso amerita una decisión administrativa. */
  PROCEDENTE,
  /** La investigación concluyó que el caso no amerita una medida administrativa. */
  DESESTIMADO
}
