package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * La configuración del almacenamiento: o completa, o ausente; nunca a medias.
 *
 * <p>También cubre la ida y vuelta entre clave y URL pública, de la que depende poder borrar y
 * sustituir objetos cuando la base de datos solo guarda la URL.
 */
class PropiedadesDeAlmacenamientoTest {

  private static final String SECRETO = "centinela-secreto-de-token-r2";

  @Test
  void sinNingunaVariableArrancaSinConfigurar() {
    PropiedadesDeAlmacenamiento propiedades =
        new PropiedadesDeAlmacenamiento(null, null, null, null, null);

    assertThat(propiedades.estaConfigurado()).isFalse();
  }

  @Test
  void conLasCincoVariablesQuedaConfigurado() {
    assertThat(propiedadesCompletas().estaConfigurado()).isTrue();
  }

  @Test
  void unaConfiguracionAMediasDetieneElArranque() {
    assertThatThrownBy(
            () ->
                new PropiedadesDeAlmacenamiento(
                    "cuenta", "access", null, "bucket", "https://imagenes.moica.ni"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MOICA_R2_")
        .as("el mensaje orienta sin revelar ningún valor")
        .satisfies(error -> assertThat(error.getMessage()).doesNotContain("cuenta"));
  }

  @Test
  void laBasePublicaDebeSerHttps() {
    assertThatThrownBy(
            () ->
                new PropiedadesDeAlmacenamiento(
                    "cuenta", "access", SECRETO, "bucket", "http://imagenes.moica.ni"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("https://");
  }

  @Test
  void laBaseAdmiteTantoR2DevComoUnDominioPropio() {
    assertThatCode(
            () ->
                new PropiedadesDeAlmacenamiento(
                    "cuenta", "access", SECRETO, "bucket", "https://pub-abc123.r2.dev"))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                new PropiedadesDeAlmacenamiento(
                    "cuenta", "access", SECRETO, "bucket", "https://imagenes.moica.ni/"))
        .doesNotThrowAnyException();
  }

  @Test
  void construyeLaUrlPublicaYRecuperaLaClaveDeVuelta() {
    PropiedadesDeAlmacenamiento propiedades = propiedadesCompletas();

    String url = propiedades.urlPublicaDe("perfiles/abc123.png");

    assertThat(url).isEqualTo("https://imagenes.moica.ni/perfiles/abc123.png");
    assertThat(propiedades.claveDe(url)).contains("perfiles/abc123.png");
  }

  @Test
  void unaUrlDeOtraBaseNoEntregaClave() {
    PropiedadesDeAlmacenamiento propiedades = propiedadesCompletas();

    assertThat(propiedades.claveDe("https://otro-dominio.example/perfiles/abc.png")).isEmpty();
    assertThat(propiedades.claveDe(null)).isEmpty();
    assertThat(propiedades.claveDe("https://imagenes.moica.ni/")).isEmpty();
  }

  @Test
  void laBarraFinalDeLaBaseNoDuplicaSeparadores() {
    PropiedadesDeAlmacenamiento propiedades =
        new PropiedadesDeAlmacenamiento(
            "cuenta", "access", SECRETO, "bucket", "https://imagenes.moica.ni/");

    assertThat(propiedades.urlPublicaDe("perfiles/abc.png"))
        .isEqualTo("https://imagenes.moica.ni/perfiles/abc.png");
  }

  private static PropiedadesDeAlmacenamiento propiedadesCompletas() {
    return new PropiedadesDeAlmacenamiento(
        "cuenta-de-prueba", "access-key", SECRETO, "moica-publico", "https://imagenes.moica.ni");
  }
}
