package com.moica.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.PruebaDeIntegracionConPostgres;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Inicio de sesión: credenciales, fila {@code sesion}, JWT y cookie. */
class InicioDeSesionIT extends PruebaDeIntegracionConPostgres {

  private static final String RUTA_SESION = "/api/auth/sesion";
  private static final String RUTA_USUARIOS = "/api/usuarios";
  private static final String CORREO = "persona@moica.test";
  private static final String CLAVE = "Moica2026$segura";

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper mapeador;

  @LocalServerPort private int puerto;

  private NavegadorDePrueba navegador;

  @BeforeEach
  void registrarUnaCuenta() {
    jdbc.update("DELETE FROM sesion");
    jdbc.update("DELETE FROM solicitud_verificacion_prestador");
    jdbc.update("DELETE FROM imagen_servicio_publicado");
    jdbc.update("DELETE FROM servicio_publicado");
    jdbc.update("DELETE FROM usuario");

    navegador = new NavegadorDePrueba(puerto, mapeador);
    navegador.get(RUTA_SESION);
    navegador.post(
        RUTA_USUARIOS,
        Map.of(
            "nombreCompleto", "Persona Registrada", "correoElectronico", CORREO, "clave", CLAVE));
  }

  @Test
  void abreUnaSesionDeSieteDiasYEntregaSuJwtEnLaCookie() throws Exception {
    HttpResponse<String> respuesta = iniciarSesion(CORREO, CLAVE);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());

    JsonNode cuerpo = mapeador.readTree(respuesta.body());
    assertThat(cuerpo.path("usuario").get("correoElectronico").asText()).isEqualTo(CORREO);
    assertThat(cuerpo.path("sesion").get("segundoFactorVerificado").asBoolean()).isFalse();

    OffsetDateTime inicio = fechaDeLaSesion("fecha_inicio");
    OffsetDateTime expiracion = fechaDeLaSesion("fecha_expiracion");

    assertThat(Duration.between(inicio, expiracion)).isEqualTo(Duration.ofDays(7));
    assertThat(fechaDeLaSesion("fecha_revocacion")).isNull();
    assertThat(jdbc.queryForObject("SELECT motivo_revocacion FROM sesion", String.class)).isNull();
    assertThat(jdbc.queryForObject("SELECT segundo_factor_verificado FROM sesion", Boolean.class))
        .isFalse();
  }

  @Test
  void laCookieDeSesionEsHttpOnlyYNoViajaEnPeticionesCruzadas() {
    HttpResponse<String> respuesta = iniciarSesion(CORREO, CLAVE);

    String cookie =
        respuesta.headers().allValues("set-cookie").stream()
            .filter(valor -> valor.startsWith("moica_sesion="))
            .findFirst()
            .orElseThrow();

    assertThat(cookie).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/");
  }

  @Test
  void elJwtSenalaLaFilaDeSesionMedianteSuJti() throws Exception {
    iniciarSesion(CORREO, CLAVE);

    String token = navegador.cookie("moica_sesion").orElseThrow();
    Claims contenido =
        Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(SECRETO_JWT.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();

    assertThat(contenido.getId())
        .isEqualTo(jdbc.queryForObject("SELECT identificador_token FROM sesion", String.class));
    assertThat(contenido.getSubject())
        .isEqualTo(
            String.valueOf(jdbc.queryForObject("SELECT id_usuario FROM sesion", Long.class)));
  }

  @Test
  void elJwtNoValeMasTiempoQueLaSesionPersistida() {
    iniciarSesion(CORREO, CLAVE);

    String token = navegador.cookie("moica_sesion").orElseThrow();
    Claims contenido =
        Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(SECRETO_JWT.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();

    OffsetDateTime expiracionDeLaSesion = fechaDeLaSesion("fecha_expiracion");

    assertThat(contenido.getExpiration().toInstant())
        .isBeforeOrEqualTo(expiracionDeLaSesion.toInstant());
  }

  @Test
  void aceptaElCorreoEscritoDeOtraForma() {
    HttpResponse<String> respuesta = iniciarSesion("  Persona@MOICA.test  ", CLAVE);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
  }

  @Test
  void rechazaUnaContrasenaIncorrectaSinAbrirSesion() throws Exception {
    HttpResponse<String> respuesta = iniciarSesion(CORREO, "Moica2026$distinta");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(mapeador.readTree(respuesta.body()).get("codigo").asText())
        .isEqualTo("CREDENCIALES_INVALIDAS");
    assertThat(navegador.cookie("moica_sesion")).isEmpty();
    assertThat(cantidadDeSesiones()).isZero();
  }

  @Test
  void respondeIgualAnteUnCorreoInexistenteQueAnteUnaContrasenaIncorrecta() throws Exception {
    HttpResponse<String> claveIncorrecta = iniciarSesion(CORREO, "Moica2026$distinta");
    HttpResponse<String> correoInexistente = iniciarSesion("nadie@moica.test", CLAVE);

    assertThat(correoInexistente.statusCode()).isEqualTo(claveIncorrecta.statusCode());
    assertThat(mensajeYCodigo(correoInexistente)).isEqualTo(mensajeYCodigo(claveIncorrecta));
    assertThat(cantidadDeSesiones()).isZero();
  }

  @Test
  void rechazaUnCuerpoSinCredenciales() throws Exception {
    HttpResponse<String> respuesta = navegador.post(RUTA_SESION, Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(mapeador.readTree(respuesta.body()).get("codigo").asText()).isEqualTo("VALIDACION");
    assertThat(cantidadDeSesiones()).isZero();
  }

  @Test
  void noIniciaSesionSinElTokenCsrf() {
    HttpResponse<String> respuesta =
        navegador.postSinTokenCsrf(
            RUTA_SESION, Map.of("correoElectronico", CORREO, "clave", CLAVE));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(cantidadDeSesiones()).isZero();
  }

  private HttpResponse<String> iniciarSesion(String correo, String clave) {
    return navegador.post(RUTA_SESION, Map.of("correoElectronico", correo, "clave", clave));
  }

  private OffsetDateTime fechaDeLaSesion(String columna) {
    // La columna la elige esta prueba, nunca un dato de entrada.
    return jdbc.queryForObject("SELECT " + columna + " FROM sesion", OffsetDateTime.class);
  }

  private Integer cantidadDeSesiones() {
    return jdbc.queryForObject("SELECT count(*) FROM sesion", Integer.class);
  }

  private String mensajeYCodigo(HttpResponse<String> respuesta) throws Exception {
    JsonNode cuerpo = mapeador.readTree(respuesta.body());
    return cuerpo.get("codigo").asText() + "|" + cuerpo.get("mensaje").asText();
  }
}
