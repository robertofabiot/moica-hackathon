package com.moica.verificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Qué pasa cuando dos transacciones tocan el mismo perfil a la vez.
 *
 * <p>Bloquear solo la solicitud no bastaba. Aprobar una profesional y revocar la básica son filas
 * distintas de {@code solicitud_verificacion_prestador}, así que ambas transacciones avanzaban en
 * paralelo, leían el mismo {@code nivel_verificacion} antiguo y la última en escribir borraba la
 * decisión de la otra. Lo mismo entre el propietario editando sus datos y la administración
 * resolviendo un expediente: comparten fila, no comparten bloqueo, y la escritura completa de la
 * fila devolvía el valor que el otro acababa de cambiar. La fila de {@code perfil_prestador} es lo
 * único que todas comparten, y tomarla antes de nada es lo que las ordena.
 *
 * <h2>Cómo se hace determinista</h2>
 *
 * <p>Lanzar dos peticiones a la vez y confiar en que coincidan no demuestra nada: el resultado
 * cambia entre ejecuciones y una prueba así pasa incluso con el defecto presente. Aquí el orden lo
 * fija la prueba:
 *
 * <ol>
 *   <li>Una conexión ajena a la aplicación retiene la fila del perfil con {@code SELECT … FOR
 *       UPDATE} y no la suelta.
 *   <li>Se lanza la primera petición y se espera a que PostgreSQL confirme que está bloqueada.
 *   <li>Se lanza la segunda y se espera a que también lo esté.
 *   <li>Se suelta la retención. PostgreSQL despierta a los que esperan en el orden en que llegaron,
 *       así que la primera petición lanzada es la primera que resuelve.
 * </ol>
 *
 * <p>La espera se comprueba contra {@code pg_stat_activity}, no con pausas: cada paso avanza cuando
 * la base de datos dice que la petición está detenida. Sin el bloqueo estas transacciones también
 * acaban encolándose —al escribir—, así que la coordinación funciona igual en las dos versiones del
 * código; lo que cambia es el resultado, porque sin el bloqueo cada una ya leyó el perfil antes de
 * esperar y trabaja con un dato viejo. Por eso cada prueba afirma sobre el estado final de {@code
 * perfil_prestador} y {@code solicitud_verificacion_prestador}, que es donde se ve la incoherencia.
 *
 * <p>{@link #cadaEscrituraDelPerfilLoBloqueaAntesDeLeerlo()} añade la comprobación estructural que
 * las demás no hacen: que la consulta detenida sea la **lectura** del perfil. Es la diferencia
 * entre serializar la escritura, que no basta, y leer ya bajo el bloqueo, que es lo que hace que el
 * dato no pueda ser viejo.
 */
class ConcurrenciaDeVerificacionIT extends EscenarioDeVerificacion {

  private static final String MOTIVO = "El documento presentado resultó no ser auténtico.";
  private static final String NOMBRE_EDITADO = "Taller La Esperanza — Servicio 24 horas";

  /** Tope de la espera activa. Generoso: solo salta si algo no llegó nunca a bloquearse. */
  private static final long ESPERA_MAXIMA_MS = 30_000;

  private NavegadorDePrueba admin;
  private ExecutorService hilos;

  @BeforeEach
  void prepararConcurrencia() {
    admin = administradora(CORREO_ADMIN);
    hilos = Executors.newCachedThreadPool();
  }

  @AfterEach
  void cerrarHilos() {
    hilos.shutdownNow();
  }

  // --- Dos decisiones administrativas sobre solicitudes distintas del mismo perfil -------------

  @Test
  @Timeout(180)
  void siLaAprobacionProfesionalEntraPrimeroLaRevocacionBasicaTambienLaAnula() throws Exception {
    long basica = aprobarBasica(admin);
    long profesional = enviarProfesionalYTomarla();

    Future<HttpResponse<String>> aprobacion;
    Future<HttpResponse<String>> revocacion;

    Connection retencion = retenerElPerfil();
    try {
      aprobacion = enCola(() -> aprobar(profesional));
      esperarAQueSeBloqueen(1);
      revocacion = enCola(() -> revocar(basica));
      esperarAQueSeBloqueen(2);
    } finally {
      soltar(retencion);
    }

    assertThat(aprobacion.get().statusCode())
        .as("la aprobación llegó primero y no la estorba nadie")
        .isEqualTo(HttpStatus.OK.value());
    assertThat(revocacion.get().statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(estadoDeLaSolicitud(basica)).isEqualTo("REVOCADA");
    assertThat(estadoDeLaSolicitud(profesional))
        .as("la revocación de la básica ve la profesional recién aprobada y la anula con ella")
        .isEqualTo("REVOCADA");
    assertThat(nivelDelPerfil()).isEqualTo("SIN_VERIFICAR");
  }

  @Test
  @Timeout(180)
  void siLaRevocacionBasicaEntraPrimeroLaAprobacionProfesionalFalla() throws Exception {
    long basica = aprobarBasica(admin);
    long profesional = enviarProfesionalYTomarla();

    Future<HttpResponse<String>> revocacion;
    Future<HttpResponse<String>> aprobacion;

    Connection retencion = retenerElPerfil();
    try {
      revocacion = enCola(() -> revocar(basica));
      esperarAQueSeBloqueen(1);
      aprobacion = enCola(() -> aprobar(profesional));
      esperarAQueSeBloqueen(2);
    } finally {
      soltar(retencion);
    }

    assertThat(revocacion.get().statusCode()).isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> rechazada = aprobacion.get();
    assertThat(rechazada.statusCode())
        .as("la básica ya no está vigente, así que la profesional no puede concederse")
        .isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(rechazada)).isEqualTo("VERIFICACION_BASICA_REQUERIDA");

    assertThat(estadoDeLaSolicitud(basica)).isEqualTo("REVOCADA");
    assertThat(estadoDeLaSolicitud(profesional))
        .as("la aprobación que falla no deja nada a medias")
        .isEqualTo("EN_REVISION");
    assertThat(nivelDelPerfil()).isEqualTo("SIN_VERIFICAR");
  }

  // --- El propietario editando mientras la administración decide ------------------------------

  @Test
  @Timeout(180)
  void laEdicionDelPropietarioNoDevuelveElPerfilAUnNivelYaSuperado() throws Exception {
    long basica = enviarBasicaCorrecta();
    tomar(basica);

    Future<HttpResponse<String>> aprobacion;
    Future<HttpResponse<String>> edicion;

    Connection retencion = retenerElPerfil();
    try {
      aprobacion = enCola(() -> aprobar(basica));
      esperarAQueSeBloqueen(1);
      edicion = enCola(this::editarElNombrePublico);
      esperarAQueSeBloqueen(2);
    } finally {
      soltar(retencion);
    }

    assertThat(aprobacion.get().statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(edicion.get().statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(nivelDelPerfil())
        .as("la edición del propietario no reescribe el nivel que acaba de decidirse")
        .isEqualTo("VERIFICADO_BASICO");
    assertThat(nombrePublicoDelPerfil()).isEqualTo(NOMBRE_EDITADO);
    assertThat(estadoDeLaSolicitud(basica)).isEqualTo("APROBADA");
  }

  @Test
  @Timeout(180)
  void laDecisionAdministrativaNoPisaLoQueElPropietarioAcabaDeEditar() throws Exception {
    long basica = enviarBasicaCorrecta();
    tomar(basica);

    Future<HttpResponse<String>> edicion;
    Future<HttpResponse<String>> aprobacion;

    Connection retencion = retenerElPerfil();
    try {
      edicion = enCola(this::editarElNombrePublico);
      esperarAQueSeBloqueen(1);
      aprobacion = enCola(() -> aprobar(basica));
      esperarAQueSeBloqueen(2);
    } finally {
      soltar(retencion);
    }

    assertThat(edicion.get().statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(aprobacion.get().statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(nombrePublicoDelPerfil())
        .as("aprobar no devuelve los datos editables a como estaban")
        .isEqualTo(NOMBRE_EDITADO);
    assertThat(nivelDelPerfil()).isEqualTo("VERIFICADO_BASICO");
  }

  @Test
  @Timeout(180)
  void laEdicionDelPropietarioNoRevivaUnaVerificacionRevocada() throws Exception {
    long basica = aprobarBasica(admin);

    Future<HttpResponse<String>> revocacion;
    Future<HttpResponse<String>> edicion;

    Connection retencion = retenerElPerfil();
    try {
      revocacion = enCola(() -> revocar(basica));
      esperarAQueSeBloqueen(1);
      edicion = enCola(this::editarElNombrePublico);
      esperarAQueSeBloqueen(2);
    } finally {
      soltar(retencion);
    }

    assertThat(revocacion.get().statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(edicion.get().statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(nivelDelPerfil())
        .as("una insignia revocada no vuelve porque el propietario guarde su perfil")
        .isEqualTo("SIN_VERIFICAR");
    assertThat(nombrePublicoDelPerfil()).isEqualTo(NOMBRE_EDITADO);
    assertThat(estadoDeLaSolicitud(basica)).isEqualTo("REVOCADA");
  }

  // --- Todas las escrituras del perfil pasan por el mismo bloqueo ------------------------------

  @Test
  @Timeout(180)
  void cadaEscrituraDelPerfilLoBloqueaAntesDeLeerlo() throws Exception {
    comprobarQueEsperaLeyendo("actualizar el perfil", this::editarElNombrePublico);
    comprobarQueEsperaLeyendo(
        "cambiar la disponibilidad",
        () -> navegador.put(RUTA_DISPONIBILIDAD, Map.of("disponibilidad", "NO_DISPONIBLE")));
    comprobarQueEsperaLeyendo(
        "sustituir la imagen",
        () -> navegador.putArchivo(RUTA_IMAGEN, "foto.png", "image/png", imagenPng()));
  }

  /**
   * Comprueba que una operación se queda esperando el perfil **mientras lo lee**.
   *
   * <p>Es la diferencia entre estar arreglado y parecerlo. Cualquier escritura acaba encolándose
   * contra la fila retenida, aunque sea al hacer el {@code UPDATE}; lo que demuestra que el dato
   * con el que trabaja no puede ser viejo es que la consulta que espera sea la lectura.
   */
  private void comprobarQueEsperaLeyendo(
      String descripcion, Callable<HttpResponse<String>> operacion) throws Exception {

    Future<HttpResponse<String>> enCurso;
    List<String> esperando;

    Connection retencion = retenerElPerfil();
    try {
      enCurso = enCola(operacion);
      esperando = esperarAQueSeBloqueen(1);
    } finally {
      soltar(retencion);
    }

    assertThat(esperando)
        .as("al %s, la consulta detenida ante el perfil retenido", descripcion)
        .singleElement()
        .satisfies(
            consulta -> {
              assertThat(consulta.toLowerCase())
                  .as("espera leyendo el perfil, no escribiéndolo")
                  .startsWith("select");
              assertThat(consulta).contains("perfil_prestador");
            });

    assertThat(enCurso.get().statusCode()).as(descripcion).isEqualTo(HttpStatus.OK.value());
  }

  // --- Utilidades de coordinación --------------------------------------------------------------

  /**
   * Retiene la fila del perfil desde fuera de la aplicación y no la suelta.
   *
   * <p>Es la salida de la carrera: todo lo que quiera escribir ese perfil se queda esperando aquí,
   * y la prueba decide en qué orden llegan a la cola.
   */
  private Connection retenerElPerfil() throws SQLException {
    DataSource origen = Objects.requireNonNull(jdbc.getDataSource());
    Connection conexion = origen.getConnection();
    try {
      conexion.setAutoCommit(false);
      try (PreparedStatement retencion =
          conexion.prepareStatement(
              "SELECT id_prestador FROM perfil_prestador WHERE id_prestador = ? FOR UPDATE")) {
        retencion.setLong(1, idDe(CORREO));
        retencion.executeQuery().close();
      }
      return conexion;
    } catch (SQLException | RuntimeException fallo) {
      conexion.close();
      throw fallo;
    }
  }

  /** Suelta la retención y deja que PostgreSQL despierte a quienes esperaban, en su orden. */
  private void soltar(Connection retencion) throws SQLException {
    try (Connection cerrable = retencion) {
      cerrable.rollback();
    }
  }

  private Future<HttpResponse<String>> enCola(Callable<HttpResponse<String>> peticion) {
    return hilos.submit(peticion);
  }

  /**
   * Espera a que haya exactamente {@code cuantas} peticiones detenidas ante la fila retenida.
   *
   * <p>No hay pausas fijas: se avanza cuando PostgreSQL confirma la espera, y por eso el orden de
   * la cola es el orden en que se lanzaron. Si algo no llega nunca a bloquearse, la prueba falla
   * diciendo qué esperaba en vez de quedarse colgada.
   *
   * <p>Aquí no se mira **en qué** consulta esperan: eso se comprueba aparte. Una transacción sin el
   * bloqueo también acaba encolándose —al escribir— y esta espera debe dejarla llegar hasta el
   * final, porque lo que la delata es el estado incoherente que deja, no la forma de esperar.
   *
   * @return las consultas detenidas en ese instante
   */
  private List<String> esperarAQueSeBloqueen(int cuantas) throws InterruptedException {
    long limite = System.nanoTime() + ESPERA_MAXIMA_MS * 1_000_000L;
    List<String> esperando = consultasBloqueadas();

    while (esperando.size() != cuantas && System.nanoTime() < limite) {
      Thread.sleep(20);
      esperando = consultasBloqueadas();
    }

    assertThat(esperando)
        .as("peticiones esperando un bloqueo tras %d ms", ESPERA_MAXIMA_MS)
        .hasSize(cuantas);
    return esperando;
  }

  /** Las consultas que ahora mismo están detenidas esperando un bloqueo de fila. */
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

  // --- Atajos del escenario --------------------------------------------------------------------

  /** Envía una profesional sobre una básica ya vigente y la deja tomada, lista para resolver. */
  private long enviarProfesionalYTomarla() {
    long profesional =
        json(enviarExpediente("PROFESIONAL", certificado()))
            .get("idSolicitudVerificacion")
            .asLong();
    tomar(profesional);
    return profesional;
  }

  private void tomar(long idSolicitud) {
    assertThat(admin.post(RUTA_REVISION + "/" + idSolicitud + "/toma", Map.of()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
  }

  private HttpResponse<String> aprobar(long idSolicitud) {
    return admin.post(RUTA_REVISION + "/" + idSolicitud + "/aprobacion", Map.of());
  }

  private HttpResponse<String> revocar(long idSolicitud) {
    return admin.post(
        RUTA_REVISION + "/" + idSolicitud + "/revocacion", Map.of("observacion", MOTIVO));
  }

  private HttpResponse<String> editarElNombrePublico() {
    Map<String, Object> datos = new HashMap<>(solicitudDePerfil());
    datos.put("nombrePublico", NOMBRE_EDITADO);
    return navegador.put(RUTA_PERFIL, datos);
  }

  private String nombrePublicoDelPerfil() {
    return jdbc.queryForObject(
        "SELECT nombre_publico FROM perfil_prestador WHERE id_prestador = ?",
        String.class,
        idDe(CORREO));
  }
}
