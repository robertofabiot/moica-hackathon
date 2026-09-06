package com.moica.auth.seguridad;

import com.moica.auth.entity.Sesion;
import com.moica.auth.service.SegundoFactorService;
import com.moica.auth.service.SesionService;
import com.moica.auth.service.TokenDeSesionService;
import com.moica.usuario.dto.DatosDeUsuario;
import com.moica.usuario.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reconoce a quien llega con una sesión vigente.
 *
 * <p>En cada petición: lee la cookie, comprueba la firma y la expiración del JWT, y con su {@code
 * jti} busca la fila {@code sesion}. Solo autentica si esa fila existe, no ha expirado y no fue
 * revocada; esa comprobación persistente es la que hace efectivos el cierre de sesión, el cambio de
 * credenciales y, más adelante, las medidas administrativas.
 *
 * <p>Cuando algo no cuadra el filtro no rechaza la petición: simplemente no autentica. Quien decide
 * si hacía falta estar autenticado es la cadena de autorización, que responderá 401.
 *
 * <p>Reconocer a alguien no equivale a dejarle hacer nada: el sujeto que deja en el contexto
 * incluye el rol, el estado de la cuenta y si el segundo factor está verificado, y con eso la
 * cadena de autorización responde 403 a lo que esa sesión no pueda hacer.
 *
 * <p>No es un {@code @Component} a propósito: Spring Boot registra automáticamente en el servidor
 * todo bean que sea un filtro, y entonces este se ejecutaría también fuera de la cadena de
 * seguridad, antes de que Spring Security prepare el contexto. El contexto que dejara puesto se
 * perdería. Lo construye {@link ConfiguracionDeSeguridad}, que es quien lo coloca en la cadena.
 */
public class FiltroDeSesion extends OncePerRequestFilter {

  /** Autoridad con la que Spring Security reconoce el rol administrativo. */
  public static final String ROL_ADMINISTRADOR = "ROLE_ADMINISTRADOR";

  private final CookieDeSesion cookie;
  private final TokenDeSesionService tokens;
  private final SesionService sesiones;
  private final UsuarioService usuarios;
  private final SegundoFactorService segundoFactor;

  public FiltroDeSesion(
      CookieDeSesion cookie,
      TokenDeSesionService tokens,
      SesionService sesiones,
      UsuarioService usuarios,
      SegundoFactorService segundoFactor) {
    this.cookie = cookie;
    this.tokens = tokens;
    this.sesiones = sesiones;
    this.usuarios = usuarios;
    this.segundoFactor = segundoFactor;
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
    // El rol, el estado de la cuenta y la configuración del segundo factor se
    // releen aquí en lugar de viajar en el token: así retirar un permiso o
    // suspender una cuenta surte efecto en la petición siguiente.
    DatosDeUsuario usuario = usuarios.obtener(sesion.getIdUsuario());

    UsuarioAutenticado sujeto =
        new UsuarioAutenticado(
            sesion.getIdUsuario(),
            sesion.getIdSesion(),
            usuario.esAdministrador(),
            usuario.estadoCuenta(),
            segundoFactor.estaActivoEn(sesion.getIdUsuario()),
            sesion.isSegundoFactorVerificado());

    UsernamePasswordAuthenticationToken autenticacion =
        UsernamePasswordAuthenticationToken.authenticated(sujeto, null, autoridadesDe(sujeto));
    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(peticion));

    SecurityContextHolder.getContext().setAuthentication(autenticacion);
  }

  private static List<SimpleGrantedAuthority> autoridadesDe(UsuarioAutenticado sujeto) {
    return sujeto.administrador()
        ? List.of(new SimpleGrantedAuthority(ROL_ADMINISTRADOR))
        : List.of();
  }
}
