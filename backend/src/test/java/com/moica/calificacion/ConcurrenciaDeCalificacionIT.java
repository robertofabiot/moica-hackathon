package com.moica.calificacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
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
 * Qué pasa cuando la misma persona envía dos calificaciones a la vez.
 *
 * <p>La comprobación previa no basta: dos transacciones simultáneas pueden leer las dos que todavía
 * no hay calificación y las dos intentar insertar. Quien decide es {@code
 * uq_calificacion_usuario_solicitud_calificador}, y lo que se comprueba aquí es que el envío
 * perdedor salga como 409 controlado y no como 500.
 */
class ConcurrenciaDeCalificacionIT extends EscenarioDeCalificacion {

  private static final long ESPERA_MAXIMA_MS = 30_000;

  private ExecutorService hilos;
  private long idSolicitud;

  @BeforeEach
  void prepararConcurrencia() {
    idSolicitud = solicitudCompletada();
    hilos = Executors.newCachedThreadPool();
  }

  @AfterEach
  void cerrarHilos() {
    hilos.shutdownNow();
  }

  /**
   * El caso que la comprobación previa no puede cubrir, forzado paso a paso.
   *
   * <p>Una transacción externa inserta la calificación del cliente y no confirma. La petición pasa
   * entonces su comprobación previa —la fila sin confirmar todavía no es visible— y se queda
   * esperando en el índice único. Al confirmar la transacción externa, el envío choca contra la
   * restricción real.
   */
  @Test
  @Timeout(180)
  void unSegundoEnvioQueChocaConLaUnicidadRespondeConflictoYNoErrorInterno() throws Exception {
    Connection adelantada = adelantarLaCalificacionDelCliente();
    Future<HttpResponse<String>> envio;
    try {
      envio = hilos.submit(() -> calificar(cliente, idSolicitud, 4, "Muy bien."));
      esperarAQueSeBloqueen(1);
    } finally {
      confirmar(adelantada);
    }

    HttpResponse<String> respuesta = envio.get();

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CALIFICACION_DUPLICADA");
    assertThat(calificacionesEnBase(idSolicitud)).isEqualTo(1);
    assertThat(puntuacionesEnBase()).containsExactly(5);
  }

  /** Dos envíos disparados a la vez desde el mismo navegador: solo uno puede persistir. */
  @Test
  @Timeout(180)
  void dosEnviosSimultaneosDelMismoParticipanteDejanUnaSolaFila() throws Exception {
    CyclicBarrier salida = new CyclicBarrier(2);

    Future<HttpResponse<String>> primero = enCola(salida, 5);
    Future<HttpResponse<String>> segundo = enCola(salida, 1);

    List<Integer> estados = List.of(primero.get().statusCode(), segundo.get().statusCode());

    assertThat(estados)
        .as("un envío se confirma y el otro es un conflicto controlado")
        .containsExactlyInAnyOrder(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());
    assertThat(calificacionesEnBase(idSolicitud)).isEqualTo(1);
  }

  private Future<HttpResponse<String>> enCola(CyclicBarrier salida, int puntuacion) {
    Callable<HttpResponse<String>> peticion =
        () -> {
          salida.await();
          return calificar(cliente, idSolicitud, puntuacion);
        };
    return hilos.submit(peticion);
  }

  private List<Integer> puntuacionesEnBase() {
    return jdbc.queryForList(
        "SELECT puntuacion FROM calificacion_usuario WHERE id_solicitud_servicio = ?",
        Integer.class,
        idSolicitud);
  }

  private Connection adelantarLaCalificacionDelCliente() throws SQLException {
    DataSource origen = Objects.requireNonNull(jdbc.getDataSource());
    Connection conexion = origen.getConnection();
    try {
      conexion.setAutoCommit(false);
      try (PreparedStatement insercion =
          conexion.prepareStatement(
              """
              INSERT INTO calificacion_usuario
                  (id_solicitud_servicio, id_calificador, id_calificado,
                   rol_calificado, puntuacion)
              VALUES (?, ?, ?, 'PRESTADOR', 5)
              """)) {
        insercion.setLong(1, idSolicitud);
        insercion.setLong(2, idDe(CORREO_CLIENTE));
        insercion.setLong(3, idDe(CORREO));
        insercion.executeUpdate();
      }
      return conexion;
    } catch (SQLException | RuntimeException fallo) {
      conexion.close();
      throw fallo;
    }
  }

  private void confirmar(Connection adelantada) throws SQLException {
    try (Connection cerrable = adelantada) {
      cerrable.commit();
    }
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
