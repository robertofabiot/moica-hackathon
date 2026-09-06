package com.moica.moderacion;

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
 * Qué pasa cuando la misma persona envía dos reportes a la vez.
 *
 * <p>La comprobación previa no basta: dos transacciones simultáneas pueden leer las dos que todavía
 * no hay caso y las dos intentar insertarlo. Quien decide es {@code
 * uq_caso_moderacion_solicitud_reportante}, y lo que se comprueba aquí es que el envío perdedor
 * salga como 409 controlado y no como 500.
 *
 * <p>Sirve además como prueba de la atomicidad: el envío que pierde no deja ni el caso ni una
 * versión huérfana, porque las dos filas viven en la misma transacción.
 */
class ConcurrenciaDeReporteIT extends EscenarioDeModeracion {

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

  /**
   * El caso que la comprobación previa no puede cubrir, forzado paso a paso.
   *
   * <p>Una transacción externa inserta el caso del cliente y no confirma. La petición pasa entonces
   * su comprobación previa —la fila sin confirmar todavía no es visible— y se queda esperando en el
   * índice único. Al confirmar la transacción externa, el envío choca contra la restricción real.
   */
  @Test
  @Timeout(180)
  void unSegundoReporteQueChocaConLaUnicidadRespondeConflictoYNoErrorInterno() throws Exception {
    Connection adelantada = adelantarElReporteDelCliente();
    Future<HttpResponse<String>> envio;
    try {
      envio = hilos.submit(() -> reportar(cliente, idSolicitud));
      esperarAQueSeBloqueen(1);
    } finally {
      confirmar(adelantada);
    }

    HttpResponse<String> respuesta = envio.get();

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("REPORTE_DUPLICADO");
    assertThat(casosEnBase(idSolicitud)).isEqualTo(1);
    assertThat(motivosEnBase()).containsExactly("Adelantado por otra transacción");
    // El caso que ganó lo insertó la transacción externa, que no crea historial:
    // lo importante es que el envío perdedor no dejó ninguna versión suelta.
    assertThat(versionesTotales()).isZero();
  }

  /** Dos envíos disparados a la vez desde el mismo navegador: solo uno puede persistir. */
  @Test
  @Timeout(180)
  void dosEnviosSimultaneosDelMismoParticipanteDejanUnSoloCasoYUnaSolaVersion() throws Exception {
    CyclicBarrier salida = new CyclicBarrier(2);

    Future<HttpResponse<String>> primero = enCola(salida, "Trato irrespetuoso");
    Future<HttpResponse<String>> segundo = enCola(salida, "Incumplimiento");

    List<Integer> estados = List.of(primero.get().statusCode(), segundo.get().statusCode());

    assertThat(estados)
        .as("un envío se confirma y el otro es un conflicto controlado")
        .containsExactlyInAnyOrder(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());
    assertThat(casosEnBase(idSolicitud)).isEqualTo(1);
    assertThat(versionesTotales()).isEqualTo(1);
  }

  /**
   * Los dos participantes reportando a la vez no se estorban.
   *
   * <p>La unicidad es por pareja solicitud–reportante, así que cada lado abre el suyo: la solicitud
   * termina con dos casos y dos versiones iniciales, una por caso.
   */
  @Test
  @Timeout(180)
  void losDosParticipantesReportandoALaVezAbrenSuPropioCaso() throws Exception {
    CyclicBarrier salida = new CyclicBarrier(2);

    Future<HttpResponse<String>> delCliente =
        hilos.submit(
            () -> {
              salida.await();
              return reportar(cliente, idSolicitud);
            });
    Future<HttpResponse<String>> delPrestador =
        hilos.submit(
            () -> {
              salida.await();
              return reportar(navegador, idSolicitud);
            });

    assertThat(delCliente.get().statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(delPrestador.get().statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(casosEnBase(idSolicitud)).isEqualTo(2);
    assertThat(versionesTotales()).isEqualTo(2);
  }

  private Future<HttpResponse<String>> enCola(CyclicBarrier salida, String motivo) {
    Callable<HttpResponse<String>> peticion =
        () -> {
          salida.await();
          return reportar(cliente, idSolicitud, motivo, DESCRIPCION);
        };
    return hilos.submit(peticion);
  }

  private List<String> motivosEnBase() {
    return jdbc.queryForList(
        "SELECT motivo FROM caso_moderacion WHERE id_solicitud_servicio = ?",
        String.class,
        idSolicitud);
  }

  private Integer versionesTotales() {
    return jdbc.queryForObject("SELECT count(*) FROM historial_caso", Integer.class);
  }

  private Connection adelantarElReporteDelCliente() throws SQLException {
    DataSource origen = Objects.requireNonNull(jdbc.getDataSource());
    Connection conexion = origen.getConnection();
    try {
      conexion.setAutoCommit(false);
      try (PreparedStatement insercion =
          conexion.prepareStatement(
              """
              INSERT INTO caso_moderacion
                  (id_solicitud_servicio, id_reportante, id_reportado, motivo, descripcion)
              VALUES (?, ?, ?, 'Adelantado por otra transacción', 'Hechos ya reportados.')
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
