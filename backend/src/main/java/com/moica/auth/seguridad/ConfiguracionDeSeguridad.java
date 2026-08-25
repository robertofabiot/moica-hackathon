package com.moica.auth.seguridad;

import com.moica.auth.service.SegundoFactorService;
import com.moica.auth.service.SesionService;
import com.moica.auth.service.TokenDeSesionService;
import com.moica.usuario.service.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Cadena de seguridad de la API.
 *
 * <p>Decisiones que aplica, todas del plan:
 *
 * <ul>
 *   <li>La autenticación viaja en una cookie {@code HttpOnly} y se comprueba contra la fila {@code
 *       sesion} en cada petición, así que no hay sesión de servlet: {@link
 *       SessionCreationPolicy#STATELESS}.
 *   <li>CSRF sigue activo. La configuración es la que Spring Security documenta para una SPA del
 *       mismo origen: el token viaja en la cookie {@code XSRF-TOKEN}, legible por JavaScript, y el
 *       navegador lo devuelve en la cabecera {@code X-XSRF-TOKEN} de toda operación mutable.
 *   <li>No se abre CORS: en producción el frontend y la API comparten origen y en desarrollo lo
 *       resuelve el proxy de Vite.
 *   <li>Autorización por omisión cerrada: lo que no se declara exige una sesión plena. Añadir un
 *       endpoint nuevo sin pensar en sus permisos lo deja protegido, no abierto.
 * </ul>
 */
@Configuration
public class ConfiguracionDeSeguridad {

  /**
   * Algoritmo con el que se guardan las contraseñas.
   *
   * <p>BCrypt con su coste por omisión. Solo tiene en cuenta los primeros 72 bytes, por eso la
   * política añade un tope de 72 bytes UTF-8 además de los 8–72 caracteres de D-SEC-02.
   */
  @Bean
  public PasswordEncoder codificadorDeClaves() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain cadenaDeSeguridad(
      HttpSecurity http,
      CookieDeSesion cookie,
      TokenDeSesionService tokens,
      SesionService sesiones,
      UsuarioService usuarios,
      SegundoFactorService segundoFactor,
      PuntoDeEntradaNoAutenticado puntoDeEntrada,
      ManejadorDeAccesoDenegado accesoDenegado,
      PropiedadesDeSeguridad propiedades)
      throws Exception {

    // El filtro se construye aquí y no se publica como bean: si lo fuera, Spring
    // Boot lo registraría además en el servidor, fuera de esta cadena.
    FiltroDeSesion filtroDeSesion =
        new FiltroDeSesion(cookie, tokens, sesiones, usuarios, segundoFactor);

    return http.csrf(
            csrf ->
                csrf.csrfTokenRepository(repositorioDeTokenCsrf(propiedades))
                    .csrfTokenRequestHandler(manejadorDeTokenCsrf()))
        .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Moica solo se autentica con su propia cookie de sesión: ni formulario
        // de Spring Security, ni autenticación básica, ni su cierre de sesión.
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            rutas ->
                rutas
                    // Registrarse e iniciar sesión son, por definición, lo que
                    // se hace sin haber iniciado sesión.
                    .requestMatchers(HttpMethod.POST, "/api/usuarios")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/sesion")
                    .permitAll()
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    // El despacho interno de errores de Spring MVC no es una
                    // petición de nadie: sin esto, un 404 se convertiría en 403.
                    .requestMatchers("/error")
                    .permitAll()
                    // Lo único que puede hacer una sesión provisional: mirar en
                    // qué estado está, presentar su código y marcharse. Es
                    // también lo único que le queda a una cuenta suspendida.
                    .requestMatchers(HttpMethod.GET, "/api/auth/sesion")
                    .authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/auth/sesion")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/auth/sesion/segundo-factor")
                    .authenticated()
                    // El área administrativa exige rol y segundo factor
                    // verificado en esta misma sesión.
                    .requestMatchers("/api/admin/**")
                    .access(AutorizacionDeSesion.areaAdministrativa())
                    .anyRequest()
                    .access(AutorizacionDeSesion.sesionPlena()))
        .exceptionHandling(
            errores ->
                errores
                    .authenticationEntryPoint(puntoDeEntrada)
                    .accessDeniedHandler(accesoDenegado))
        // Antes del filtro anónimo a propósito: ese deja siempre un sujeto
        // anónimo en el contexto, y a partir de ahí el filtro de sesión ya no
        // podría distinguir «nadie ha llegado» de «llegó una sesión válida».
        .addFilterBefore(filtroDeSesion, AnonymousAuthenticationFilter.class)
        .build();
  }

  private static CookieCsrfTokenRepository repositorioDeTokenCsrf(
      PropiedadesDeSeguridad propiedades) {

    // `withHttpOnlyFalse` es intencionado: el token CSRF debe poder leerlo el
    // JavaScript de Moica para devolverlo en la cabecera. Lo que nunca es
    // legible por script es la cookie de sesión.
    CookieCsrfTokenRepository repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repositorio.setCookieCustomizer(
        cookie -> cookie.secure(propiedades.cookieSegura()).sameSite("Lax").path("/"));
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
