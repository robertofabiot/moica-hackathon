package com.moica.prestador;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * El ciclo del perfil propio: crearlo, consultarlo, actualizarlo y cambiar su disponibilidad.
 *
 * <p>Cubre las reglas que el diccionario y la definición encargan a la aplicación: un solo perfil
 * por cuenta, municipio de un departamento habilitado, nacimiento {@code DISPONIBLE} y {@code
 * SIN_VERIFICAR}, nivel de verificación fuera del alcance del propietario y mutaciones cerradas
 * para cuentas que no están activas.
 */
class PerfilPrestadorIT extends EscenarioDePrestador {

  @Test
  void creaElPerfilQueNaceDisponibleYSinVerificar() {
    HttpResponse<String> respuesta = navegador.post(RUTA_PERFIL, solicitudDePerfil());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());

    JsonNode perfil = json(respuesta);
    assertThat(perfil.get("nombrePublico").asText()).isEqualTo("Taller La Esperanza");
    assertThat(perfil.get("disponibilidad").asText()).isEqualTo("DISPONIBLE");
    assertThat(perfil.get("nivelVerificacion").asText()).isEqualTo("SIN_VERIFICAR");
    assertThat(perfil.get("municipioPrincipal").get("nombreMunicipio").asText())
        .isEqualTo("Managua");
    assertThat(perfil.get("municipioPrincipal").get("nombreDepartamento").asText())
        .isEqualTo("Managua");
    assertThat(perfil.get("urlImagenPerfil").isNull()).isTrue();

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM perfil_prestador WHERE nivel_verificacion = 'SIN_VERIFICAR'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void rechazaUnSegundoPerfilParaLaMismaCuenta() {
    crearPerfil();

    HttpResponse<String> respuesta = navegador.post(RUTA_PERFIL, solicitudDePerfil());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("PERFIL_YA_EXISTE");
  }

  @Test
  void sinPerfilLaConsultaDiceQueNoExiste() {
    HttpResponse<String> respuesta = navegador.get(RUTA_PERFIL);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("PERFIL_NO_ENCONTRADO");
  }

  @Test
  void actualizaLosDatosEditablesYMueveLaFechaDeActualizacion() {
    crearPerfil();

    Map<String, Object> cambios = new HashMap<>(solicitudDePerfil());
    cambios.put("nombrePublico", "Taller La Esperanza y Familia");
    cambios.put("tipoPrestador", "EMPRENDIMIENTO");

    HttpResponse<String> respuesta = navegador.put(RUTA_PERFIL, cambios);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("nombrePublico").asText())
        .isEqualTo("Taller La Esperanza y Familia");
    assertThat(json(respuesta).get("tipoPrestador").asText()).isEqualTo("EMPRENDIMIENTO");

    assertThat(
            jdbc.queryForObject(
                "SELECT fecha_actualizacion > fecha_creacion FROM perfil_prestador", Boolean.class))
        .as("la aplicación mantiene fechaActualizacion al modificar el perfil")
        .isTrue();
  }

  @Test
  void rechazaUnMunicipioInexistente() {
    Map<String, Object> solicitud = new HashMap<>(solicitudDePerfil());
    solicitud.put("idMunicipioPrincipal", 999999);

    HttpResponse<String> respuesta = navegador.post(RUTA_PERFIL, solicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("MUNICIPIO_NO_DISPONIBLE");
  }

  @Test
  void rechazaUnMunicipioDeUnDepartamentoNoHabilitado() {
    Map<String, Object> solicitud = new HashMap<>(solicitudDePerfil());
    solicitud.put("idMunicipioPrincipal", municipioDeDepartamentoNoHabilitado());

    HttpResponse<String> respuesta = navegador.post(RUTA_PERFIL, solicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("MUNICIPIO_NO_DISPONIBLE");
  }

  @Test
  void alternaLaDisponibilidadEnAmbosSentidos() {
    crearPerfil();

    HttpResponse<String> apagada =
        navegador.put(RUTA_DISPONIBILIDAD, Map.of("disponibilidad", "NO_DISPONIBLE"));
    assertThat(apagada.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(apagada).get("disponibilidad").asText()).isEqualTo("NO_DISPONIBLE");

    HttpResponse<String> encendida =
        navegador.put(RUTA_DISPONIBILIDAD, Map.of("disponibilidad", "DISPONIBLE"));
    assertThat(json(encendida).get("disponibilidad").asText()).isEqualTo("DISPONIBLE");

    assertThat(jdbc.queryForObject("SELECT disponibilidad FROM perfil_prestador", String.class))
        .isEqualTo("DISPONIBLE");
  }

  @Test
  void elPropietarioNoPuedeTocarSuNivelDeVerificacion() {
    crearPerfil();

    // El campo sobrante se ignora: el nivel es una proyección del flujo de
    // verificación (P4V) y ningún DTO de P4 lo acepta.
    Map<String, Object> conNivel = new HashMap<>(solicitudDePerfil());
    conNivel.put("nivelVerificacion", "PROFESIONAL_VERIFICADO");

    HttpResponse<String> respuesta = navegador.put(RUTA_PERFIL, conNivel);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("nivelVerificacion").asText()).isEqualTo("SIN_VERIFICAR");
    assertThat(jdbc.queryForObject("SELECT nivel_verificacion FROM perfil_prestador", String.class))
        .isEqualTo("SIN_VERIFICAR");
  }

  @Test
  void unaCuentaRestringidaConservaLaLecturaPeroNoLasMutaciones() {
    crearPerfil();
    restringirCuenta(CORREO);

    assertThat(navegador.get(RUTA_PERFIL).statusCode()).isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> mutacion = navegador.put(RUTA_PERFIL, solicitudDePerfil());
    assertThat(mutacion.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(mutacion)).isEqualTo("CUENTA_RESTRINGIDA");

    HttpResponse<String> disponibilidad =
        navegador.put(RUTA_DISPONIBILIDAD, Map.of("disponibilidad", "NO_DISPONIBLE"));
    assertThat(disponibilidad.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(disponibilidad)).isEqualTo("CUENTA_RESTRINGIDA");
  }

  @Test
  void unaCuentaSuspendidaNoLlegaNiALaLectura() {
    crearPerfil();
    suspenderCuenta(CORREO);

    HttpResponse<String> respuesta = navegador.get(RUTA_PERFIL);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("ACCESO_DENEGADO");
  }

  @Test
  void sinSesionElPerfilRespondeNoAutenticado() {
    HttpResponse<String> respuesta = abrirNavegador().get(RUTA_PERFIL);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("NO_AUTENTICADO");
  }

  @Test
  void laValidacionDetallaLosCamposRechazados() {
    Map<String, Object> solicitud = new HashMap<>(solicitudDePerfil());
    solicitud.put("nombrePublico", "   ");
    solicitud.put("descripcion", "");

    HttpResponse<String> respuesta = navegador.post(RUTA_PERFIL, solicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("VALIDACION");
    assertThat(json(respuesta).get("errores")).isNotEmpty();
  }

  @Test
  void crearElPerfilSinTokenCsrfEstaProhibido() {
    HttpResponse<String> respuesta = navegador.postSinTokenCsrf(RUTA_PERFIL, solicitudDePerfil());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("ACCESO_DENEGADO");
  }
}
