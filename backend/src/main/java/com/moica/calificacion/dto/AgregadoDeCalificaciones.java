package com.moica.calificacion.dto;

/**
 * Lo que devuelve la consulta agrupada de calificaciones para una persona y un rol.
 *
 * <p>Es una proyección de acceso a datos, no una superficie: no sale de la capacidad {@code
 * calificacion}. Existe para que el promedio, la cantidad y las cinco cuentas del desglose viajen
 * en una sola fila y una sola consulta, en lugar de siete recorridos de la misma tabla.
 *
 * <p>Todos los campos son objetos y no primitivos porque los produce el constructor de una consulta
 * JPQL: {@code AVG} devuelve {@code Double} y {@code COUNT} y {@code SUM} devuelven {@code Long}, y
 * el emparejamiento con el constructor es por tipo declarado.
 *
 * @param promedio media de las puntuaciones, tal como la calcula PostgreSQL y sin redondear
 */
public record AgregadoDeCalificaciones(
    Long idCalificado,
    Double promedio,
    Long cantidad,
    Long unaEstrella,
    Long dosEstrellas,
    Long tresEstrellas,
    Long cuatroEstrellas,
    Long cincoEstrellas) {}
