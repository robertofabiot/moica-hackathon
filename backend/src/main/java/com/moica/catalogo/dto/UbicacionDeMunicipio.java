package com.moica.catalogo.dto;

/**
 * Cómo se describe un municipio ya elegido: su nombre, su departamento y si Moica opera ahí.
 *
 * <p>Lo consume la capacidad {@code prestador} para validar el municipio principal de un perfil y
 * para mostrarlo sin repetir la consulta al catálogo.
 */
public record UbicacionDeMunicipio(
    Integer idMunicipio,
    String nombreMunicipio,
    String nombreDepartamento,
    boolean departamentoHabilitado) {}
