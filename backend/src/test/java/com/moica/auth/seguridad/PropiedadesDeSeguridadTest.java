package com.moica.auth.seguridad;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * La configuración de seguridad se comprueba al arrancar.
 *
 * <p>Un secreto corto o una duración absurda deben impedir el arranque, no descubrirse el día que
 * alguien intente iniciar sesión.
 */
class PropiedadesDeSeguridadTest {

  private static final String SECRETO_SUFICIENTE =
      "secreto-de-al-menos-treinta-y-dos-bytes-para-hmac";

  @Test
  void aceptaUnSecretoDeAlMenosTreintaYDosBytes() {
    assertThatCode(() -> new PropiedadesDeSeguridad(SECRETO_SUFICIENTE, Duration.ofDays(7), false))
        .doesNotThrowAnyException();
  }

  @Test
  void rechazaUnSecretoDemasiadoCorto() {
    assertThatThrownBy(() -> new PropiedadesDeSeguridad("corto", Duration.ofDays(7), false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MOICA_JWT_SECRETO");
  }

  @Test
  void rechazaLaAusenciaDeSecreto() {
    assertThatThrownBy(() -> new PropiedadesDeSeguridad(null, Duration.ofDays(7), false))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void noRevelaElSecretoEnElMensajeDeError() {
    assertThatThrownBy(() -> new PropiedadesDeSeguridad("clave-corta", Duration.ofDays(7), false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageNotContaining("clave-corta");
  }

  @Test
  void rechazaUnaDuracionDeSesionQueNoAvanza() {
    assertThatThrownBy(() -> new PropiedadesDeSeguridad(SECRETO_SUFICIENTE, Duration.ZERO, false))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> new PropiedadesDeSeguridad(SECRETO_SUFICIENTE, Duration.ofDays(-1), false))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new PropiedadesDeSeguridad(SECRETO_SUFICIENTE, null, false))
        .isInstanceOf(IllegalStateException.class);
  }
}
