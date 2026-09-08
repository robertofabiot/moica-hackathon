package com.moica.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.PruebaDeIntegracionConPostgres;
import com.moica.demo.service.DemoService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/** Comprueba el ApplicationRunner real y la transacción del bean gestionado por Spring. */
@TestPropertySource(properties = "MOICA_SEED_DEMO_ENABLED=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ArranqueDeDemoIT extends PruebaDeIntegracionConPostgres {
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DemoService demo;

  @Test
  void arranqueActivadoYaConfirmoLosDatosYReejecutarNoDuplica() {
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid'",
                Integer.class))
        .isEqualTo(6);
    var resumen = demo.sincronizar();
    assertThat(resumen.usuariosCreados()).isZero();
    assertThat(resumen.perfilesCreados()).isZero();
    assertThat(resumen.serviciosCreados()).isZero();
    assertThat(resumen.imagenesCreadas()).isZero();
    assertThat(resumen.serviciosExistentes()).isEqualTo(9);
  }

  @AfterAll
  static void retirarSoloLaDemoDeEsteArranque(@Autowired JdbcTemplate jdbc) {
    jdbc.update(
        "DELETE FROM servicio_publicado WHERE id_prestador IN (SELECT id_usuario FROM usuario WHERE correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid')");
    jdbc.update(
        "DELETE FROM usuario WHERE correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid'");
  }
}
