package com.moica.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * Lo que puede y lo que no puede hacer una sesión a la que le falta el segundo factor.
 *
 * <p>La distinción importante: {@code segundoFactorVerificado = false} no significa «provisional».
 * Solo lo es cuando la cuenta además tiene el segundo factor activo. Quien no lo usa opera con
 * normalidad desde el primer momento.
 */
class SesionProvisionalIT extends EscenarioDeSeguridad {

  @Test
  void unaCuentaSinSegundoFactorUsaSuSesionConNormalidad() {
    HttpResponse<String> inicio = iniciarSesion(navegador);

    JsonNode sesion = json(inicio).path("sesion");
    assertThat(sesion.get("segundoFactorRequerido").asBoolean()).isFalse();
    assertThat(sesion.get("segundoFactorVerificado").asBoolean()).isFalse();
    assertThat(sesion.get("pendienteDeSegundoFactor").asBoolean())
        .as("sin segundo factor configurado, la sesión está completa")
        .isFalse();

    assertThat(navegador.get(RUTA_SEGUNDO_FACTOR).statusCode()).isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void conSegundoFactorActivoElLoginAbreUnaSesionQueLoAnunciaPendiente() {
    conSegundoFactorActivo();

    NavegadorDePrueba otro = abrirNavegador();
    HttpResponse<String> inicio = iniciarSesion(otro);

    assertThat(inicio.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    JsonNode sesion = json(inicio).path("sesion");
    assertThat(sesion.get("segundoFactorRequerido").asBoolean()).isTrue();
    assertThat(sesion.get("segundoFactorVerificado").asBoolean()).isFalse();
    assertThat(sesion.get("pendienteDeSegundoFactor").asBoolean()).isTrue();
    assertThat(otro.cookie(COOKIE_SESION)).isPresent();
  }

  @Test
  void unaSesionProvisionalPuedeConsultarseAunqueNoPuedaOperar() {
    conSegundoFactorActivo();
    NavegadorDePrueba provisional = sesionProvisional();

    HttpResponse<String> consulta = provisional.get(RUTA_SESION);

    assertThat(consulta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(consulta).path("sesion").get("pendienteDeSegundoFactor").asBoolean()).isTrue();
  }

  @Test
  void unaSesionProvisionalNoAlcanzaNingunaOtraRutaProtegida() {
    conSegundoFactorActivo();
    NavegadorDePrueba provisional = sesionProvisional();

    HttpResponse<String> consultaDelSegundoFactor = provisional.get(RUTA_SEGUNDO_FACTOR);
    assertThat(consultaDelSegundoFactor.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(consultaDelSegundoFactor)).isEqualTo("ACCESO_DENEGADO");

    assertThat(
            provisional
                .put(RUTA_CLAVE, Map.of("claveActual", CLAVE, "claveNueva", "Moica2026$otra"))
                .statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(provisional.get(RUTA_ADMIN).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void unaSesionProvisionalSiPuedeCerrarse() {
    conSegundoFactorActivo();
    NavegadorDePrueba provisional = sesionProvisional();

    assertThat(provisional.delete(RUTA_SESION).statusCode())
        .isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(provisional.get(RUTA_SESION).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void unCodigoValidoCompletaLaSesionProvisional() {
    String secreto = conSegundoFactorActivo();
    NavegadorDePrueba provisional = sesionProvisional();

    HttpResponse<String> verificacion =
        provisional.post(RUTA_VERIFICACION, Map.of("codigo", codigoValido(secreto)));

    assertThat(verificacion.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(verificacion).path("sesion").get("segundoFactorVerificado").asBoolean())
        .isTrue();
    assertThat(json(verificacion).path("sesion").get("pendienteDeSegundoFactor").asBoolean())
        .isFalse();
    assertThat(provisional.get(RUTA_SEGUNDO_FACTOR).statusCode()).isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void aceptaElCodigoConLosEspaciosConLosQueLoMuestraLaAplicacionAutenticadora() {
    String secreto = conSegundoFactorActivo();
    NavegadorDePrueba provisional = sesionProvisional();

    String conEspacios = codigoValido(secreto).replaceAll("(\\d{3})(\\d{3})", "$1 $2");

    assertThat(provisional.post(RUTA_VERIFICACION, Map.of("codigo", conEspacios)).statusCode())
        .isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void unCodigoIncorrectoNoCompletaLaSesion() {
    conSegundoFactorActivo();
    NavegadorDePrueba provisional = sesionProvisional();

    HttpResponse<String> verificacion =
        provisional.post(RUTA_VERIFICACION, Map.of("codigo", "000000"));

    assertThat(verificacion.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(verificacion)).isEqualTo("CODIGO_INVALIDO");
    assertThat(provisional.get(RUTA_SEGUNDO_FACTOR).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void unCodigoFueraDeLaToleranciaNoCompletaLaSesion() {
    String secreto = conSegundoFactorActivo();
    NavegadorDePrueba provisional = sesionProvisional();

    HttpResponse<String> verificacion =
        provisional.post(RUTA_VERIFICACION, Map.of("codigo", codigoDeOtroMomento(secreto)));

    assertThat(verificacion.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(verificacion)).isEqualTo("CODIGO_INVALIDO");
  }

  @Test
  void verificarUnaSesionNoCompletaLasDemas() {
    String secreto = conSegundoFactorActivo();
    NavegadorDePrueba unDispositivo = sesionProvisional();
    NavegadorDePrueba otroDispositivo = sesionProvisional();

    unDispositivo.post(RUTA_VERIFICACION, Map.of("codigo", codigoValido(secreto)));

    assertThat(unDispositivo.get(RUTA_SEGUNDO_FACTOR).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(otroDispositivo.get(RUTA_SEGUNDO_FACTOR).statusCode())
        .as("cada sesión presenta su propio código")
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(
            jdbc.queryForObject(
                """
                SELECT count(*) FROM sesion
                WHERE segundo_factor_verificado AND fecha_revocacion IS NULL
                """,
                Integer.class))
        .as("de las dos sesiones vigentes, solo la que presentó su código quedó verificada")
        .isEqualTo(1);
  }

  @Test
  void noSeVerificaNadaSinUnaSesion() {
    conSegundoFactorActivo();
    NavegadorDePrueba anonimo = abrirNavegador();

    HttpResponse<String> respuesta = anonimo.post(RUTA_VERIFICACION, Map.of("codigo", "123456"));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("NO_AUTENTICADO");
  }

  @Test
  void verificarSinSegundoFactorActivoEsUnConflicto() {
    NavegadorDePrueba sesion = abrirNavegador();
    iniciarSesion(sesion);

    HttpResponse<String> respuesta = sesion.post(RUTA_VERIFICACION, Map.of("codigo", "123456"));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SEGUNDO_FACTOR_NO_ACTIVO");
  }

  @Test
  void noVerificaElSegundoFactorSinElTokenCsrf() {
    String secreto = conSegundoFactorActivo();
    NavegadorDePrueba provisional = sesionProvisional();

    HttpResponse<String> respuesta =
        provisional.postSinTokenCsrf(RUTA_VERIFICACION, Map.of("codigo", codigoValido(secreto)));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(provisional.get(RUTA_SEGUNDO_FACTOR).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void ningunaRespuestaDeLaVerificacionLlevaSecretosNiTrazas() {
    String secreto = conSegundoFactorActivo();
    NavegadorDePrueba provisional = sesionProvisional();

    HttpResponse<String> fallo = provisional.post(RUTA_VERIFICACION, Map.of("codigo", "000000"));

    assertThat(fallo.body())
        .doesNotContain(secreto)
        .doesNotContain("com.moica")
        .doesNotContain("SELECT")
        .doesNotContain("Exception");
    assertThat(json(fallo).has("errores")).isFalse();
    assertThat(json(fallo).get("ruta").asText()).isEqualTo(RUTA_VERIFICACION);
  }

  /** Deja la cuenta del escenario con el segundo factor activo y devuelve su secreto. */
  private String conSegundoFactorActivo() {
    NavegadorDePrueba configuracion = abrirNavegador();
    iniciarSesion(configuracion);
    String secreto = activarSegundoFactor(configuracion);
    configuracion.delete(RUTA_SESION);
    return secreto;
  }

  /** Un navegador nuevo que acaba de iniciar sesión y todavía no ha presentado su código. */
  private NavegadorDePrueba sesionProvisional() {
    NavegadorDePrueba provisional = abrirNavegador();
    iniciarSesion(provisional);
    return provisional;
  }
}
