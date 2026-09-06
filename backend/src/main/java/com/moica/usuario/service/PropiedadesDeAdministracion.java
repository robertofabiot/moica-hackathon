package com.moica.usuario.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del rol administrativo.
 *
 * @param correo correo de la cuenta ordinaria que debe recibir el rol administrativo al arrancar.
 *     Llega por la variable de entorno {@code MOICA_ADMIN_CORREO} y viene vacío por omisión: sin
 *     ella Moica no promueve a nadie. No es un secreto, pero tampoco se registra en los logs
 */
@ConfigurationProperties("moica.administracion")
public record PropiedadesDeAdministracion(String correo) {

  public PropiedadesDeAdministracion {
    correo = (correo == null) ? "" : correo.strip();
  }

  /** Indica si alguien configuró a quién promover. */
  public boolean hayCuentaIndicada() {
    return !correo.isBlank();
  }
}
