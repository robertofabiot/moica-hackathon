package com.moica.admin.dto;

/**
 * Una persona administradora, con lo justo para elegirla en una reasignación.
 *
 * <p>Solo identificador y nombre. No lleva correo, fecha de asignación ni estado de cuenta: quien
 * reasigna un caso necesita saber a quién se lo pasa, no leer su ficha.
 */
public record DatosDeAdministrador(Long idAdministrador, String nombreCompleto) {}
