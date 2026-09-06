package com.moica.admin.dto;

import java.time.OffsetDateTime;

/**
 * Lo que ve una persona administradora al entrar en el área administrativa.
 *
 * <p>Es deliberadamente mínimo: P3 protege el área, no la llena. Las bandejas de verificación
 * documental y de moderación llegan con P4V y P10.
 *
 * @param nombreCompleto nombre de la cuenta administradora
 * @param correoElectronico su correo, para que se vea con qué cuenta se entró
 * @param fechaAsignacion cuándo recibió los permisos administrativos
 */
public record ResumenAdministrativo(
    String nombreCompleto, String correoElectronico, OffsetDateTime fechaAsignacion) {}
