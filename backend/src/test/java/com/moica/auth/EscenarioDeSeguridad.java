package com.moica.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.PruebaDeIntegracionConPostgres;
import com.moica.auth.seguridad.AlgoritmoTotp;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Punto de partida común de las pruebas de seguridad de P3.
 *
 * <p>Cada una necesita lo mismo: una base limpia, un navegador con su cookie CSRF, una cuenta
 * registrada y, según el caso, un segundo factor activo o un rol administrativo. Repetir ese
 * montaje en cinco archivos escondería lo que cada prueba viene a demostrar.
 *
 * <p>Los códigos TOTP se calculan con el mismo algoritmo del servidor sobre un instante explícito.
 * Ninguna prueba espera a que pase un periodo real.
 */
public abstract class EscenarioDeSeguridad extends PruebaDeIntegracionConPostgres {

  protected static final String RUTA_SESION = "/api/auth/sesion";
  protected static final String RUTA_VERIFICACION = "/api/auth/sesion/segundo-factor";
  protected static final String RUTA_SEGUNDO_FACTOR = "/api/auth/segundo-factor";
  protected static final String RUTA_CLAVE = "/api/auth/clave";
  protected static final String RUTA_USUARIOS = "/api/usuarios";
  protected static final String RUTA_ADMIN = "/api/admin/resumen";

  protected static final String COOKIE_SESION = "moica_sesion";
  protected static final String CORREO = "persona@moica.test";
  protected static final String CLAVE = "Moica2026$segura";

  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected ObjectMapper mapeador;
  @Autowired protected AlgoritmoTotp totp;

  @LocalServerPort protected int puerto;

  protected NavegadorDePrueba navegador;

  @BeforeEach
  protected void prepararEscenario() {
    // Las solicitudes de verificación apuntan al administrador que las resolvió
    // con ON DELETE RESTRICT, así que se retiran antes: si no, borrar las
    // cuentas chocaría con esa restricción en lugar de limpiar.
    jdbc.update("DELETE FROM solicitud_verificacion_prestador");
    // Los servicios apuntan al perfil con ON DELETE RESTRICT: hay que
    // retirarlos antes de borrar las cuentas.
    jdbc.update("DELETE FROM imagen_servicio_publicado");
    jdbc.update("DELETE FROM servicio_publicado");
    // Borrar las cuentas arrastra en cascada sus sesiones, su segundo factor y
    // su rol administrativo, que es justamente lo que declara la migración.
    jdbc.update("DELETE FROM usuario");

    navegador = abrirNavegador();
    registrar(navegador, CORREO, CLAVE);
  }

  /** Un navegador nuevo, como quien abre la aplicación por primera vez. */
  protected NavegadorDePrueba abrirNavegador() {
    NavegadorDePrueba nuevo = new NavegadorDePrueba(puerto, mapeador);
    // Cualquier primera visita deja el token CSRF que acompaña a lo que venga.
    nuevo.get(RUTA_SESION);
    return nuevo;
  }

  protected void registrar(NavegadorDePrueba desde, String correo, String clave) {
    HttpResponse<String> respuesta =
        desde.post(
            RUTA_USUARIOS,
            Map.of(
                "nombreCompleto",
                "Persona de Prueba",
                "correoElectronico",
                correo,
                "clave",
                clave));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
  }

  protected HttpResponse<String> iniciarSesion(
      NavegadorDePrueba desde, String correo, String clave) {
    return desde.post(RUTA_SESION, Map.of("correoElectronico", correo, "clave", clave));
  }

  /** Inicia sesión con la cuenta del escenario y da por hecho que sale bien. */
  protected HttpResponse<String> iniciarSesion(NavegadorDePrueba desde) {
    HttpResponse<String> respuesta = iniciarSesion(desde, CORREO, CLAVE);
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return respuesta;
  }

  /**
   * Recorre la activación completa del segundo factor desde un navegador con sesión iniciada.
   *
   * @return el secreto en Base32, que la API entrega una única vez
   */
  protected String activarSegundoFactor(NavegadorDePrueba desde) {
    HttpResponse<String> activacion = desde.post(RUTA_SEGUNDO_FACTOR, Map.of());
    assertThat(activacion.statusCode()).isEqualTo(HttpStatus.OK.value());

    String secreto = json(activacion).get("claveManual").asText();

    HttpResponse<String> confirmacion =
        desde.post(RUTA_SEGUNDO_FACTOR + "/activacion", Map.of("codigo", codigoValido(secreto)));
    assertThat(confirmacion.statusCode()).isEqualTo(HttpStatus.OK.value());

    return secreto;
  }

  /** Código que la aplicación autenticadora mostraría en este momento. */
  protected String codigoValido(String secreto) {
    return totp.codigoPara(secreto, Instant.now());
  }

  /** Código correcto pero de otro momento, muy fuera de la tolerancia configurada. */
  protected String codigoDeOtroMomento(String secreto) {
    return totp.codigoPara(secreto, Instant.now().plus(Duration.ofHours(2)));
  }

  /** Concede el rol administrativo sin pasar por el arranque, para montar el escenario. */
  protected void darRolAdministrativo(String correo) {
    jdbc.update(
        """
        INSERT INTO administrador (id_administrador)
        SELECT id_usuario FROM usuario WHERE correo_electronico = ?
        """,
        correo);
  }

  protected JsonNode json(HttpResponse<String> respuesta) {
    return mapeador.readTree(respuesta.body());
  }

  protected String codigoDeError(HttpResponse<String> respuesta) {
    return json(respuesta).get("codigo").asText();
  }

  protected Integer sesionesVigentes() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM sesion WHERE fecha_revocacion IS NULL", Integer.class);
  }
}
