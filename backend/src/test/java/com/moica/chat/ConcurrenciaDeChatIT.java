package com.moica.chat;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Qué pasa cuando alguien escribe justo mientras la solicitud se cierra.
 *
 * <p>Sin bloquear la fila, enviar y cancelar podrían leer el mismo {@code ACEPTADA} y dejar un
 * mensaje confirmado *después* de que el hilo quedó en solo lectura. Solo dos desenlaces son
 * válidos: o el mensaje se confirma antes de la transición, o la transición gana y el envío se
 * rechaza.
 *
 * <p>El orden lo fija la prueba reteniendo la solicitud con {@code FOR UPDATE} y soltándola cuando
 * ambas peticiones ya esperan, igual que en {@code ConcurrenciaDeSolicitudIT}.
 */
class ConcurrenciaDeChatIT extends EscenarioDeChat {

  private static final long ESPERA_MAXIMA_MS = 30_000;

  private ExecutorService hilos;
  private long idSolicitud;

  @BeforeEach
  void prepararConcurrencia() {
    idSolicitud = solicitudAceptada();
    hilos = Executors.newCachedThreadPool();
  }

  @AfterEach
  void cerrarHilos() {
    hilos.shutdownNow();
  }

  @Test
  @Timeout(180)
  void enviarYCancelarNoDejanUnMensajePosteriorAlCierre() throws Exception {
    Future<HttpResponse<String>> envio;
    Future<HttpResponse<String>> cancelacion;

    Connection retencion = retenerLaSolicitud();
    try {
      envio = enCola(() -> enviarMensaje(cliente, idSolicitud, "¿Seguimos con el martes?"));
      esperarAQueSeBloqueen(1);
      cancelacion =
          enCola(() -> cancelarConMotivo(navegador, idSolicitud, "Me surgió un imprevisto."));
      esperarAQueSeBloqueen(2);
    } finally {
      soltar(retencion);
    }

    int enviado = envio.get().statusCode();

    assertThat(cancelacion.get().statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(estadoActualEnBase(idSolicitud)).isEqualTo("CANCELADA");
    assertThat(enviado)
        .as("el envío o se confirma antes del cierre o se rechaza")
        .isIn(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());

    if (enviado == HttpStatus.CREATED.value()) {
      assertThat(mensajesEnBase(idSolicitud)).isEqualTo(1);
    } else {
      assertThat(codigoDeError(envio.get())).isEqualTo("CHAT_SOLO_LECTURA");
      assertThat(mensajesEnBase(idSolicitud)).isZero();
    }
    assertThat(mensajesPosterioresAlCierre("CANCELADA")).isZero();
  }

  @Test
  @Timeout(180)
  void enviarYCompletarNoDejanUnMensajePosteriorAlCierre() throws Exception {
    Future<HttpResponse<String>> envio;
    Future<HttpResponse<String>> completado;

    Connection retencion = retenerLaSolicitud();
    try {
      envio =
          enCola(() -> enviarMensaje(cliente, idSolicitud, "Falta revisar la llave del patio."));
      esperarAQueSeBloqueen(1);
      completado = enCola(() -> completar(navegador, idSolicitud));
      esperarAQueSeBloqueen(2);
    } finally {
      soltar(retencion);
    }

    int enviado = envio.get().statusCode();

    assertThat(completado.get().statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(estadoActualEnBase(idSolicitud)).isEqualTo("COMPLETADA");
    assertThat(enviado).isIn(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());
    assertThat(mensajesEnBase(idSolicitud))
        .isEqualTo(enviado == HttpStatus.CREATED.value() ? 1 : 0);
    assertThat(mensajesPosterioresAlCierre("COMPLETADA")).isZero();
  }

  /** Cuántos mensajes quedaron con un instante posterior al de la transición final. */
  private int mensajesPosterioresAlCierre(String estadoFinal) {
    Integer posteriores =
        jdbc.queryForObject(
            """
            SELECT count(*) FROM mensaje_solicitud m
            WHERE m.id_solicitud_servicio = ?
              AND m.fecha_envio > (
                SELECT c.fecha_cambio FROM cambio_estado_solicitud c
                WHERE c.id_solicitud_servicio = m.id_solicitud_servicio
                  AND c.estado_nuevo = ?
              )
            """,
            Integer.class,
            idSolicitud,
            estadoFinal);
    assertThat(posteriores).isNotNull();
    return posteriores;
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
