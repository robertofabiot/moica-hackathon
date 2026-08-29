package com.moica.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.PruebaDeIntegracionConPostgres;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Lo que ocurre después de iniciar sesión: reconocer a la persona, rechazar una sesión que ya no
 * vale y cerrarla.
 *
 * <p>Cada caso comprueba lo mismo desde un ángulo distinto: manda la fila {@code sesion}, no el
 * token.
 */
class CicloDeSesionIT extends PruebaDeIntegracionConPostgres {

  private static final String RUTA_SESION = "/api/auth/sesion";
  private static final String COOKIE_SESION = "moica_sesion";
  private static final String CORREO = "sesion@moica.test";
  private static final String CLAVE = "Moica2026$segura";

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper mapeador;

  @LocalServerPort private int puerto;

  private NavegadorDePrueba navegador;

  @BeforeEach
  void registrarCuentaEIniciarSesion() {
    jdbc.update("DELETE FROM sesion");
    jdbc.update("DELETE FROM solicitud_verificacion_prestador");
    jdbc.update("DELETE FROM imagen_servicio_publicado");
    jdbc.update("DELETE FROM servicio_publicado");
    jdbc.update("DELETE FROM usuario");

    navegador = new NavegadorDePrueba(puerto, mapeador);
    navegador.get(RUTA_SESION);
    navegador.post(
        "/api/usuarios",
        Map.of(
            "nombreCompleto", "Persona Con Sesión", "correoElectronico", CORREO, "clave", CLAVE));
    navegador.post(RUTA_SESION, Map.of("correoElectronico", CORREO, "clave", CLAVE));
  }

  @Test
  void reconoceALaPersonaMientrasLaSesionSigueVigente() throws Exception {
    HttpResponse<String> respuesta = navegador.get(RUTA_SESION);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());

    JsonNode cuerpo = mapeador.readTree(respuesta.body());
    assertThat(cuerpo.path("usuario").get("correoElectronico").asText()).isEqualTo(CORREO);
    assertThat(cuerpo.path("usuario").get("nombreCompleto").asText())
        .isEqualTo("Persona Con Sesión");
    assertThat(cuerpo.path("sesion").get("fechaExpiracion").asText()).isNotBlank();
    assertThat(respuesta.body()).doesNotContain(CLAVE).doesNotContain("identificadorToken");
  }

  @Test
  void rechazaLaPeticionQueLlegaSinCookie() throws Exception {
    navegador.olvidarCookies();

    HttpResponse<String> respuesta = navegador.get(RUTA_SESION);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

    JsonNode cuerpo = mapeador.readTree(respuesta.body());
    assertThat(cuerpo.get("codigo").asText()).isEqualTo("NO_AUTENTICADO");
    assertThat(cuerpo.get("estado").asInt()).isEqualTo(401);
    assertThat(cuerpo.get("ruta").asText()).isEqualTo(RUTA_SESION);
    assertThat(cuerpo.has("errores")).isFalse();
  }

  @Test
  void rechazaUnTokenFirmadoConOtraClave() {
    String suplantado =
        Jwts.builder()
            .subject("1")
            .id(identificadorDeLaSesion())
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
            .signWith(claveDeFirma("otro-secreto-que-moica-no-conoce-de-nada"))
            .compact();

    navegador.ponerCookie(COOKIE_SESION, suplantado);

    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void rechazaUnJwtQueYaVencio() {
    String vencido =
        Jwts.builder()
            .subject("1")
            .id(identificadorDeLaSesion())
            .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.DAYS)))
            .expiration(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
            .signWith(claveDeFirma(SECRETO_JWT))
            .compact();

    navegador.ponerCookie(COOKIE_SESION, vencido);

    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void rechazaUnaSesionExpiradaAunqueElJwtSigaVigente() {
    // Solo envejece la fila: el JWT del navegador conserva su expiración futura.
    jdbc.update(
        """
        UPDATE sesion
        SET fecha_inicio = now() - interval '8 days',
            fecha_expiracion = now() - interval '1 day'
        """);

    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void rechazaUnaSesionRevocadaAunqueElJwtSigaVigente() {
    jdbc.update(
        """
        UPDATE sesion
        SET fecha_revocacion = now(), motivo_revocacion = 'MEDIDA_ADMINISTRATIVA'
        """);

    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void cerrarSesionLaRevocaComoCierreVoluntarioYCaducaLaCookie() {
    HttpResponse<String> cierre = navegador.delete(RUTA_SESION);

    assertThat(cierre.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(jdbc.queryForObject("SELECT motivo_revocacion FROM sesion", String.class))
        .isEqualTo("CIERRE_VOLUNTARIO");
    assertThat(
            jdbc.queryForObject(
                "SELECT fecha_revocacion FROM sesion", java.time.OffsetDateTime.class))
        .isNotNull();
    assertThat(navegador.cookie(COOKIE_SESION))
        .as("el navegador debe quedarse sin cookie de sesión")
        .isEmpty();
  }

  @Test
  void laPeticionSiguienteAlCierreDeSesionYaNoAutentica() {
    navegador.delete(RUTA_SESION);

    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void elTokenSigueSinValerAunqueSeVuelvaAPresentarLaCookie() {
    String token = navegador.cookie(COOKIE_SESION).orElseThrow();

    navegador.delete(RUTA_SESION);
    navegador.ponerCookie(COOKIE_SESION, token);

    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void noCierraLaSesionSinElTokenCsrf() {
    HttpResponse<String> respuesta = navegador.deleteSinTokenCsrf(RUTA_SESION);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(jdbc.queryForObject("SELECT motivo_revocacion FROM sesion", String.class)).isNull();
    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.OK.value());
  }

  private String identificadorDeLaSesion() {
    return jdbc.queryForObject("SELECT identificador_token FROM sesion", String.class);
  }

  private static SecretKey claveDeFirma(String secreto) {
    return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
  }
}
