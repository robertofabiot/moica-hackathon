package com.moica.moderacion.dto;

import jakarta.validation.constraints.NotNull;

/**
 * A qué persona administradora queda asignado un caso.
 *
 * <p>Es un cuerpo y no un parámetro de ruta porque asignar es una decisión, no una navegación: el
 * identificador viaja donde viajan los datos de la petición.
 *
 * <p>Que el destinatario exista y tenga el rol administrativo lo comprueba el servicio: una
 * anotación no puede consultar la base.
 */
public record AsignacionDeCaso(
    @NotNull(message = "Elige a la persona administradora responsable.") Long idAdministrador) {}
