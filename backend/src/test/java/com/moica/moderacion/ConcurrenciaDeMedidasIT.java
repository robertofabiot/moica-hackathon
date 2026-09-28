package com.moica.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.moderacion.service.ExpiracionDeMedidas;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Qué pasa cuando dos decisiones sobre la <b>misma cuenta</b> se cruzan.
 *
 * <p>Es la carrera que el bloqueo del caso no puede arbitrar: dos expedientes distintos de la misma
 * persona son filas distintas, así que sin bloquear la cuenta las dos transacciones leerían que no
 * hay ninguna medida vigente y las dos la aplicarían. Lo que se comprueba aquí es que eso no ocurra
 * ni siquiera con las dos peticiones saliendo en el mismo instante, que la perdedora salga como un
 * conflicto controlado y no como un 500, y que el historial no quede con dos versiones vigentes ni
 * con periodos superpuestos.
 *
 * <p>La red final es {@code uq_caso_moderacion_medida_vigente_por_cuenta}, el índice único parcial
 * de {@code V52}: aunque el bloqueo fallara, PostgreSQL seguiría sin admitir dos medidas vigentes
 * sobre la misma cuenta.
 */
class ConcurrenciaDeMedidasIT extends EscenarioDeMedidas {

  @Autowired private ExpiracionDeMedidas barrido;
  @Autowired private DataSource origenDeDatos;

  private ExecutorService hilos;
  private NavegadorDePrueba admin;

  @BeforeEach
  void prepararConcurrencia() {
    admin = administradora(CORREO_ADMIN);
    hilos = Executors.newCachedThreadPool();
  }

  @AfterEach
  void cerrarHilos() {
    hilos.shutdownNow();
  }

  private OffsetDateTime dentroDeUnaSemana() {
    return OffsetDateTime.now().plusDays(7);
  }

  @Test
  @Timeout(180)
  @DisplayName("Dos medidas simultáneas desde expedientes distintos no dejan dos vigentes")
  void dosAplicacionesSimultaneasNoDejanDosMedidasVigentes() throws Exception {
    long primero = casoProcedenteDe(admin, CORREO_ADMIN);
    long segundo = otroCasoProcedenteDe(admin, CORREO_ADMIN);
    short restriccion = medidaDeRestriccion();
    short suspension = medidaDeSuspension();
    CyclicBarrier salida = new CyclicBarrier(2);

    Future<HttpResponse<String>> una =
        hilos.submit(
            () ->
                conSalidaComun(
                    salida, () -> aplicarMedida(admin, primero, restriccion, dentroDeUnaSemana())));
    Future<HttpResponse<String>> otra =
        hilos.submit(
            () ->
                conSalidaComun(
                    salida, () -> aplicarMedida(admin, segundo, suspension, dentroDeUnaSemana())));

    List<Integer> estados =
        List.of(una.get().statusCode(), otra.get().statusCode()).stream().sorted().toList();

    // Una gana; la otra encuentra la cuenta ya sancionada y exige confirmar el
    // reemplazo. Ninguna de las dos revienta con un 500.
    assertThat(estados).containsExactly(HttpStatus.OK.value(), HttpStatus.CONFLICT.value());

    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
    assertThat(estadoDeCuentaEnBase(CORREO)).isIn("RESTRINGIDA_TEMPORAL", "SUSPENDIDA_TEMPORAL");
    assertThat(versionesVigentes(primero)).isEqualTo(1);
    assertThat(versionesVigentes(segundo)).isEqualTo(1);
    assertThat(vigenciasSuperpuestas(primero)).isZero();
    assertThat(vigenciasSuperpuestas(segundo)).isZero();
  }

  @Test
  @Timeout(180)
  @DisplayName("Dos reemplazos confirmados a la vez tampoco dejan dos medidas vigentes")
  void dosReemplazosConfirmadosSimultaneosSeSerializan() throws Exception {
    long primero = casoProcedenteDe(admin, CORREO_ADMIN);
    long segundo = otroCasoProcedenteDe(admin, CORREO_ADMIN);
    short restriccion = medidaDeRestriccion();
    short suspension = medidaDeSuspension();
    CyclicBarrier salida = new CyclicBarrier(2);

    Future<HttpResponse<String>> una =
        hilos.submit(
            () ->
                conSalidaComun(
                    salida,
                    () -> aplicarMedida(admin, primero, restriccion, dentroDeUnaSemana(), true)));
    Future<HttpResponse<String>> otra =
        hilos.submit(
            () ->
                conSalidaComun(
                    salida,
                    () -> aplicarMedida(admin, segundo, suspension, dentroDeUnaSemana(), true)));

    // Las dos son válidas por separado: confirmar el reemplazo autoriza a
    // sustituir lo que hubiera. Se serializan y la última manda.
    assertThat(una.get().statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(otra.get().statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
    assertThat(versionesVigentes(primero)).isEqualTo(1);
    assertThat(versionesVigentes(segundo)).isEqualTo(1);
    assertThat(vigenciasSuperpuestas(primero)).isZero();
    assertThat(vigenciasSuperpuestas(segundo)).isZero();

    // La fila del caso y su fotografía no se contradicen.
    assertThat(versionActual(primero).get("id_medida_administrativa"))
        .isEqualTo(casoEnBase(primero).get("id_medida_administrativa_actual"));
    assertThat(versionActual(segundo).get("id_medida_administrativa"))
        .isEqualTo(casoEnBase(segundo).get("id_medida_administrativa_actual"));
  }

  @Test
  @Timeout(180)
  @DisplayName("Revocar mientras se aplica otra medida deja la cuenta coherente")
  void revocarYAplicarALaVezDejanLaCuentaCoherente() throws Exception {
    long primero = casoProcedenteDe(admin, CORREO_ADMIN);
    long segundo = otroCasoProcedenteDe(admin, CORREO_ADMIN);
    short restriccion = medidaDeRestriccion();
    short suspension = medidaDeSuspension();

    assertThat(aplicarMedida(admin, primero, restriccion, dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    CyclicBarrier salida = new CyclicBarrier(2);
    Future<HttpResponse<String>> revocacion =
        hilos.submit(() -> conSalidaComun(salida, () -> revocarMedida(admin, primero)));
    Future<HttpResponse<String>> aplicacion =
        hilos.submit(
            () ->
                conSalidaComun(
                    salida,
                    () -> aplicarMedida(admin, segundo, suspension, dentroDeUnaSemana(), true)));

    HttpResponse<String> respuestaDeLaRevocacion = revocacion.get();

    // El reemplazo siempre sale adelante: confirmarlo autoriza a sustituir lo
    // que hubiera. La revocación depende del orden, y las dos respuestas son
    // correctas.
    assertThat(aplicacion.get().statusCode()).isEqualTo(HttpStatus.OK.value());

    if (respuestaDeLaRevocacion.statusCode() == HttpStatus.CONFLICT.value()) {
      // El reemplazo llegó primero y ya le había quitado la medida al primer
      // expediente, así que no queda nada que revocar. Es un conflicto
      // controlado, no un fallo.
      assertThat(codigoDeError(respuestaDeLaRevocacion)).isEqualTo("SIN_MEDIDA_VIGENTE");
      assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("SUSPENDIDA_TEMPORAL");
      assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
      assertThat(casoEnBase(segundo).get("id_medida_administrativa_actual")).isNotNull();
    } else {
      // La revocación llegó primero: dejó la cuenta activa y el reemplazo
      // aplicó su suspensión encima, sin encontrar nada que sustituir.
      assertThat(respuestaDeLaRevocacion.statusCode()).isEqualTo(HttpStatus.OK.value());
      assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("SUSPENDIDA_TEMPORAL");
      assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
      assertThat(casoEnBase(primero).get("id_medida_administrativa_actual")).isNull();
    }

    // En los dos órdenes, lo que no puede quedar son dos medidas vigentes ni un
    // estado de cuenta que contradiga a la que quedó.
    assertThat(versionesVigentes(primero)).isEqualTo(1);
    assertThat(versionesVigentes(segundo)).isEqualTo(1);
    assertThat(vigenciasSuperpuestas(primero)).isZero();
    assertThat(vigenciasSuperpuestas(segundo)).isZero();
  }

  @Test
  @Timeout(180)
  @DisplayName("El barrido de expiración y una revocación simultánea no se pisan")
  void expirarYRevocarALaVezNoDuplicanElEvento() throws Exception {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(
            aplicarMedida(admin, idCaso, medidaDeRestriccion(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    vencerLaMedidaDe(idCaso);

    int versionesAntes = versionesEnBase(idCaso);
    CyclicBarrier salida = new CyclicBarrier(2);

    Future<HttpResponse<String>> revocacion =
        hilos.submit(() -> conSalidaComun(salida, () -> revocarMedida(admin, idCaso)));
    Future<Integer> expiracion =
        hilos.submit(
            () -> {
              salida.await();
              return barrido.expirarLasVencidas();
            });

    HttpResponse<String> respuestaDeLaRevocacion = revocacion.get();
    int expiradas = expiracion.get();

    // Solo uno de los dos levanta la medida: el otro llega y ya no hay nada.
    if (respuestaDeLaRevocacion.statusCode() == HttpStatus.OK.value()) {
      assertThat(expiradas).isZero();
      assertThat(eventosEnOrden(idCaso)).endsWith("MEDIDA_REVOCADA");
    } else {
      assertThat(respuestaDeLaRevocacion.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
      assertThat(codigoDeError(respuestaDeLaRevocacion)).isEqualTo("SIN_MEDIDA_VIGENTE");
      assertThat(expiradas).isEqualTo(1);
      assertThat(eventosEnOrden(idCaso)).endsWith("MEDIDA_EXPIRADA");
    }

    assertThat(versionesEnBase(idCaso)).isEqualTo(versionesAntes + 1);
    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
    assertThat(medidasVigentesDe(CORREO)).isZero();
    comprobarCadenaScd2(idCaso);
  }

  @Test
  @Timeout(180)
  @DisplayName("El barrido de expiración y un reemplazo simultáneo dejan una sola medida")
  void expirarYReemplazarALaVezDejanUnaSolaMedida() throws Exception {
    long primero = casoProcedenteDe(admin, CORREO_ADMIN);
    long segundo = otroCasoProcedenteDe(admin, CORREO_ADMIN);
    short suspension = medidaDeSuspension();

    assertThat(
            aplicarMedida(admin, primero, medidaDeRestriccion(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    vencerLaMedidaDe(primero);

    CyclicBarrier salida = new CyclicBarrier(2);
    Future<HttpResponse<String>> reemplazo =
        hilos.submit(
            () ->
                conSalidaComun(
                    salida,
                    () -> aplicarMedida(admin, segundo, suspension, dentroDeUnaSemana(), true)));
    Future<Integer> expiracion =
        hilos.submit(
            () -> {
              salida.await();
              return barrido.expirarLasVencidas();
            });

    assertThat(reemplazo.get().statusCode()).isEqualTo(HttpStatus.OK.value());
    expiracion.get();

    // Gane quien gane el orden, la suspensión del segundo caso es la vigente y
    // la del primero ya no está.
    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
    assertThat(casoEnBase(primero).get("id_medida_administrativa_actual")).isNull();
    assertThat(casoEnBase(segundo).get("id_medida_administrativa_actual"))
        .isEqualTo((int) suspension);
    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("SUSPENDIDA_TEMPORAL");
    comprobarCadenaScd2(primero);
    comprobarCadenaScd2(segundo);
  }

  @Test
  @Timeout(180)
  @DisplayName("Un reemplazo no deshace lo que el expediente anterior cambió mientras esperaba")
  void elReemplazoLeeElExpedienteAnteriorYaBloqueado() throws Exception {
    long primero = casoProcedenteDe(admin, CORREO_ADMIN);
    long segundo = otroCasoProcedenteDe(admin, CORREO_ADMIN);
    short suspension = medidaDeSuspension();
    assertThat(
            aplicarMedida(admin, primero, medidaDeRestriccion(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    // Otra operación sobre el expediente anterior —una reapertura, que solo
    // bloquea esa fila— lo tiene tomado cuando llega el reemplazo, y confirma
    // mientras el reemplazo espera. Sin barreras ni sueños: el orden es exacto.
    Future<HttpResponse<String>> reemplazo;
    try (Connection otra = origenDeDatos.getConnection()) {
      otra.setAutoCommit(false);
      ejecutar(
          otra, "SELECT 1 FROM caso_moderacion WHERE id_caso_moderacion = ? FOR UPDATE", primero);

      reemplazo =
          hilos.submit(() -> aplicarMedida(admin, segundo, suspension, dentroDeUnaSemana(), true));
      esperarAQueAlguienEspereUnBloqueo(jdbc);

      ejecutar(
          otra,
          """
          UPDATE caso_moderacion
          SET estado_actual = 'REABIERTO', resultado_actual = NULL,
              resolucion_actual = NULL, fecha_cierre_actual = NULL
          WHERE id_caso_moderacion = ?
          """,
          primero);
      otra.commit();
    }

    assertThat(reemplazo.get().statusCode()).isEqualTo(HttpStatus.OK.value());

    // La reapertura sobrevive: el reemplazo leyó el expediente ya bloqueado y
    // solo le retiró la medida.
    assertThat(casoEnBase(primero).get("estado_actual")).isEqualTo("REABIERTO");
    assertThat(casoEnBase(primero).get("id_medida_administrativa_actual")).isNull();
    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
  }

  @Test
  @Timeout(180)
  @DisplayName("El barrido fecha la expiración cuando ya tiene el caso, no cuando empezó")
  void elBarridoFechaLaExpiracionTrasBloquearElCaso() throws Exception {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(
            aplicarMedida(admin, idCaso, medidaDeRestriccion(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    vencerLaMedidaDe(idCaso);

    // Mientras el barrido espera el caso, otra transacción podría versionarlo
    // con un instante posterior al inicio de la pasada. Si la expiración usara
    // ese inicio, su cierre quedaría antes de la versión que cierra.
    Future<Integer> pasada;
    Instant liberado;
    try (Connection otra = origenDeDatos.getConnection()) {
      otra.setAutoCommit(false);
      ejecutar(
          otra, "SELECT 1 FROM caso_moderacion WHERE id_caso_moderacion = ? FOR UPDATE", idCaso);

      pasada = hilos.submit(barrido::expirarLasVencidas);
      esperarAQueAlguienEspereUnBloqueo(jdbc);

      liberado = Instant.now().truncatedTo(ChronoUnit.MICROS);
      otra.commit();
    }

    assertThat(pasada.get()).isEqualTo(1);
    Map<String, Object> version = versionActual(idCaso);
    assertThat(version.get("tipo_evento")).isEqualTo("MEDIDA_EXPIRADA");
    assertThat(((Timestamp) version.get("fecha_inicio_vigencia")).toInstant())
        .isAfterOrEqualTo(liberado);
    comprobarCadenaScd2(idCaso);
  }

  private static void ejecutar(Connection conexion, String sql, long idCaso) throws SQLException {
    try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
      sentencia.setLong(1, idCaso);
      sentencia.execute();
    }
  }

  /** Suelta las dos peticiones en el mismo instante, para que se crucen de verdad. */
  private HttpResponse<String> conSalidaComun(
      CyclicBarrier salida, Callable<HttpResponse<String>> peticion) throws Exception {
    salida.await();
    return peticion.call();
  }
}
