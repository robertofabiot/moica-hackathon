package com.moica.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Cambio de contraseña de extremo a extremo.
 *
 * <p>Lo que se demuestra aquí es que cambiar la contraseña deja fuera a todo el mundo: a la sesión
 * desde la que se hizo el cambio y a las que hubiera abiertas en otros dispositivos.
 */
class CambioDeClaveIT extends EscenarioDeSeguridad {

  private static final String CLAVE_NUEVA = "Moica2026$distinta";

  @BeforeEach
  void iniciarSesionAntesDeCambiar() {
    iniciarSesion(navegador);
  }

  @Test
  void cambiaLaContrasenaYRevocaTodasLasSesionesComoCambioDeCredenciales() {
    HttpResponse<String> respuesta = cambiar(navegador, CLAVE, CLAVE_NUEVA);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(sesionesVigentes()).isZero();
    assertThat(jdbc.queryForObject("SELECT motivo_revocacion FROM sesion", String.class))
        .isEqualTo("CAMBIO_CREDENCIALES");
    assertThat(navegador.cookie(COOKIE_SESION))
        .as("la respuesta caduca la cookie: hay que volver a entrar")
        .isEmpty();
  }

  @Test
  void laPeticionSiguienteAlCambioYaNoAutentica() {
    cambiar(navegador, CLAVE, CLAVE_NUEVA);

    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void dejaFueraTambienALasSesionesAbiertasEnOtrosDispositivos() {
    NavegadorDePrueba otroDispositivo = abrirNavegador();
    iniciarSesion(otroDispositivo);
    assertThat(otroDispositivo.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.OK.value());

    cambiar(navegador, CLAVE, CLAVE_NUEVA);

    assertThat(otroDispositivo.get(RUTA_SESION).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void despuesDelCambioSoloSirveLaContrasenaNueva() {
    cambiar(navegador, CLAVE, CLAVE_NUEVA);

    NavegadorDePrueba otro = abrirNavegador();
    assertThat(iniciarSesion(otro, CORREO, CLAVE).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(iniciarSesion(otro, CORREO, CLAVE_NUEVA).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
  }

  /**
   * La regla completa de la API sobre un solo endpoint, que es donde se puede confundir.
   *
   * <p>Los tres casos piden a quien usa Moica cosas distintas —volver a entrar, resolver lo que le
   * falta a la sesión o escribir bien la contraseña—, así que ninguno debe poder tomarse por otro.
   * El estado separa «ya no hay sesión» de «la hay pero no alcanza para esto»; dentro del 403, el
   * código dice qué es lo que no alcanza.
   */
  @Test
  void separaSinSesionDeSesionQueNoAlcanzaYDeContrasenaEquivocada() {
    NavegadorDePrueba anonimo = abrirNavegador();
    HttpResponse<String> sinSesion = cambiar(anonimo, CLAVE, CLAVE_NUEVA);

    assertThat(sinSesion.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(codigoDeError(sinSesion)).isEqualTo("NO_AUTENTICADO");

    // Una sesión abierta con la contraseña correcta a la que le falta el
    // segundo factor: existe, pero no alcanza para cambiar credenciales.
    activarSegundoFactor(navegador);
    NavegadorDePrueba provisional = abrirNavegador();
    iniciarSesion(provisional);
    HttpResponse<String> sesionQueNoAlcanza = cambiar(provisional, CLAVE, CLAVE_NUEVA);

    assertThat(sesionQueNoAlcanza.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(sesionQueNoAlcanza)).isEqualTo("ACCESO_DENEGADO");

    HttpResponse<String> claveEquivocada = cambiar(navegador, "Moica2026$equivocada", CLAVE_NUEVA);

    assertThat(claveEquivocada.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(claveEquivocada))
        .as("el código es lo que distingue un 403 del otro")
        .isEqualTo("CREDENCIALES_INVALIDAS");
    assertThat(navegador.get(RUTA_SESION).statusCode())
        .as("acertar mal la contraseña no termina la sesión")
        .isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void rechazaElCambioSiLaContrasenaActualNoEsCorrecta() {
    HttpResponse<String> respuesta = cambiar(navegador, "Moica2026$equivocada", CLAVE_NUEVA);

    assertThat(respuesta.statusCode())
        .as("la sesión sigue viva; lo que no se acredita es la propiedad de la cuenta")
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CREDENCIALES_INVALIDAS");
    assertThat(sesionesVigentes()).isEqualTo(1);
    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void noCambiaElHashSiLaContrasenaActualNoEsCorrecta() {
    String hashPrevio = hashGuardado();

    cambiar(navegador, "Moica2026$equivocada", CLAVE_NUEVA);

    assertThat(hashGuardado()).isEqualTo(hashPrevio);
  }

  @Test
  void guardaSoloElHashDeLaContrasenaNueva() {
    String hashPrevio = hashGuardado();

    cambiar(navegador, CLAVE, CLAVE_NUEVA);

    assertThat(hashGuardado())
        .isNotEqualTo(hashPrevio)
        .startsWith("$2")
        .doesNotContain(CLAVE_NUEVA);
  }

  @Test
  void exigeQueLaContrasenaNuevaCumplaLaPolitica() {
    HttpResponse<String> respuesta = cambiar(navegador, CLAVE, "corta");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("VALIDACION");
    assertThat(json(respuesta).get("errores").toString()).contains("claveNueva");
    assertThat(sesionesVigentes()).isEqualTo(1);
  }

  @Test
  void rechazaUnaContrasenaNuevaQueNoCabeEnBcrypt() {
    // 72 caracteres que en UTF-8 ocupan 90 bytes.
    HttpResponse<String> respuesta = cambiar(navegador, CLAVE, "Añ1$".repeat(18));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(json(respuesta).get("errores").toString()).contains("claveNueva");
  }

  @Test
  void noDevuelveNiLaContrasenaNiSuHash() {
    HttpResponse<String> respuesta = cambiar(navegador, CLAVE, CLAVE_NUEVA);

    assertThat(respuesta.body()).doesNotContain(CLAVE_NUEVA).doesNotContain("$2a$");
  }

  @Test
  void noCambiaLaContrasenaSinElTokenCsrf() {
    HttpResponse<String> respuesta =
        navegador.putSinTokenCsrf(
            RUTA_CLAVE, Map.of("claveActual", CLAVE, "claveNueva", CLAVE_NUEVA));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(sesionesVigentes()).isEqualTo(1);
  }

  @Test
  void noPermiteCambiarLaContrasenaSinSesion() {
    NavegadorDePrueba anonimo = abrirNavegador();

    HttpResponse<String> respuesta = cambiar(anonimo, CLAVE, CLAVE_NUEVA);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("NO_AUTENTICADO");
  }

  private HttpResponse<String> cambiar(
      NavegadorDePrueba desde, String claveActual, String claveNueva) {
    return desde.put(RUTA_CLAVE, Map.of("claveActual", claveActual, "claveNueva", claveNueva));
  }

  private String hashGuardado() {
    return jdbc.queryForObject(
        "SELECT clave_hash FROM usuario WHERE correo_electronico = ?", String.class, CORREO);
  }
}
