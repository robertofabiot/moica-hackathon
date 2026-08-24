package com.moica.auth.seguridad;

import com.moica.auth.entity.Sesion;
import com.moica.auth.service.SesionService;
import com.moica.auth.service.TokenDeSesionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reconoce a quien llega con una sesión vigente.
 *
 * <p>En cada petición: lee la cookie, comprueba la firma y la expiración del JWT, y con su {@code
 * jti} busca la fila {@code sesion}. Solo autentica si esa fila existe, no ha expirado y no fue
 * revocada; esa comprobación persistente es la que hace efectivos el cierre de sesión y, más
 * adelante, el cambio de credenciales y las medidas administrativas.
 *
 * <p>Cuando algo no cuadra el filtro no rechaza la petición: simplemente no autentica. Quien decide
 * si hacía falta estar autenticado es la cadena de autorización, que responderá 401.
 *
 * <p>No es un {@code @Component} a propósito: Spring Boot registra automáticamente en el servidor
 * todo bean que sea un filtro, y entonces este se ejecutaría también fuera de la cadena de
 * seguridad, antes de que Spring Security prepare el contexto. El contexto que dejara puesto se
 * perdería. Lo construye {@link ConfiguracionDeSeguridad}, que es quien lo coloca en la cadena.
 */
public class FiltroDeSesion extends OncePerRequestFilter {

  private final CookieDeSesion cookie;
  private final TokenDeSesionService tokens;
  private final SesionService sesiones;

  public FiltroDeSesion(
      CookieDeSesion cookie, TokenDeSesionService tokens, SesionService sesiones) {
    this.cookie = cookie;
    this.tokens = tokens;
    this.sesiones = sesiones;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
      throws ServletException, IOException {

    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      cookie
          .leerToken(peticion)
          .flatMap(tokens::leerIdentificadorDeSesion)
          .flatMap(sesiones::buscarVigente)
          .ifPresent(sesion -> autenticar(sesion, peticion));
    }

    cadena.doFilter(peticion, respuesta);
  }

  private void autenticar(Sesion sesion, HttpServletRequest peticion) {
    UsuarioAutenticado sujeto = new UsuarioAutenticado(sesion.getIdUsuario(), sesion.getIdSesion());

    // P2 no tiene roles todavía: la lista de permisos llega con la matriz de
    // autorización de P3.
    UsernamePasswordAuthenticationToken autenticacion =
        UsernamePasswordAuthenticationToken.authenticated(sujeto, null, List.of());
    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(peticion));

    SecurityContextHolder.getContext().setAuthentication(autenticacion);
  }
}
