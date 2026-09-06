package com.moica.usuario.service;

import com.moica.usuario.service.AdministradorService.ResultadoDeAsignacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Asigna el rol administrativo al arrancar, si el entorno indica a quién.
 *
 * <p>Es la única vía por la que existe un administrador en Moica: no hay registro público de
 * administradores, ni endpoint de promoción, ni contraseña fija, ni ningún secreto versionado. La
 * cuenta se registra como cualquier otra y después el entorno declara cuál de ellas manda.
 *
 * <p>Se ejecuta en cada arranque y es idempotente: si la cuenta ya tiene el rol no cambia nada. Si
 * el correo no corresponde a ninguna cuenta, deja un aviso y sigue: impedir el arranque por eso
 * dejaría la aplicación caída justo cuando falta registrar esa cuenta.
 *
 * <p>Ningún mensaje incluye el correo. Es un dato personal y un arranque suele quedar registrado.
 */
@Component
public class BootstrapDeAdministrador implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(BootstrapDeAdministrador.class);

  private final PropiedadesDeAdministracion propiedades;
  private final AdministradorService administradores;

  public BootstrapDeAdministrador(
      PropiedadesDeAdministracion propiedades, AdministradorService administradores) {
    this.propiedades = propiedades;
    this.administradores = administradores;
  }

  @Override
  public void run(ApplicationArguments argumentos) {
    if (!propiedades.hayCuentaIndicada()) {
      LOG.info("MOICA_ADMIN_CORREO no está definida: no se asigna el rol administrativo.");
      return;
    }

    ResultadoDeAsignacion resultado = administradores.asignarloA(propiedades.correo());

    switch (resultado) {
      case ASIGNADO ->
          LOG.info(
              "Rol administrativo asignado a la cuenta indicada en MOICA_ADMIN_CORREO."
                  + " Para entrar en /admin debe activar su segundo factor TOTP.");
      case YA_LO_TENIA ->
          LOG.debug("La cuenta indicada en MOICA_ADMIN_CORREO ya tenía el rol administrativo.");
      case CUENTA_INEXISTENTE ->
          LOG.warn(
              "MOICA_ADMIN_CORREO apunta a una cuenta que todavía no existe."
                  + " Regístrala desde la aplicación y vuelve a arrancar para asignarle el rol"
                  + " administrativo.");
    }
  }
}
