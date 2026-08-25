package com.moica.auth.seguridad;

import java.util.function.Predicate;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Reglas de autorización que dependen del sujeto y no solo de la ruta.
 *
 * <p>Spring Security resuelve por sí solo «hace falta estar autenticado» y «hace falta este rol».
 * Lo que no puede resolver solo es lo que en Moica decide el acceso: si la sesión ya superó el
 * segundo factor cuando la cuenta lo exige y si el estado de la cuenta la deja operar. Esas dos
 * condiciones se declaran aquí una vez y se aplican en {@link ConfiguracionDeSeguridad}.
 *
 * <p>Ninguna regla devuelve «me abstengo»: cada una concede o deniega. Una petición sin sesión
 * llega como anónima y se deniega igual, y es el traductor de excepciones de Spring Security el que
 * la convierte en 401 en lugar de 403.
 */
public final class AutorizacionDeSesion {

  private AutorizacionDeSesion() {}

  /**
   * Sesión con la que se puede operar con normalidad.
   *
   * <p>Deja fuera dos casos: la sesión provisional, a la que todavía le falta el segundo factor, y
   * la de una cuenta suspendida.
   */
  public static AuthorizationManager<RequestAuthorizationContext> sesionPlena() {
    return regla(UsuarioAutenticado::esPlena);
  }

  /**
   * Sesión que puede entrar en el área administrativa.
   *
   * <p>Son dos condiciones simultáneas, tal como pide la definición del producto: rol
   * administrativo y segundo factor verificado <em>en esa sesión</em>.
   */
  public static AuthorizationManager<RequestAuthorizationContext> areaAdministrativa() {
    return regla(UsuarioAutenticado::puedeAdministrar);
  }

  private static AuthorizationManager<RequestAuthorizationContext> regla(
      Predicate<UsuarioAutenticado> condicion) {

    return new AuthorizationManager<>() {
      @Override
      public AuthorizationResult authorize(
          Supplier<? extends Authentication> autenticacion, RequestAuthorizationContext contexto) {
        return new AuthorizationDecision(cumple(autenticacion.get(), condicion));
      }
    };
  }

  private static boolean cumple(
      Authentication autenticacion, Predicate<UsuarioAutenticado> condicion) {

    return autenticacion != null
        && autenticacion.getPrincipal() instanceof UsuarioAutenticado sujeto
        && condicion.test(sujeto);
  }
}
