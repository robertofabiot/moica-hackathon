package com.moica.auth.seguridad;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * La cookie que transporta el JWT de sesión.
 *
 * <p>Es {@code HttpOnly} para que ningún script pueda leerla, {@code SameSite=Lax} para que no
 * viaje en peticiones cruzadas y {@code Secure} en producción. El token no se guarda en {@code
 * localStorage} ni en {@code sessionStorage}.
 */
@Component
public class CookieDeSesion {

  /** Nombre de la cookie. Se mantiene estable porque el navegador la conserva entre despliegues. */
  public static final String NOMBRE = "moica_sesion";

  private final PropiedadesDeSeguridad propiedades;

  public CookieDeSesion(PropiedadesDeSeguridad propiedades) {
    this.propiedades = propiedades;
  }

  /** Cookie con el token de una sesión recién abierta. */
  public ResponseCookie conToken(String token) {
    return base(token).maxAge(propiedades.duracionDeSesion()).build();
  }

  private ResponseCookie.ResponseCookieBuilder base(String valor) {
    return ResponseCookie.from(NOMBRE, valor)
        .httpOnly(true)
        .secure(propiedades.cookieSegura())
        .sameSite("Lax")
        .path("/");
  }
}
