package com.moica.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.PruebaDeIntegracionConPostgres;
import com.moica.usuario.service.PropiedadesDeAdministracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Arranque con {@code MOICA_ADMIN_CORREO} apuntando a una cuenta que todavía no existe.
 *
 * <p>Es el caso normal la primera vez que se despliega Moica: la variable se configura antes de que
 * nadie se haya registrado. La aplicación debe arrancar igual y limitarse a avisar; caerse dejaría
 * el sistema inservible justo cuando falta registrar esa cuenta.
 *
 * <p>Esta clase estrena su propio contexto porque cambia una propiedad de configuración. Reutiliza
 * el contenedor de PostgreSQL de la suite, así que el coste es el del contexto y no el de una base
 * de datos nueva.
 */
@TestPropertySource(
    properties =
        "moica.administracion.correo=" + ArranqueConAdministradorInexistenteIT.CORREO_SIN_CUENTA)
class ArranqueConAdministradorInexistenteIT extends PruebaDeIntegracionConPostgres {

  static final String CORREO_SIN_CUENTA = "nadie-todavia@moica.test";

  @Autowired private ConfigurableApplicationContext contexto;
  @Autowired private PropiedadesDeAdministracion propiedades;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void laAplicacionArrancaAunqueLaCuentaIndicadaNoExista() {
    assertThat(propiedades.correo())
        .as("el escenario solo vale si la variable llegó de verdad a la aplicación")
        .isEqualTo(CORREO_SIN_CUENTA);
    assertThat(propiedades.hayCuentaIndicada()).isTrue();
    assertThat(contexto.isRunning())
        .as("el aviso del arranque no debe impedir que la aplicación quede en marcha")
        .isTrue();
  }

  @Test
  void elArranqueNoRegistraLaCuentaQueFalta() {
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE correo_electronico = ?",
                Integer.class,
                CORREO_SIN_CUENTA))
        .isZero();
  }
}
