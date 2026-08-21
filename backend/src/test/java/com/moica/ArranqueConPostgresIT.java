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
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Comprueba que la aplicación arranca contra un PostgreSQL real, que Flyway aplica sobre esa base
 * las migraciones versionadas de Moica y que el healthcheck refleja el estado de la conexión.
 *
 * <p>Desde P2 el directorio de migraciones ya no está vacío, así que estas pruebas trabajan sobre
 * el esquema real y no sobre una migración de demostración.
 */
class ArranqueConPostgresIT extends PruebaDeIntegracionConPostgres {

  @Autowired private DataSource origenDeDatos;

  @LocalServerPort private int puerto;

  @Test
  void arrancaConectadoAPostgresqlReal() throws Exception {
    try (Connection conexion = origenDeDatos.getConnection();
        Statement sentencia = conexion.createStatement();
        ResultSet resultado = sentencia.executeQuery("SELECT version()")) {

      assertThat(resultado.next()).isTrue();
      assertThat(resultado.getString(1)).contains("PostgreSQL");
    }
  }

  @Test
  void flywayAplicaLasMigracionesDeMoicaYRegistraSuHistorial() throws Exception {
    try (Connection conexion = origenDeDatos.getConnection();
        Statement sentencia = conexion.createStatement();
        ResultSet resultado =
            sentencia.executeQuery(
                "SELECT success, description FROM flyway_schema_history WHERE version = '10'")) {

      assertThat(resultado.next())
          .as("Flyway debe registrar cada migración aplicada en su historial")
          .isTrue();
      assertThat(resultado.getBoolean("success")).isTrue();
      assertThat(resultado.getString("description")).contains("usuario");
    }
  }

  @Test
  void elEsquemaMigradoTieneLasTablasDeIdentidad() throws Exception {
    try (Connection conexion = origenDeDatos.getConnection();
        Statement sentencia = conexion.createStatement();
        ResultSet resultado =
            sentencia.executeQuery(
                """
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name IN ('usuario', 'sesion')
                ORDER BY table_name
                """)) {

      assertThat(resultado.next()).isTrue();
      assertThat(resultado.getString(1)).isEqualTo("sesion");
      assertThat(resultado.next()).isTrue();
      assertThat(resultado.getString(1)).isEqualTo("usuario");
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
