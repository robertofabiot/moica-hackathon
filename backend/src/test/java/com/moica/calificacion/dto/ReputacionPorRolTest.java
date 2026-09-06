package com.moica.calificacion.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.calificacion.dto.ReputacionPorRol.TramoDeReputacion;
import com.moica.calificacion.entity.RolCalificado;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Cómo se presenta un agregado de reputación.
 *
 * <p>Lo que se comprueba aquí es la aritmética y la forma —promedio nulo, redondeo a un decimal y
 * las cinco filas del desglose—. Que las cifras salgan de las calificaciones reales lo demuestra
 * {@code ReputacionPorRolIT}.
 */
class ReputacionPorRolTest {

  @Test
  void sinCalificacionesElPromedioEsNuloYNoCero() {
    ReputacionPorRol reputacion = ReputacionPorRol.sinCalificaciones(RolCalificado.PRESTADOR);

    assertThat(reputacion.promedio()).isNull();
    assertThat(reputacion.cantidad()).isZero();
    assertThat(reputacion.rol()).isEqualTo(RolCalificado.PRESTADOR);
  }

  @Test
  void elDesgloseLlevaSiempreLasCincoPuntuacionesDeMayorAMenor() {
    ReputacionPorRol vacia = ReputacionPorRol.sinCalificaciones(RolCalificado.CLIENTE);

    assertThat(vacia.desglose())
        .extracting(TramoDeReputacion::estrellas)
        .containsExactly((short) 5, (short) 4, (short) 3, (short) 2, (short) 1);
    assertThat(vacia.desglose()).allSatisfy(tramo -> assertThat(tramo.cantidad()).isZero());
  }

  @Test
  void redondeaElPromedioAUnDecimal() {
    ReputacionPorRol reputacion =
        ReputacionPorRol.de(RolCalificado.PRESTADOR, agregado(13.0 / 3, 3L, 0, 0, 0, 2, 1));

    assertThat(reputacion.promedio()).isEqualByComparingTo(new BigDecimal("4.3"));
    assertThat(reputacion.promedio().scale()).isEqualTo(1);
  }

  @Test
  void unPromedioEnteroConservaSuDecimal() {
    ReputacionPorRol reputacion =
        ReputacionPorRol.de(RolCalificado.PRESTADOR, agregado(5.0, 2L, 0, 0, 0, 0, 2));

    assertThat(reputacion.promedio()).isEqualTo(new BigDecimal("5.0"));
  }

  @Test
  void reparteElDesgloseEnLaFilaQueCorresponde() {
    ReputacionPorRol reputacion =
        ReputacionPorRol.de(RolCalificado.CLIENTE, agregado(3.0, 15L, 1, 2, 3, 4, 5));

    assertThat(reputacion.cantidad()).isEqualTo(15L);
    assertThat(reputacion.desglose())
        .extracting(TramoDeReputacion::cantidad)
        .containsExactly(5L, 4L, 3L, 2L, 1L);
  }

  @Test
  void unAgregadoSinFilasSePresentaComoSinCalificaciones() {
    ReputacionPorRol reputacion =
        ReputacionPorRol.de(RolCalificado.PRESTADOR, agregado(0.0, 0L, 0, 0, 0, 0, 0));

    assertThat(reputacion.promedio()).isNull();
    assertThat(reputacion.cantidad()).isZero();
  }

  private static AgregadoDeCalificaciones agregado(
      double promedio, long cantidad, long una, long dos, long tres, long cuatro, long cinco) {
    return new AgregadoDeCalificaciones(7L, promedio, cantidad, una, dos, tres, cuatro, cinco);
  }
}
