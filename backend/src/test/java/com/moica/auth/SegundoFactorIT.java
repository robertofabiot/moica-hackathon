package com.moica.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.auth.seguridad.CifradoDeSecretos;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/** Configuración del segundo factor: registro del secreto, activación y desactivación. */
class SegundoFactorIT extends EscenarioDeSeguridad {

  @Autowired private CifradoDeSecretos cifrado;

  @BeforeEach
  void iniciarSesionAntesDeConfigurar() {
    iniciarSesion(navegador);
  }

  @Test
  void unaCuentaNuevaNoTieneSegundoFactor() {
    HttpResponse<String> respuesta = navegador.get(RUTA_SEGUNDO_FACTOR);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("estado").isNull()).isTrue();
    assertThat(json(respuesta).get("obligatorio").asBoolean()).isFalse();
  }

  @Test
  void iniciarLaActivacionEntregaLaClaveManualYLaUriDeConfiguracion() {
    HttpResponse<String> respuesta = navegador.post(RUTA_SEGUNDO_FACTOR, Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());

    String secreto = json(respuesta).get("claveManual").asText();
    assertThat(secreto).hasSize(32).matches("[A-Z2-7]+");
    assertThat(json(respuesta).get("uriDeConfiguracion").asText())
        .startsWith("otpauth://totp/")
        .contains("secret=" + secreto);
    assertThat(json(respuesta).get("digitos").asInt()).isEqualTo(6);
    assertThat(json(respuesta).get("periodoEnSegundos").asLong()).isEqualTo(30);
    assertThat(estadoGuardado()).isEqualTo("PENDIENTE_ACTIVACION");
  }

  @Test
  void laRespuestaQueLlevaElSecretoPideQueNoSeGuardeEnNingunaCache() {
    HttpResponse<String> respuesta = navegador.post(RUTA_SEGUNDO_FACTOR, Map.of());

    assertThat(respuesta.headers().firstValue("Cache-Control"))
        .as(
            "es la única respuesta con el secreto: ni el navegador ni un intermediario debe copiarla")
        .hasValueSatisfying(cabecera -> assertThat(cabecera).contains("no-store"));
  }

  @Test
  void guardaElSecretoCifradoYNuncaEnClaro() {
    String secreto =
        json(navegador.post(RUTA_SEGUNDO_FACTOR, Map.of())).get("claveManual").asText();

    String guardado = secretoGuardado();

    assertThat(guardado).isNotEqualTo(secreto).doesNotContain(secreto);
    assertThat(cifrado.descifrar(guardado))
        .as("solo la clave de cifrado del entorno recupera el secreto")
        .isEqualTo(secreto);
  }

  @Test
  void elPrimerCodigoValidoDejaElSegundoFactorActivo() {
    String secreto =
        json(navegador.post(RUTA_SEGUNDO_FACTOR, Map.of())).get("claveManual").asText();

    HttpResponse<String> confirmacion =
        navegador.post(
            RUTA_SEGUNDO_FACTOR + "/activacion", Map.of("codigo", codigoValido(secreto)));

    assertThat(confirmacion.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(confirmacion).get("estado").asText()).isEqualTo("ACTIVO");
    assertThat(estadoGuardado()).isEqualTo("ACTIVO");
    assertThat(fechaGuardada("fecha_activacion")).isNotNull();
    assertThat(fechaGuardada("fecha_ultima_verificacion")).isNotNull();
  }

  @Test
  void activarloDejaVerificadaLaSesionDesdeLaQueSeActivo() {
    activarSegundoFactor(navegador);

    HttpResponse<String> sesion = navegador.get(RUTA_SESION);

    assertThat(json(sesion).path("sesion").get("segundoFactorVerificado").asBoolean()).isTrue();
    assertThat(json(sesion).path("sesion").get("pendienteDeSegundoFactor").asBoolean()).isFalse();
    assertThat(jdbc.queryForObject("SELECT segundo_factor_verificado FROM sesion", Boolean.class))
        .isTrue();
  }

  @Test
  void rechazaLaConfirmacionConUnCodigoIncorrecto() {
    navegador.post(RUTA_SEGUNDO_FACTOR, Map.of());

    HttpResponse<String> respuesta =
        navegador.post(RUTA_SEGUNDO_FACTOR + "/activacion", Map.of("codigo", "000000"));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CODIGO_INVALIDO");
    assertThat(estadoGuardado()).isEqualTo("PENDIENTE_ACTIVACION");
  }

  @Test
  void rechazaLaConfirmacionConUnCodigoDeOtroMomento() {
    String secreto =
        json(navegador.post(RUTA_SEGUNDO_FACTOR, Map.of())).get("claveManual").asText();

    HttpResponse<String> respuesta =
        navegador.post(
            RUTA_SEGUNDO_FACTOR + "/activacion", Map.of("codigo", codigoDeOtroMomento(secreto)));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CODIGO_INVALIDO");
    assertThat(estadoGuardado()).isEqualTo("PENDIENTE_ACTIVACION");
  }

  @Test
  void volverAEmpezarLaActivacionSustituyeElSecretoPendiente() {
    String abandonado =
        json(navegador.post(RUTA_SEGUNDO_FACTOR, Map.of())).get("claveManual").asText();
    String vigente =
        json(navegador.post(RUTA_SEGUNDO_FACTOR, Map.of())).get("claveManual").asText();

    assertThat(vigente).isNotEqualTo(abandonado);

    HttpResponse<String> conElAbandonado =
        navegador.post(
            RUTA_SEGUNDO_FACTOR + "/activacion", Map.of("codigo", codigoValido(abandonado)));
    assertThat(conElAbandonado.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());

    HttpResponse<String> conElVigente =
        navegador.post(
            RUTA_SEGUNDO_FACTOR + "/activacion", Map.of("codigo", codigoValido(vigente)));
    assertThat(conElVigente.statusCode()).isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void noEntregaElSecretoUnaVezActivado() {
    activarSegundoFactor(navegador);

    HttpResponse<String> consulta = navegador.get(RUTA_SEGUNDO_FACTOR);

    assertThat(consulta.body()).doesNotContain("claveManual").doesNotContain("otpauth");
    assertThat(json(consulta).get("estado").asText()).isEqualTo("ACTIVO");
  }

  @Test
  void noPermiteReconfigurarloSinDesactivarloAntes() {
    activarSegundoFactor(navegador);

    HttpResponse<String> respuesta = navegador.post(RUTA_SEGUNDO_FACTOR, Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SEGUNDO_FACTOR_YA_ACTIVO");
  }

  @Test
  void confirmarSinActivacionPendienteEsUnConflicto() {
    activarSegundoFactor(navegador);

    HttpResponse<String> respuesta =
        navegador.post(RUTA_SEGUNDO_FACTOR + "/activacion", Map.of("codigo", "123456"));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SEGUNDO_FACTOR_SIN_ACTIVACION_PENDIENTE");
  }

  @Test
  void desactivarloExigeContrasenaYCodigoYRevocaTodasLasSesiones() {
    String secreto = activarSegundoFactor(navegador);

    HttpResponse<String> respuesta = desactivar(CLAVE, codigoValido(secreto));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(estadoGuardado()).isEqualTo("DESACTIVADO");
    assertThat(sesionesVigentes()).isZero();
    assertThat(jdbc.queryForObject("SELECT motivo_revocacion FROM sesion", String.class))
        .isEqualTo("CAMBIO_CREDENCIALES");
    assertThat(navegador.cookie(COOKIE_SESION)).isEmpty();
  }

  @Test
  void noDesactivaConLaContrasenaEquivocada() {
    String secreto = activarSegundoFactor(navegador);

    HttpResponse<String> respuesta = desactivar("Moica2026$equivocada", codigoValido(secreto));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CREDENCIALES_INVALIDAS");
    assertThat(estadoGuardado()).isEqualTo("ACTIVO");
    assertThat(sesionesVigentes()).isEqualTo(1);
  }

  @Test
  void noDesactivaConUnCodigoEquivocado() {
    activarSegundoFactor(navegador);

    HttpResponse<String> respuesta = desactivar(CLAVE, "000000");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CODIGO_INVALIDO");
    assertThat(estadoGuardado()).isEqualTo("ACTIVO");
    assertThat(sesionesVigentes()).isEqualTo(1);
  }

  @Test
  void noDesactivaLoQueNoEstabaActivo() {
    navegador.post(RUTA_SEGUNDO_FACTOR, Map.of());

    HttpResponse<String> respuesta = desactivar(CLAVE, "123456");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SEGUNDO_FACTOR_NO_ACTIVO");
  }

  @Test
  void reactivarloDespuesDeDesactivarloGeneraUnSecretoNuevo() {
    String primerSecreto = activarSegundoFactor(navegador);
    desactivar(CLAVE, codigoValido(primerSecreto));

    NavegadorDePrueba deNuevo = abrirNavegador();
    iniciarSesion(deNuevo);
    String segundoSecreto = activarSegundoFactor(deNuevo);

    assertThat(segundoSecreto).isNotEqualTo(primerSecreto);
    assertThat(estadoGuardado()).isEqualTo("ACTIVO");
  }

  @Test
  void unaCuentaAdministradoraNoPuedeDesactivarSuSegundoFactor() {
    String secreto = activarSegundoFactor(navegador);
    darRolAdministrativo(CORREO);

    HttpResponse<String> respuesta = desactivar(CLAVE, codigoValido(secreto));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SEGUNDO_FACTOR_OBLIGATORIO");
    assertThat(estadoGuardado()).isEqualTo("ACTIVO");
    assertThat(sesionesVigentes()).isEqualTo(1);
  }

  @Test
  void alRolAdministrativoLeConstaQueEsObligatorio() {
    darRolAdministrativo(CORREO);

    assertThat(json(navegador.get(RUTA_SEGUNDO_FACTOR)).get("obligatorio").asBoolean()).isTrue();
  }

  @Test
  void ningunaRespuestaDelCicloDeSegundoFactorLlevaElSecretoGuardado() {
    String secreto = activarSegundoFactor(navegador);

    assertThat(navegador.get(RUTA_SEGUNDO_FACTOR).body()).doesNotContain(secreto);
    assertThat(navegador.get(RUTA_SESION).body()).doesNotContain(secreto);
    assertThat(navegador.get(RUTA_SEGUNDO_FACTOR).body()).doesNotContain(secretoGuardado());
  }

  @Test
  void noConfiguraElSegundoFactorSinElTokenCsrf() {
    HttpResponse<String> respuesta = navegador.postSinTokenCsrf(RUTA_SEGUNDO_FACTOR, Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(cantidadDeSegundosFactores()).isZero();
  }

  @Test
  void nadieSinSesionPuedeTocarElSegundoFactor() {
    NavegadorDePrueba anonimo = abrirNavegador();

    assertThat(anonimo.get(RUTA_SEGUNDO_FACTOR).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(anonimo.post(RUTA_SEGUNDO_FACTOR, Map.of()).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  private HttpResponse<String> desactivar(String claveActual, String codigo) {
    return navegador.post(
        RUTA_SEGUNDO_FACTOR + "/desactivacion",
        Map.of("claveActual", claveActual, "codigo", codigo));
  }

  private String estadoGuardado() {
    return jdbc.queryForObject(
        "SELECT estado_segundo_factor FROM segundo_factor_usuario", String.class);
  }

  private String secretoGuardado() {
    return jdbc.queryForObject("SELECT secreto_totp FROM segundo_factor_usuario", String.class);
  }

  private OffsetDateTime fechaGuardada(String columna) {
    // La columna la elige esta prueba, nunca un dato de entrada.
    return jdbc.queryForObject(
        "SELECT " + columna + " FROM segundo_factor_usuario", OffsetDateTime.class);
  }

  private Integer cantidadDeSegundosFactores() {
    return jdbc.queryForObject("SELECT count(*) FROM segundo_factor_usuario", Integer.class);
  }
}
