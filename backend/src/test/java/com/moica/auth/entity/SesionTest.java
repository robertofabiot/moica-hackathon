package com.moica.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** La vigencia de una sesión, que es lo que decide si un token concede acceso. */
class SesionTest {

  private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-08-21T10:00:00-06:00");

  @Test
  void unaSesionReciennAbiertaEstaVigente() {
    Sesion sesion = sesionDeSieteDias();

    assertThat(sesion.estaVigente(INICIO.plusDays(3))).isTrue();
  }

  @Test
  void unaSesionDejaDeValerAlLlegarSuExpiracion() {
    Sesion sesion = sesionDeSieteDias();

    assertThat(sesion.estaVigente(INICIO.plusDays(7))).isFalse();
    assertThat(sesion.estaVigente(INICIO.plusDays(8))).isFalse();
  }

  @Test
  void unaSesionRevocadaNoValeAunqueLeQuedeTiempo() {
    Sesion sesion = sesionDeSieteDias();

    sesion.revocar(INICIO.plusDays(1), MotivoRevocacionSesion.CIERRE_VOLUNTARIO);

    assertThat(sesion.estaVigente(INICIO.plusDays(2))).isFalse();
    assertThat(sesion.getFechaRevocacion()).isEqualTo(INICIO.plusDays(1));
    assertThat(sesion.getMotivoRevocacion()).isEqualTo(MotivoRevocacionSesion.CIERRE_VOLUNTARIO);
  }

  @Test
  void unaSesionNaceSinElSegundoFactorVerificado() {
    assertThat(sesionDeSieteDias().isSegundoFactorVerificado()).isFalse();
  }

  private static Sesion sesionDeSieteDias() {
    return new Sesion(1L, "identificador-de-prueba", INICIO, INICIO.plusDays(7));
  }
}
