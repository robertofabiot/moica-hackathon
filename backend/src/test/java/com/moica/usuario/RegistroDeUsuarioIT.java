package com.moica.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.PruebaDeIntegracionConPostgres;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Registro de una cuenta de extremo a extremo: petición HTTP real, validación, hash y fila en
 * PostgreSQL.
 */
class RegistroDeUsuarioIT extends PruebaDeIntegracionConPostgres {

  private static final String RUTA = "/api/usuarios";
  private static final String CLAVE_VALIDA = "Moica2026$segura";

  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper mapeador;

  @LocalServerPort private int puerto;

  private NavegadorDePrueba navegador;

  @BeforeEach
  void prepararNavegadorYBase() {
    jdbc.update("DELETE FROM sesion");
    jdbc.update("DELETE FROM calificacion_usuario");
    jdbc.update("DELETE FROM mensaje_solicitud");
    jdbc.update("DELETE FROM cambio_estado_solicitud");
    jdbc.update("DELETE FROM solicitud_servicio");
    jdbc.update("DELETE FROM solicitud_verificacion_prestador");
    jdbc.update("DELETE FROM imagen_servicio_publicado");
    jdbc.update("DELETE FROM servicio_publicado");
    jdbc.update("DELETE FROM usuario");
    navegador = new NavegadorDePrueba(puerto, mapeador);
    // Como haría el navegador de una persona: cualquier primera visita deja el
    // token CSRF que después acompaña a cada operación mutable.
    navegador.get("/actuator/health");
  }

  @Test
  void registraLaCuentaYGuardaSoloElHashDeLaContrasena() throws Exception {
    HttpResponse<String> respuesta =
        navegador.post(RUTA, cuerpo("Erving Miranda", "erving@moica.test", CLAVE_VALIDA));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());

    JsonNode cuerpo = mapeador.readTree(respuesta.body());
    assertThat(cuerpo.get("idUsuario").asLong()).isPositive();
    assertThat(cuerpo.get("nombreCompleto").asText()).isEqualTo("Erving Miranda");
    assertThat(cuerpo.get("correoElectronico").asText()).isEqualTo("erving@moica.test");
    assertThat(cuerpo.get("estadoCuenta").asText()).isEqualTo("ACTIVA");
    assertThat(cuerpo.get("fechaRegistro").asText()).isNotBlank();

    String claveHash =
        jdbc.queryForObject(
            "SELECT clave_hash FROM usuario WHERE correo_electronico = ?",
            String.class,
            "erving@moica.test");

    assertThat(claveHash).isNotEqualTo(CLAVE_VALIDA).startsWith("$2");
  }

  @Test
  void noDevuelveLaContrasenaNiSuHash() {
    HttpResponse<String> respuesta =
        navegador.post(RUTA, cuerpo("Persona Discreta", "discreta@moica.test", CLAVE_VALIDA));

    assertThat(respuesta.body()).doesNotContain(CLAVE_VALIDA).doesNotContain("clave");
  }

  @Test
  void guardaElCorreoNormalizado() {
    HttpResponse<String> respuesta =
        navegador.post(RUTA, cuerpo("Persona Distraída", "  Persona@Moica.TEST  ", CLAVE_VALIDA));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());

    Integer cuentas =
        jdbc.queryForObject(
            "SELECT count(*) FROM usuario WHERE correo_electronico = ?",
            Integer.class,
            "persona@moica.test");
    assertThat(cuentas).isEqualTo(1);
  }

  @Test
  void rechazaUnCorreoYaRegistrado() throws Exception {
    navegador.post(RUTA, cuerpo("Primera Persona", "unica@moica.test", CLAVE_VALIDA));

    HttpResponse<String> repetido =
        navegador.post(RUTA, cuerpo("Segunda Persona", "unica@moica.test", CLAVE_VALIDA));

    assertThat(repetido.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(mapeador.readTree(repetido.body()).get("codigo").asText())
        .isEqualTo("CORREO_YA_REGISTRADO");
  }

  @Test
  void trataComoRepetidoUnCorreoQueSoloCambiaEnMayusculasOEspacios() {
    navegador.post(RUTA, cuerpo("Primera Persona", "duplicada@moica.test", CLAVE_VALIDA));

    HttpResponse<String> variante =
        navegador.post(RUTA, cuerpo("Segunda Persona", " Duplicada@MOICA.test ", CLAVE_VALIDA));

    assertThat(variante.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
  }

  @Test
  void rechazaUnaContrasenaSinSimbolo() throws Exception {
    HttpResponse<String> respuesta =
        navegador.post(RUTA, cuerpo("Persona Confiada", "confiada@moica.test", "Moica2026clave"));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(camposConError(respuesta)).contains("clave");
    assertThat(mapeador.readTree(respuesta.body()).get("codigo").asText()).isEqualTo("VALIDACION");
  }

  @Test
  void rechazaUnaContrasenaDemasiadoCorta() throws Exception {
    HttpResponse<String> respuesta =
        navegador.post(RUTA, cuerpo("Persona Breve", "breve@moica.test", "Mo1$ab"));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(camposConError(respuesta)).contains("clave");
  }

  @Test
  void rechazaUnCorreoConFormatoInvalido() throws Exception {
    HttpResponse<String> respuesta =
        navegador.post(RUTA, cuerpo("Persona Apurada", "esto-no-es-un-correo", CLAVE_VALIDA));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(camposConError(respuesta)).contains("correoElectronico");
  }

  @Test
  void rechazaUnCuerpoSinLosDatosObligatorios() throws Exception {
    HttpResponse<String> respuesta = navegador.post(RUTA, Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(camposConError(respuesta)).contains("nombreCompleto", "correoElectronico", "clave");
  }

  @Test
  void rechazaUnCuerpoQueNiSiquieraEsJson() throws Exception {
    HttpResponse<String> respuesta = navegador.post(RUTA, "{ esto no es json");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(mapeador.readTree(respuesta.body()).get("mensaje").asText()).isNotBlank();
  }

  @Test
  void aceptaUnaContrasenaDeLosSetentaYDosCaracteresPermitidos() {
    String clave = "Moica2026$" + "a".repeat(62);

    HttpResponse<String> respuesta =
        navegador.post(RUTA, cuerpo("Persona Precavida", "larga@moica.test", clave));

    assertThat(clave).hasSize(72);
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
  }

  @Test
  void rechazaConUnErrorClaroLaContrasenaQueNoCabeEnBcrypt() throws Exception {
    // 72 caracteres que en UTF-8 ocupan 90 bytes. BCrypt no admite más de 72
    // bytes: la petición debe quedarse en una validación, no en un fallo del
    // servidor.
    String clave = "Añ1$".repeat(18);

    HttpResponse<String> respuesta =
        navegador.post(RUTA, cuerpo("Persona Acentuada", "acentos@moica.test", clave));

    assertThat(clave).hasSize(72);
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(camposConError(respuesta)).contains("clave");
  }

  @Test
  void noRegistraSinElTokenCsrf() {
    HttpResponse<String> respuesta =
        navegador.postSinTokenCsrf(RUTA, cuerpo("Sin Token", "sintoken@moica.test", CLAVE_VALIDA));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());

    Integer cuentas =
        jdbc.queryForObject(
            "SELECT count(*) FROM usuario WHERE correo_electronico = ?",
            Integer.class,
            "sintoken@moica.test");
    assertThat(cuentas).isZero();
  }

  private String camposConError(HttpResponse<String> respuesta) throws Exception {
    JsonNode errores = mapeador.readTree(respuesta.body()).get("errores");
    assertThat(errores).as("un error de validación debe indicar qué campos fallaron").isNotNull();
    return errores.toString();
  }

  private static Map<String, Object> cuerpo(String nombre, String correo, String clave) {
    Map<String, Object> datos = new HashMap<>();
    datos.put("nombreCompleto", nombre);
    datos.put("correoElectronico", correo);
    datos.put("clave", clave);
    return datos;
  }
}
