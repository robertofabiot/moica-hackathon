package com.moica.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpStatus;

/**
 * Qué pasa cuando dos personas administradoras actúan sobre el mismo caso a la vez.
 *
 * <p>La comprobación de estado no basta por sí sola: dos transacciones simultáneas podrían leer el
 * mismo estado de partida y creer las dos que su transición es válida. Quien las ordena es el
 * bloqueo pesimista que toma cada mutación antes de leer, y lo que se comprueba aquí es que la
 * perdedora salga como un conflicto controlado y que el historial no quede con dos versiones
 * vigentes ni con periodos superpuestos.
 */
class ConcurrenciaDeRevisionDeCasosIT extends EscenarioDeRevisionDeCasos {

  private ExecutorService hilos;
  private NavegadorDePrueba primera;
  private NavegadorDePrueba segunda;

  @BeforeEach
  void prepararConcurrencia() {
    primera = administradora(CORREO_ADMIN);
    segunda = administradora(CORREO_OTRO_ADMIN);
    hilos = Executors.newCachedThreadPool();
  }

  @AfterEach
  void cerrarHilos() {
    hilos.shutdownNow();
  }

  @Test
  @Timeout(180)
  @DisplayName("Dos asignaciones simultáneas dejan un solo responsable y una sola versión vigente")
  void dosAsignacionesSimultaneasNoDuplicanLaVersion() throws Exception {
    long idCaso = casoAbierto();
    CyclicBarrier salida = new CyclicBarrier(2);

    Future<HttpResponse<String>> unaAsignacion =
        hilos.submit(
            () -> conSalidaComun(salida, () -> asignar(primera, idCaso, idDe(CORREO_ADMIN))));
    Future<HttpResponse<String>> otraAsignacion =
        hilos.submit(
            () -> conSalidaComun(salida, () -> asignar(segunda, idCaso, idDe(CORREO_OTRO_ADMIN))));

    // Las dos son válidas —reasignar es coordinación— así que las dos responden
    // 200. Lo que importa es que se serializaron: el bloqueo impide que ambas
    // cierren la misma versión y dejen dos vigentes.
    assertThat(unaAsignacion.get().statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(otraAsignacion.get().statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(versionesVigentes(idCaso)).isEqualTo(1);
    assertThat(vigenciasSuperpuestas(idCaso)).isZero();
    assertThat(versionesEnBase(idCaso)).isEqualTo(3);
    assertThat(numerosDeVersion(idCaso)).containsExactly(1, 2, 3);

    // El responsable que quedó es el de la que escribió última, y la versión
    // vigente lo dice: la fila del caso y su fotografía no se contradicen.
    assertThat(versionActual(idCaso).get("id_administrador_responsable"))
        .isEqualTo(casoEnBase(idCaso).get("id_administrador_responsable"));
  }

  @Test
  @Timeout(180)
  @DisplayName("Dos cierres simultáneos del mismo responsable dejan una sola resolución")
  void dosCierresSimultaneosDejanUnaSolaResolucion() throws Exception {
    long idCaso = casoEnRevisionDe(primera, CORREO_ADMIN);
    CyclicBarrier salida = new CyclicBarrier(2);

    Future<HttpResponse<String>> unCierre =
        hilos.submit(() -> conSalidaComun(salida, () -> cerrar(primera, idCaso, "PROCEDENTE")));
    Future<HttpResponse<String>> otroCierre =
        hilos.submit(() -> conSalidaComun(salida, () -> cerrar(primera, idCaso, "DESESTIMADO")));

    List<Integer> estados =
        List.of(unCierre.get().statusCode(), otroCierre.get().statusCode()).stream()
            .sorted()
            .toList();

    // Uno gana; el otro llega cuando el caso ya no está EN_REVISION.
    assertThat(estados).containsExactly(HttpStatus.OK.value(), HttpStatus.CONFLICT.value());

    Map<String, Object> caso = casoEnBase(idCaso);
    assertThat(caso.get("estado_actual")).isEqualTo("CERRADO");
    assertThat(caso.get("resultado_actual")).isIn("PROCEDENTE", "DESESTIMADO");

    assertThat(versionesVigentes(idCaso)).isEqualTo(1);
    assertThat(vigenciasSuperpuestas(idCaso)).isZero();
    // Apertura, asignación, inicio de revisión y un único cierre.
    assertThat(versionesEnBase(idCaso)).isEqualTo(4);
    assertThat(versionActual(idCaso).get("resultado_caso")).isEqualTo(caso.get("resultado_actual"));
  }

  @Test
  @Timeout(180)
  @DisplayName("Una reasignación simultánea no deja resolver a quien acaba de perder el caso")
  void reasignarMientrasSeResuelveNoDejaUnaDecisionSinDueno() throws Exception {
    long idCaso = casoEnRevisionDe(primera, CORREO_ADMIN);
    CyclicBarrier salida = new CyclicBarrier(2);

    Future<HttpResponse<String>> cierre =
        hilos.submit(() -> conSalidaComun(salida, () -> cerrar(primera, idCaso, "PROCEDENTE")));
    Future<HttpResponse<String>> reasignacion =
        hilos.submit(
            () -> conSalidaComun(salida, () -> asignar(segunda, idCaso, idDe(CORREO_OTRO_ADMIN))));

    HttpResponse<String> respuestaDelCierre = cierre.get();
    HttpResponse<String> respuestaDeLaReasignacion = reasignacion.get();

    // Los dos órdenes posibles son correctos; lo que no puede quedar es una
    // decisión firmada por quien ya no llevaba el caso.
    if (respuestaDelCierre.statusCode() == HttpStatus.OK.value()) {
      // El cierre llegó primero: la decisión existe y la reasignación se topa
      // con un caso ya cerrado, que no se reasigna.
      assertThat(respuestaDeLaReasignacion.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
      assertThat(codigoDeError(respuestaDeLaReasignacion)).isEqualTo("TRANSICION_NO_PERMITIDA");
      assertThat(casoEnBase(idCaso).get("estado_actual")).isEqualTo("CERRADO");
      assertThat(casoEnBase(idCaso).get("resultado_actual")).isEqualTo("PROCEDENTE");
      assertThat(casoEnBase(idCaso).get("id_administrador_responsable"))
          .isEqualTo(idDe(CORREO_ADMIN));
    } else {
      // La reasignación ganó: quien iba a cerrar ya no es el responsable y su
      // decisión no llega a escribirse.
      assertThat(respuestaDeLaReasignacion.statusCode()).isEqualTo(HttpStatus.OK.value());
      assertThat(respuestaDelCierre.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
      assertThat(codigoDeError(respuestaDelCierre)).isEqualTo("CASO_DE_OTRO_ADMINISTRADOR");
      assertThat(casoEnBase(idCaso).get("estado_actual")).isEqualTo("EN_REVISION");
      assertThat(casoEnBase(idCaso).get("resultado_actual")).isNull();
      assertThat(casoEnBase(idCaso).get("id_administrador_responsable"))
          .isEqualTo(idDe(CORREO_OTRO_ADMIN));
    }

    assertThat(versionesVigentes(idCaso)).isEqualTo(1);
    assertThat(vigenciasSuperpuestas(idCaso)).isZero();
  }

  /** Los números de versión que quedaron, para comprobar que la secuencia no tiene huecos. */
  private List<Integer> numerosDeVersion(long idCaso) {
    return jdbc.queryForList(
        "SELECT numero_version FROM historial_caso WHERE id_caso_moderacion = ?"
            + " ORDER BY numero_version",
        Integer.class,
        idCaso);
  }

  /** Suelta las dos peticiones en el mismo instante, para que se crucen de verdad. */
  private HttpResponse<String> conSalidaComun(
      CyclicBarrier salida, java.util.concurrent.Callable<HttpResponse<String>> peticion)
      throws Exception {
    salida.await();
    return peticion.call();
  }
}
