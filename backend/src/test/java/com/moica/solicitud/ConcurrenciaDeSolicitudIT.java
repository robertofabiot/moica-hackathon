package com.moica.solicitud;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpStatus;

/**
 * Qué pasa cuando dos acciones tocan la misma solicitud a la vez.
 *
 * <p>Sin bloquear la fila, aceptar y cancelar podrían leer el mismo {@code PENDIENTE} y dejar dos
 * transiciones incompatibles. Aquí el orden lo fija la prueba reteniendo la solicitud con {@code
 * FOR UPDATE} y soltándola cuando ambas peticiones ya esperan.
 */
class ConcurrenciaDeSolicitudIT extends EscenarioDeSolicitud {

  private static final long ESPERA_MAXIMA_MS = 30_000;

  private NavegadorDePrueba cliente;
  private ExecutorService hilos;
  private long idSolicitud;

  @BeforeEach
  void prepararConcurrencia() {
    long idServicio = publicarServicioActivo();
    cliente = clienteAutenticado();
    idSolicitud = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    hilos = Executors.newCachedThreadPool();
  }

  @AfterEach
  void cerrarHilos() {
    hilos.shutdownNow();
  }

  @Test
  @Timeout(180)
  void aceptarYCancelarNoDejanEstadoEHistorialDivergentes() throws Exception {
    Future<HttpResponse<String>> aceptacion;
    Future<HttpResponse<String>> cancelacion;

    Connection retencion = retenerLaSolicitud();
    try {
      aceptacion = enCola(() -> aceptar(navegador, idSolicitud));
      esperarAQueSeBloqueen(1);
      cancelacion = enCola(() -> cancelar(cliente, idSolicitud));
      esperarAQueSeBloqueen(2);
    } finally {
      soltar(retencion);
    }

    int aceptada = aceptacion.get().statusCode();
    int cancelada = cancelacion.get().statusCode();

    // Si gana la aceptación, cancelar sin motivo ya no es una transición: pide
    // motivo (400). Si gana la cancelación, aceptar choca con el estado (409).
    assertThat(List.of(aceptada, cancelada)).contains(HttpStatus.OK.value());
    assertThat(List.of(aceptada, cancelada))
        .containsAnyOf(HttpStatus.CONFLICT.value(), HttpStatus.BAD_REQUEST.value());
    assertThat(estadoActualEnBase(idSolicitud)).isEqualTo(ultimoEstadoDelHistorial(idSolicitud));
    assertThat(cambiosRegistrados(idSolicitud)).isEqualTo(2);
    assertThat(estadoActualEnBase(idSolicitud)).isIn("ACEPTADA", "CANCELADA");
  }

  @Test
  @Timeout(180)
  void dosAceptacionesSimultaneasSoloDejanUnaTransicion() throws Exception {
    NavegadorDePrueba otraSesion = abrirNavegador();
    assertThat(iniciarSesion(otraSesion, CORREO, CLAVE).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    Future<HttpResponse<String>> primera;
    Future<HttpResponse<String>> segunda;

    Connection retencion = retenerLaSolicitud();
    try {
      primera = enCola(() -> aceptar(navegador, idSolicitud));
      esperarAQueSeBloqueen(1);
      segunda = enCola(() -> aceptar(otraSesion, idSolicitud));
      esperarAQueSeBloqueen(2);
    } finally {
      soltar(retencion);
    }

    assertThat(primera.get().statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(segunda.get().statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(segunda.get())).isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(estadoActualEnBase(idSolicitud)).isEqualTo("ACEPTADA");
    assertThat(ultimoEstadoDelHistorial(idSolicitud)).isEqualTo("ACEPTADA");
    assertThat(cambiosRegistrados(idSolicitud)).isEqualTo(2);
  }

  private Connection retenerLaSolicitud() throws SQLException {
    DataSource origen = Objects.requireNonNull(jdbc.getDataSource());
    Connection conexion = origen.getConnection();
    try {
      conexion.setAutoCommit(false);
      try (PreparedStatement retencion =
          conexion.prepareStatement(
              "SELECT id_solicitud_servicio FROM solicitud_servicio"
                  + " WHERE id_solicitud_servicio = ? FOR UPDATE")) {
        retencion.setLong(1, idSolicitud);
        retencion.executeQuery().close();
      }
      return conexion;
    } catch (SQLException | RuntimeException fallo) {
      conexion.close();
      throw fallo;
    }
  }

  private void soltar(Connection retencion) throws SQLException {
    try (Connection cerrable = retencion) {
      cerrable.rollback();
    }
  }

  private Future<HttpResponse<String>> enCola(Callable<HttpResponse<String>> peticion) {
    return hilos.submit(peticion);
  }

  private void esperarAQueSeBloqueen(int cuantas) throws InterruptedException {
    long limite = System.nanoTime() + ESPERA_MAXIMA_MS * 1_000_000L;
    List<String> esperando = consultasBloqueadas();

    while (esperando.size() != cuantas && System.nanoTime() < limite) {
      Thread.sleep(20);
      esperando = consultasBloqueadas();
    }

    assertThat(esperando)
        .as("peticiones esperando un bloqueo tras %d ms", ESPERA_MAXIMA_MS)
        .hasSize(cuantas);
  }

  private List<String> consultasBloqueadas() {
    return jdbc.queryForList(
        """
        SELECT query FROM pg_stat_activity
        WHERE datname = current_database()
          AND wait_event_type = 'Lock'
          AND pid <> pg_backend_pid()
        """,
        String.class);
  }
}
