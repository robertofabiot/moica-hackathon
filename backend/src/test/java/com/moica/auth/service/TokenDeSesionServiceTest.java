package com.moica.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.auth.entity.Sesion;
import com.moica.auth.seguridad.PropiedadesDeSeguridad;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.Test;

/** El JWT de sesión: qué transporta y qué se niega a aceptar. */
class TokenDeSesionServiceTest {

  private static final String SECRETO =
      "secreto-de-pruebas-unitarias-de-moica-suficientemente-largo";
  private static final String IDENTIFICADOR = "identificador-de-la-sesion";

  private final TokenDeSesionService servicio =
      new TokenDeSesionService(new PropiedadesDeSeguridad(SECRETO, Duration.ofDays(7), false));

  @Test
  void elTokenTransportaElIdentificadorDeLaSesion() {
    String token = servicio.emitir(sesion());

    assertThat(servicio.leerIdentificadorDeSesion(token)).contains(IDENTIFICADOR);
  }

  @Test
  void elTokenExpiraCuandoExpiraLaSesion() {
    Sesion sesion = sesion();

    String token = servicio.emitir(sesion);
    Date expiracion =
        Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration();

    // El JWT guarda los segundos, no los nanosegundos: no puede vencer después
    // que la sesión, aunque pueda hacerlo una fracción antes.
    assertThat(expiracion.toInstant()).isBeforeOrEqualTo(sesion.getFechaExpiracion().toInstant());
    assertThat(expiracion.toInstant())
        .isAfterOrEqualTo(sesion.getFechaExpiracion().toInstant().minusSeconds(1));
  }

  @Test
  void noAceptaUnTokenAlterado() {
    String token = servicio.emitir(sesion());
    String alterado = token.substring(0, token.length() - 2) + "xy";

    assertThat(servicio.leerIdentificadorDeSesion(alterado)).isEmpty();
  }

  @Test
  void noAceptaUnTokenFirmadoConOtraClave() {
    String ajeno =
        Jwts.builder()
            .id(IDENTIFICADOR)
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
            .signWith(
                Keys.hmacShaKeyFor(
                    "otro-secreto-que-moica-no-conoce-de-nada-y-es-largo"
                        .getBytes(StandardCharsets.UTF_8)))
            .compact();

    assertThat(servicio.leerIdentificadorDeSesion(ajeno)).isEmpty();
  }

  @Test
  void noAceptaUnTokenVencido() {
    String vencido =
        Jwts.builder()
            .id(IDENTIFICADOR)
            .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.DAYS)))
            .expiration(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
            .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
            .compact();

    assertThat(servicio.leerIdentificadorDeSesion(vencido)).isEmpty();
  }

  @Test
  void noAceptaUnValorQueNiSiquieraEsUnToken() {
    assertThat(servicio.leerIdentificadorDeSesion("esto-no-es-un-jwt")).isEmpty();
    assertThat(servicio.leerIdentificadorDeSesion("")).isEmpty();
  }

  private static Sesion sesion() {
    OffsetDateTime inicio = OffsetDateTime.now();
    return new Sesion(7L, IDENTIFICADOR, inicio, inicio.plusDays(7));
  }
}
