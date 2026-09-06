package com.moica.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.auth.EscenarioDeSeguridad;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Quién entra y quién no entra en el área administrativa.
 *
 * <p>La regla tiene dos condiciones simultáneas y aquí se recorre la tabla completa: falta el rol,
 * falta el segundo factor, falta verificarlo en esa sesión, y el único caso que sí pasa.
 */
class AreaAdministrativaIT extends EscenarioDeSeguridad {

  @Test
  void sinSesionResponde401() {
    NavegadorDePrueba anonimo = abrirNavegador();

    HttpResponse<String> respuesta = anonimo.get(RUTA_ADMIN);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("NO_AUTENTICADO");
  }

  @Test
  void conLaSesionRevocadaResponde401() {
    darRolAdministrativo(CORREO);
    iniciarSesion(navegador);
    String secreto = activarSegundoFactor(navegador);
    assertThat(navegador.get(RUTA_ADMIN).statusCode()).isEqualTo(HttpStatus.OK.value());

    jdbc.update(
        "UPDATE sesion SET fecha_revocacion = now(), motivo_revocacion = 'MEDIDA_ADMINISTRATIVA'");

    HttpResponse<String> respuesta = navegador.get(RUTA_ADMIN);
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(secreto).isNotBlank();
  }

  @Test
  void unaCuentaOrdinariaRecibe403() {
    iniciarSesion(navegador);

    HttpResponse<String> respuesta = navegador.get(RUTA_ADMIN);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("ACCESO_DENEGADO");
  }

  @Test
  void unaCuentaOrdinariaConSegundoFactorVerificadoSigueRecibiendo403() {
    iniciarSesion(navegador);
    activarSegundoFactor(navegador);

    assertThat(navegador.get(RUTA_ADMIN).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void unAdministradorSinSegundoFactorActivoRecibe403() {
    darRolAdministrativo(CORREO);
    iniciarSesion(navegador);

    HttpResponse<String> respuesta = navegador.get(RUTA_ADMIN);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("ACCESO_DENEGADO");
  }

  @Test
  void unAdministradorConSegundoFactorSinVerificarEnEsaSesionRecibe403() {
    darRolAdministrativo(CORREO);
    iniciarSesion(navegador);
    activarSegundoFactor(navegador);
    navegador.delete(RUTA_SESION);

    NavegadorDePrueba provisional = abrirNavegador();
    iniciarSesion(provisional);

    HttpResponse<String> respuesta = provisional.get(RUTA_ADMIN);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("ACCESO_DENEGADO");
  }

  @Test
  void unAdministradorConSegundoFactorVerificadoEntra() {
    darRolAdministrativo(CORREO);
    iniciarSesion(navegador);
    activarSegundoFactor(navegador);

    HttpResponse<String> respuesta = navegador.get(RUTA_ADMIN);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("correoElectronico").asText()).isEqualTo(CORREO);
    assertThat(json(respuesta).get("nombreCompleto").asText()).isEqualTo("Persona de Prueba");
    assertThat(json(respuesta).get("fechaAsignacion").asText()).isNotBlank();
  }

  @Test
  void unAdministradorVuelveAEntrarTrasVerificarSuSesionProvisional() {
    darRolAdministrativo(CORREO);
    iniciarSesion(navegador);
    String secreto = activarSegundoFactor(navegador);
    navegador.delete(RUTA_SESION);

    NavegadorDePrueba deVuelta = abrirNavegador();
    iniciarSesion(deVuelta);
    assertThat(deVuelta.get(RUTA_ADMIN).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());

    deVuelta.post(RUTA_VERIFICACION, Map.of("codigo", codigoValido(secreto)));

    assertThat(deVuelta.get(RUTA_ADMIN).statusCode()).isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void retirarElRolCierraElAreaEnLaPeticionSiguiente() {
    darRolAdministrativo(CORREO);
    iniciarSesion(navegador);
    activarSegundoFactor(navegador);
    assertThat(navegador.get(RUTA_ADMIN).statusCode()).isEqualTo(HttpStatus.OK.value());

    jdbc.update("DELETE FROM administrador");

    assertThat(navegador.get(RUTA_ADMIN).statusCode())
        .as("el rol se relee en cada petición, no viaja en el token")
        .isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void unaRutaAdministrativaInexistenteSigueExigiendoLosMismosPermisos() {
    iniciarSesion(navegador);

    assertThat(navegador.get("/api/admin/lo-que-sea").statusCode())
        .as("la protección cubre todo /api/admin, no solo el endpoint conocido")
        .isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void laRespuestaDeAccesoDenegadoNoRevelaNadaDelAreaAdministrativa() {
    iniciarSesion(navegador);

    HttpResponse<String> respuesta = navegador.get(RUTA_ADMIN);

    assertThat(respuesta.body())
        .doesNotContain("com.moica")
        .doesNotContain("SELECT")
        .doesNotContain("Exception");
    assertThat(json(respuesta).get("estado").asInt()).isEqualTo(403);
    assertThat(json(respuesta).get("ruta").asText()).isEqualTo(RUTA_ADMIN);
    assertThat(json(respuesta).has("errores")).isFalse();
  }
}
