package com.moica.auth.service;

import com.moica.auth.entity.Sesion;
import com.moica.auth.seguridad.PropiedadesDeSeguridad;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Emite y lee el JWT que identifica a una sesión.
 *
 * <p>El token no sustituye a la fila {@code sesion}: solo la señala. Por eso lo único que se lee de
 * él es el {@code jti}, que corresponde con {@link Sesion#getIdentificadorToken()}. Su expiración
 * se copia de la de la sesión, de modo que el token nunca vale más tiempo que ella.
 */
@Service
public final class TokenDeSesionService {

  private static final Logger LOG = LoggerFactory.getLogger(TokenDeSesionService.class);

  private final SecretKey clave;

  public TokenDeSesionService(PropiedadesDeSeguridad propiedades) {
    this.clave = Keys.hmacShaKeyFor(propiedades.secretoJwt().getBytes(StandardCharsets.UTF_8));
  }

  /** Firma el token de una sesión recién abierta. */
  public String emitir(Sesion sesion) {
    return Jwts.builder()
        .subject(String.valueOf(sesion.getIdUsuario()))
        .id(sesion.getIdentificadorToken())
        .issuedAt(Date.from(sesion.getFechaInicio().toInstant()))
        .expiration(Date.from(sesion.getFechaExpiracion().toInstant()))
        .signWith(clave)
        .compact();
  }

  /**
   * Devuelve el identificador de sesión que transporta un token válido.
   *
   * <p>Un token ilegible, alterado, firmado con otra clave o ya vencido devuelve vacío: no
   * autentica y no se distingue de la ausencia de token.
   */
  public Optional<String> leerIdentificadorDeSesion(String token) {
    try {
      Claims contenido =
          Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
      return Optional.ofNullable(contenido.getId());
    } catch (JwtException | IllegalArgumentException rechazo) {
      // Se registra el tipo de rechazo, nunca el token ni su contenido.
      LOG.debug("Token de sesión descartado: {}", rechazo.getClass().getSimpleName());
      return Optional.empty();
    }
  }
}
