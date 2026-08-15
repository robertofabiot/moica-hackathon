package com.moica;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Comprueba que la aplicación arranca contra un PostgreSQL real, que Flyway toma el control del
 * esquema y que el healthcheck refleja el estado de esa conexión.
 *
 * <p>Se usa Testcontainers y no H2: solo PostgreSQL real demuestra restricciones {@code CHECK},
 * índices parciales y exclusiones temporales, que el diccionario de datos sí utiliza.
 *
 * <p>Necesita Docker en ejecución. La ejecuta {@code ./mvnw verify}.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      // Testcontainers entrega la conexión real mediante @ServiceConnection; estos
      // valores solo evitan que las variables de entorno del despliegue queden sin
      // resolver al construir el contexto.
      "MOICA_DB_NOMBRE=moica_prueba",
      "MOICA_DB_USUARIO=moica_prueba",
      "MOICA_DB_CLAVE=moica_prueba",
      // El detalle de salud está cerrado en producción; aquí se abre para poder
      // afirmar que el componente de base de datos es el que responde.
      "management.endpoint.health.show-details=always",
      "management.endpoint.health.show-components=always"
    })
@Testcontainers
class ArranqueConPostgresIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

  @Autowired private DataSource dataSource;

  @LocalServerPort private int puerto;

  @Test
  void arrancaConectadoAPostgresqlReal() throws Exception {
    try (Connection conexion = dataSource.getConnection();
        Statement sentencia = conexion.createStatement();
        ResultSet resultado = sentencia.executeQuery("SELECT version()")) {

      assertThat(resultado.next()).isTrue();
      assertThat(resultado.getString(1)).contains("PostgreSQL");
    }
  }

  @Test
  void flywayTomaElControlDelEsquema() throws Exception {
    try (Connection conexion = dataSource.getConnection();
        Statement sentencia = conexion.createStatement();
        ResultSet resultado =
            sentencia.executeQuery(
                "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL")) {

      assertThat(resultado.next()).isTrue();
      assertThat(resultado.getBoolean(1))
          .as("Flyway debe registrar su historial de migraciones al arrancar")
          .isTrue();
    }
  }

  @Test
  void elHealthcheckReportaLaBaseDeDatosDisponible() throws Exception {
    HttpRequest peticion =
        HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/actuator/health"))
            .GET()
            .build();

    HttpResponse<String> respuesta;
    try (HttpClient cliente = HttpClient.newHttpClient()) {
      respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    assertThat(respuesta.statusCode()).isEqualTo(200);
    assertThat(respuesta.body()).contains("\"status\":\"UP\"").contains("\"db\"");
  }
}
