package com.moica;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Fija el contrato del healthcheck que el perfil {@code prod} publica a cualquiera.
 *
 * <p>Existe porque el estado agregado es lo único de Actuator que atraviesa Nginx, y basta una
 * propiedad para que Spring empiece a añadirle datos. Los grupos de sondas se activan solos cuando
 * la plataforma parece orquestada, y entonces la respuesta pasa a anunciar {@code liveness} y
 * {@code readiness}; {@code application-prod.properties} los desactiva de forma explícita para que
 * el cuerpo no dependa de dónde se despliegue.
 *
 * <p>La segunda prueba es la negativa: ninguna otra superficie de Actuator responde sin
 * autenticación al propio backend. Nginx además las devuelve como 404, pero esa barrera se
 * comprueba en {@code scripts/smoke-produccion.mjs}; aquí se demuestra que el backend no depende de
 * ella.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "MOICA_DB_NOMBRE=moica_prueba",
      "MOICA_DB_USUARIO=moica_prueba",
      "MOICA_DB_CLAVE=moica_prueba",
      "MOICA_JWT_SECRETO=" + PruebaDeIntegracionConPostgres.SECRETO_JWT,
      "MOICA_TOTP_CLAVE_CIFRADO=" + PruebaDeIntegracionConPostgres.CLAVE_DE_CIFRADO_TOTP,
      // El perfil prod exige ambas: sin ellas ConfiguracionDeProduccion aborta el arranque.
      "MOICA_COOKIE_SEGURA=true",
      "MOICA_SOPORTE_CANAL=soporte-de-prueba@example.org",
      // En producción se escucha en todas las interfaces; la prueba se limita a loopback.
      "server.address=127.0.0.1"
    })
@ActiveProfiles("prod")
class SaludPublicaEnProduccionIT extends PruebaDeIntegracionConPostgres {

  @LocalServerPort private int puerto;

  private HttpResponse<String> pedir(String ruta) throws Exception {
    HttpRequest peticion =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + ruta)).GET().build();
    try (HttpClient cliente = HttpClient.newHttpClient()) {
      return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }
  }

  @Test
  void elHealthPublicoDevuelveSoloElEstadoAgregado() throws Exception {
    HttpResponse<String> respuesta = pedir("/actuator/health");

    assertThat(respuesta.statusCode()).isEqualTo(200);
    assertThat(respuesta.body())
        .as("el healthcheck público no describe componentes, detalles ni grupos de sondas")
        .isEqualTo("{\"status\":\"UP\"}");
  }

  @Test
  void noPublicaNingunaOtraSuperficieDeActuator() throws Exception {
    for (String ruta :
        new String[] {
          "/actuator",
          "/actuator/env",
          "/actuator/beans",
          "/actuator/configprops",
          "/actuator/metrics",
          "/actuator/loggers",
          "/actuator/mappings",
          "/actuator/threaddump",
          "/actuator/heapdump",
          "/actuator/info",
          "/actuator/health/liveness",
          "/actuator/health/readiness"
        }) {

      HttpResponse<String> respuesta = pedir(ruta);

      assertThat(respuesta.statusCode())
          .as("%s no puede responder a quien no ha iniciado sesión", ruta)
          .isEqualTo(401);
    }
  }
}
