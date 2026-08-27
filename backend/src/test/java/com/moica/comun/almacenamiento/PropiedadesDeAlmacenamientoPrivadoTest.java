package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * La configuración del bucket privado: o completa, o ausente; nunca a medias.
 *
 * <p>Y, sobre todo, que ni el secreto ni el identificador del token salgan en un mensaje de
 * arranque o en una representación: los dos son material de credencial.
 */
class PropiedadesDeAlmacenamientoPrivadoTest {

  private static final String SECRETO = "centinela-secreto-de-token-privado";
  private static final String IDENTIFICADOR = "centinela-access-key-privada";

  @Test
  void sinNingunaVariableArrancaSinConfigurar() {
    assertThat(new PropiedadesDeAlmacenamientoPrivado(null, null, null, null).estaConfigurado())
        .isFalse();
  }

  @Test
  void conLasCuatroVariablesQuedaConfigurado() {
    assertThat(propiedadesCompletas().estaConfigurado()).isTrue();
  }

  @Test
  void unaConfiguracionAMediasDetieneElArranque() {
    assertThatThrownBy(
            () -> new PropiedadesDeAlmacenamientoPrivado("cuenta", IDENTIFICADOR, null, "privado"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MOICA_R2_PRIVADO_")
        .satisfies(
            error ->
                assertThat(error.getMessage())
                    .as("el mensaje orienta sin revelar ningún valor")
                    .doesNotContain(IDENTIFICADOR)
                    .doesNotContain("cuenta"));
  }

  @Test
  void recortaLosEspaciosDeCadaValor() {
    PropiedadesDeAlmacenamientoPrivado propiedades =
        new PropiedadesDeAlmacenamientoPrivado(
            "  cuenta  ", "  " + IDENTIFICADOR + "  ", "  " + SECRETO + "  ", "  privado  ");

    assertThat(propiedades.idCuenta()).isEqualTo("cuenta");
    assertThat(propiedades.bucketPrivado()).isEqualTo("privado");
  }

  @Test
  void laRepresentacionOcultaLasDosMitadesDeLaCredencial() {
    String representacion = propiedadesCompletas().toString();

    assertThat(representacion).doesNotContain(SECRETO).doesNotContain(IDENTIFICADOR);
    assertThat(representacion)
        .as("lo que no es credencial sigue a la vista para diagnosticar un despliegue")
        .contains("cuenta-privada")
        .contains("moica-privado");
  }

  private static PropiedadesDeAlmacenamientoPrivado propiedadesCompletas() {
    return new PropiedadesDeAlmacenamientoPrivado(
        "cuenta-privada", IDENTIFICADOR, SECRETO, "moica-privado");
  }
}
