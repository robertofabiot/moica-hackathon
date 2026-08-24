package com.moica.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** El recorrido del segundo factor por sus tres estados. */
class SegundoFactorUsuarioTest {

  private static final OffsetDateTime INSTANTE = OffsetDateTime.parse("2026-08-24T10:00:00-06:00");
  private static final String SECRETO_CIFRADO = "secreto-ya-cifrado-de-prueba";

  @Test
  void unSegundoFactorReciennRegistradoEstaPendienteDeActivacion() {
    SegundoFactorUsuario segundoFactor = registrado();

    assertThat(segundoFactor.getEstadoSegundoFactor())
        .isEqualTo(EstadoSegundoFactor.PENDIENTE_ACTIVACION);
    assertThat(segundoFactor.estaPendienteDeActivacion()).isTrue();
    assertThat(segundoFactor.estaActivo()).isFalse();
    assertThat(segundoFactor.getFechaActivacion()).isNull();
    assertThat(segundoFactor.getFechaUltimaVerificacion()).isNull();
  }

  @Test
  void elPrimerCodigoValidoLoDejaActivoConSuFecha() {
    SegundoFactorUsuario segundoFactor = registrado();

    segundoFactor.activar(INSTANTE);

    assertThat(segundoFactor.estaActivo()).isTrue();
    assertThat(segundoFactor.estaPendienteDeActivacion()).isFalse();
    assertThat(segundoFactor.getFechaActivacion()).isEqualTo(INSTANTE);
    assertThat(segundoFactor.getFechaUltimaVerificacion()).isEqualTo(INSTANTE);
  }

  @Test
  void desactivarloConservaLaFechaDeActivacionComoEvidencia() {
    SegundoFactorUsuario segundoFactor = registrado();
    segundoFactor.activar(INSTANTE);

    segundoFactor.desactivar();

    assertThat(segundoFactor.getEstadoSegundoFactor()).isEqualTo(EstadoSegundoFactor.DESACTIVADO);
    assertThat(segundoFactor.estaActivo()).isFalse();
    assertThat(segundoFactor.getFechaActivacion())
        .as("desactivar no borra que llegó a estar activo")
        .isEqualTo(INSTANTE);
  }

  @Test
  void reactivarloEmpiezaDeCeroConOtroSecreto() {
    SegundoFactorUsuario segundoFactor = registrado();
    segundoFactor.activar(INSTANTE);
    segundoFactor.desactivar();

    segundoFactor.reiniciarActivacion("otro-secreto-ya-cifrado");

    assertThat(segundoFactor.getSecretoCifrado())
        .as("el secreto anterior pudo quedarse en un teléfono que ya no se controla")
        .isEqualTo("otro-secreto-ya-cifrado");
    assertThat(segundoFactor.estaPendienteDeActivacion()).isTrue();
    assertThat(segundoFactor.getFechaActivacion()).isNull();
    assertThat(segundoFactor.getFechaUltimaVerificacion()).isNull();
  }

  @Test
  void cadaVerificacionCorrectaActualizaSuInstante() {
    SegundoFactorUsuario segundoFactor = registrado();
    segundoFactor.activar(INSTANTE);

    segundoFactor.registrarVerificacion(INSTANTE.plusDays(3));

    assertThat(segundoFactor.getFechaUltimaVerificacion()).isEqualTo(INSTANTE.plusDays(3));
    assertThat(segundoFactor.getFechaActivacion()).isEqualTo(INSTANTE);
  }

  private static SegundoFactorUsuario registrado() {
    return new SegundoFactorUsuario(1L, SECRETO_CIFRADO);
  }
}
