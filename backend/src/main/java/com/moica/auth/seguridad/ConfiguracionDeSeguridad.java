package com.moica.auth.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Cadena de seguridad de la API.
 *
 * <p>Decisiones que aplica, todas del plan:
 *
 * <ul>
 *   <li>Nada de sesión de servlet: la autenticación de Moica viajará en su propia cookie y se
 *       comprobará en cada petición, así que la cadena es {@link SessionCreationPolicy#STATELESS}.
 *   <li>CSRF sigue activo. La configuración es la que Spring Security documenta para una SPA del
 *       mismo origen: el token viaja en la cookie {@code XSRF-TOKEN}, legible por JavaScript, y el
 *       navegador lo devuelve en la cabecera {@code X-XSRF-TOKEN} de toda operación mutable.
 *   <li>No se abre CORS: en producción el frontend y la API comparten origen y en desarrollo lo
 *       resuelve el proxy de Vite.
 * </ul>
 */
@Configuration
public class ConfiguracionDeSeguridad {

  /**
   * Algoritmo con el que se guardan las contraseñas.
   *
   * <p>BCrypt con su coste por omisión. Solo tiene en cuenta los primeros 72 bytes, que es de donde
   * sale el máximo de la política de contraseña.
   */
  @Bean
  public PasswordEncoder codificadorDeClaves() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain cadenaDeSeguridad(
      HttpSecurity http,
      PuntoDeEntradaNoAutenticado puntoDeEntrada,
      ManejadorDeAccesoDenegado accesoDenegado,
      @Value("${moica.seguridad.cookie-segura}") boolean cookieSegura)
      throws Exception {

    return http.csrf(
            csrf ->
                csrf.csrfTokenRepository(repositorioDeTokenCsrf(cookieSegura))
                    .csrfTokenRequestHandler(manejadorDeTokenCsrf()))
        .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Moica se autenticará con su propia cookie: ni formulario de Spring
        // Security, ni autenticación básica, ni su cierre de sesión.
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            rutas ->
                rutas
                    // Registrarse es, por definición, lo que se hace sin haber
                    // iniciado sesión.
                    .requestMatchers(HttpMethod.POST, "/api/usuarios")
                    .permitAll()
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    // El despacho interno de errores de Spring MVC no es una
                    // petición de nadie: sin esto, un 404 se convertiría en 403.
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errores ->
                errores
                    .authenticationEntryPoint(puntoDeEntrada)
                    .accessDeniedHandler(accesoDenegado))
        .build();
  }

  private static CookieCsrfTokenRepository repositorioDeTokenCsrf(boolean cookieSegura) {
    // `withHttpOnlyFalse` es intencionado: el token CSRF debe poder leerlo el
    // JavaScript de Moica para devolverlo en la cabecera.
    CookieCsrfTokenRepository repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repositorio.setCookieCustomizer(
        cookie -> cookie.secure(cookieSegura).sameSite("Lax").path("/"));
    return repositorio;
  }

  private static CsrfTokenRequestAttributeHandler manejadorDeTokenCsrf() {
    CsrfTokenRequestAttributeHandler manejador = new CsrfTokenRequestAttributeHandler();
    // Sin nombre de atributo, el token se resuelve en cada petición en lugar de
    // esperar a que alguien lo lea. Es lo que garantiza que la cookie llegue al
    // navegador antes de la primera operación mutable, incluido el registro.
    manejador.setCsrfRequestAttributeName(null);
    return manejador;
  }
}
