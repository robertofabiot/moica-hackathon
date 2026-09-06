package com.moica.comun.configuracion;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moica.auth.seguridad.PropiedadesDeSeguridad;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConfiguracionDeProduccionTest {

  @Test
  void aceptaCookieSeguraYSoporteConfigurado() {
    assertThatCode(
            () -> new ConfiguracionDeProduccion(seguridad(true), soporte("demo@example.org")))
        .doesNotThrowAnyException();
  }

  @Test
  void rechazaCookieInsegura() {
    assertThatThrownBy(
            () -> new ConfiguracionDeProduccion(seguridad(false), soporte("demo@example.org")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MOICA_COOKIE_SEGURA");
  }

  @Test
  void rechazaSoporteDeEjemplo() {
    assertThatThrownBy(
            () -> new ConfiguracionDeProduccion(seguridad(true), soporte(" SOPORTE@moica.ni ")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MOICA_SOPORTE_CANAL");
  }

  private PropiedadesDeSeguridad seguridad(boolean segura) {
    return new PropiedadesDeSeguridad("x".repeat(32), Duration.ofDays(7), segura);
  }

  private PropiedadesDeSoporte soporte(String canal) {
    return new PropiedadesDeSoporte(canal);
  }
}
