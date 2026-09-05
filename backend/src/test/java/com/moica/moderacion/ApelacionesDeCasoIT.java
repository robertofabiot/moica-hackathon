package com.moica.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * El registro administrativo de apelaciones y la reapertura del expediente, de extremo a extremo.
 *
 * <p>Cubre lo que fija el criterio de salida de P10B para las apelaciones: se reciben fuera de
 * Moica y se registran aquí, se aceptan o se rechazan, aceptar una puede reabrir <b>el mismo</b>
 * expediente, y el historial conserva la resolución anterior en lugar de reescribirla.
 *
 * <p>También comprueba lo contrario: que <b>no existe ninguna vía por la que la persona sancionada
 * apele dentro de la aplicación</b>. La decisión D-MOD-04 lo excluye del MVP.
 */
class ApelacionesDeCasoIT extends EscenarioDeMedidas {

  private NavegadorDePrueba admin;

  @BeforeEach
  void prepararAdministradora() {
    admin = administradora(CORREO_ADMIN);
  }

  /** Un caso cerrado como procedente y con una apelación ya registrada. */
  private long casoConApelacion() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(registrarApelacion(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    return idCaso;
  }

  // --- No hay apelación de usuario ---------------------------------------

  @Test
  @DisplayName("La persona reportada no tiene ninguna ruta para apelar dentro de Moica")
  void laPersonaSancionadaNoApelaDesdeLaAplicacion() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);

    // El prestador es justo la persona reportada, y aun así no llega.
    assertThat(registrarApelacion(navegador, idCaso).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(resolverApelacion(navegador, idCaso, true).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(reabrir(navegador, idCaso).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());

    assertThat(registrarApelacion(abrirNavegador(), idCaso).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());

    assertThat(eventosEnOrden(idCaso)).doesNotContain("APELACION_PRESENTADA");
  }

  @Test
  @DisplayName("Solo quien tiene el caso asignado registra y resuelve su apelación")
  void soloElResponsableGestionaLaApelacion() {
    NavegadorDePrueba otra = administradora(CORREO_OTRO_ADMIN);
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);

    HttpResponse<String> ajena = registrarApelacion(otra, idCaso);

    assertThat(ajena.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(ajena)).isEqualTo("CASO_DE_OTRO_ADMINISTRADOR");
    assertThat(eventosEnOrden(idCaso)).doesNotContain("APELACION_PRESENTADA");
  }

  // --- Registrar ----------------------------------------------------------

  @Test
  @DisplayName("Registrar deja el relato en el historial a nombre de quien lo registró")
  void registrarDejaConstanciaDeLoRecibido() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);

    HttpResponse<String> registrada = registrarApelacion(admin, idCaso);

    assertThat(registrada.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(registrada).get("apelacion").asText()).isEqualTo("PENDIENTE");
    // El caso no se mueve: registrar no reabre nada.
    assertThat(json(registrada).get("caso").get("estadoActual").asText()).isEqualTo("CERRADO");

    Map<String, Object> version = versionActual(idCaso);
    assertThat(version.get("tipo_evento")).isEqualTo("APELACION_PRESENTADA");
    assertThat(version.get("tipo_actor")).isEqualTo("ADMINISTRADOR");
    assertThat(version.get("id_actor")).isEqualTo(idDe(CORREO_ADMIN));
    assertThat((String) version.get("detalle_cambio"))
        .contains("canal externo")
        .contains(RELATO_DE_APELACION);
    comprobarCadenaScd2(idCaso);
  }

  @Test
  @DisplayName("Solo se registra una apelación sobre un caso cerrado")
  void noSeRegistraUnaApelacionSobreUnCasoSinDecision() {
    long enRevision = casoEnRevisionDe(admin, CORREO_ADMIN);

    HttpResponse<String> respuesta = registrarApelacion(admin, enRevision);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(eventosEnOrden(enRevision)).doesNotContain("APELACION_PRESENTADA");
  }

  @Test
  @DisplayName("No se registra una segunda apelación mientras la anterior siga sin resolver")
  void noSeAcumulanDosApelacionesPendientes() {
    long idCaso = casoConApelacion();
    int versionesAntes = versionesEnBase(idCaso);

    HttpResponse<String> segunda = registrarApelacion(admin, idCaso);

    assertThat(segunda.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(segunda)).isEqualTo("APELACION_PENDIENTE");
    assertThat(versionesEnBase(idCaso)).isEqualTo(versionesAntes);
  }

  // --- Resolver -----------------------------------------------------------

  @Test
  @DisplayName("Aceptar la apelación la deja aceptada sin reabrir el caso todavía")
  void aceptarNoReabreElCasoPorSiSolo() {
    long idCaso = casoConApelacion();

    HttpResponse<String> aceptada = resolverApelacion(admin, idCaso, true);

    assertThat(aceptada.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(aceptada).get("apelacion").asText()).isEqualTo("ACEPTADA");
    assertThat(json(aceptada).get("caso").get("estadoActual").asText()).isEqualTo("CERRADO");
    assertThat(versionActual(idCaso).get("tipo_evento")).isEqualTo("APELACION_ACEPTADA");
  }

  @Test
  @DisplayName("Rechazar la apelación mantiene la decisión vigente")
  void rechazarMantieneLaDecision() {
    long idCaso = casoConApelacion();

    HttpResponse<String> rechazada = resolverApelacion(admin, idCaso, false);

    assertThat(rechazada.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(rechazada).get("apelacion").asText()).isEqualTo("RECHAZADA");
    assertThat(json(rechazada).get("caso").get("resultadoActual").asText()).isEqualTo("PROCEDENTE");
    assertThat(json(rechazada).get("resolucionActual").asText()).isEqualTo(RESOLUCION);
    assertThat(versionActual(idCaso).get("tipo_evento")).isEqualTo("APELACION_RECHAZADA");
  }

  @Test
  @DisplayName("Resolver sin apelación pendiente responde 409 y no versiona")
  void noSeResuelveLoQueNadieApelo() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    int versionesAntes = versionesEnBase(idCaso);

    HttpResponse<String> respuesta = resolverApelacion(admin, idCaso, true);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SIN_APELACION_PENDIENTE");
    assertThat(versionesEnBase(idCaso)).isEqualTo(versionesAntes);
  }

  // --- Reabrir ------------------------------------------------------------

  @Test
  @DisplayName("Un caso solo se reabre cuando su apelación fue aceptada")
  void reabrirExigeUnaApelacionAceptada() {
    long sinApelacion = casoProcedenteDe(admin, CORREO_ADMIN);
    HttpResponse<String> sinNada = reabrir(admin, sinApelacion);
    assertThat(sinNada.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(sinNada)).isEqualTo("APELACION_NO_ACEPTADA");

    long pendiente = casoConApelacion();
    HttpResponse<String> sinResolver = reabrir(admin, pendiente);
    assertThat(sinResolver.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(sinResolver)).isEqualTo("APELACION_NO_ACEPTADA");

    assertThat(resolverApelacion(admin, pendiente, false).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    HttpResponse<String> traRechazar = reabrir(admin, pendiente);
    assertThat(traRechazar.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(traRechazar)).isEqualTo("APELACION_NO_ACEPTADA");

    assertThat(casoEnBase(pendiente).get("estado_actual")).isEqualTo("CERRADO");
  }

  @Test
  @DisplayName("Reabrir devuelve el mismo expediente a REABIERTO y retira la decisión vigente")
  void reabrirDevuelveElMismoExpedienteARevision() {
    long idCaso = casoConApelacion();
    assertThat(resolverApelacion(admin, idCaso, true).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> reabierto = reabrir(admin, idCaso);

    assertThat(reabierto.statusCode()).isEqualTo(HttpStatus.OK.value());
    // Es el mismo caso, no otro: no se abre ningún expediente nuevo.
    assertThat(json(reabierto).get("caso").get("idCasoModeracion").asLong()).isEqualTo(idCaso);
    assertThat(json(reabierto).get("caso").get("estadoActual").asText()).isEqualTo("REABIERTO");
    assertThat(json(reabierto).get("caso").get("resultadoActual").isNull()).isTrue();
    assertThat(json(reabierto).get("resolucionActual").isNull()).isTrue();

    Map<String, Object> caso = casoEnBase(idCaso);
    assertThat(caso.get("estado_actual")).isEqualTo("REABIERTO");
    assertThat(caso.get("resultado_actual")).isNull();
    assertThat(caso.get("resolucion_actual")).isNull();
    assertThat(caso.get("fecha_cierre_actual")).isNull();
  }

  @Test
  @DisplayName("La resolución anterior sobrevive en el historial aunque el caso ya no la muestre")
  void elHistorialConservaLaResolucionAnterior() {
    long idCaso = casoConApelacion();
    assertThat(resolverApelacion(admin, idCaso, true).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(reabrir(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());

    List<Map<String, Object>> versiones = versionesEnOrden(idCaso);

    // La versión del cierre sigue guardando resultado y resolución intactos.
    Map<String, Object> cierre =
        versiones.stream()
            .filter(fila -> "RESOLUCION_REGISTRADA".equals(fila.get("tipo_evento")))
            .findFirst()
            .orElseThrow();
    assertThat(cierre.get("resultado_caso")).isEqualTo("PROCEDENTE");
    assertThat(cierre.get("resolucion")).isEqualTo(RESOLUCION);

    // Y la vigente ya no la lleva, porque la decisión dejó de serlo.
    Map<String, Object> vigente = versionActual(idCaso);
    assertThat(vigente.get("tipo_evento")).isEqualTo("CASO_REABIERTO");
    assertThat(vigente.get("estado_caso")).isEqualTo("REABIERTO");
    assertThat(vigente.get("resultado_caso")).isNull();
    assertThat(vigente.get("resolucion")).isNull();
    comprobarCadenaScd2(idCaso);
  }

  @Test
  @DisplayName("Reabrir conserva la medida vigente: volver a mirar el caso no absuelve a nadie")
  void reabrirNoLevantaLaMedida() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(
            aplicarMedida(admin, idCaso, medidaDeRestriccion(), OffsetDateTime.now().plusDays(7))
                .statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(registrarApelacion(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(resolverApelacion(admin, idCaso, true).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> reabierto = reabrir(admin, idCaso);

    assertThat(reabierto.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(reabierto).get("medidaVigente").isNull()).isFalse();
    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("RESTRINGIDA_TEMPORAL");
    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);

    // Y desde el caso reabierto sí se puede levantar, que es lo que la
    // apelación aceptada venía a permitir.
    assertThat(revocarMedida(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
  }

  @Test
  @DisplayName("Reabrir consume la apelación aceptada: no se reabre dos veces con una sola")
  void reabrirConsumeLaApelacionAceptada() {
    long idCaso = casoConApelacion();
    assertThat(resolverApelacion(admin, idCaso, true).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(reabrir(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());

    // Se retoma la revisión y se vuelve a cerrar, como permite el flujo de P10A.
    assertThat(iniciarRevision(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(cerrar(admin, idCaso, "DESESTIMADO").statusCode()).isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> segundaReapertura = reabrir(admin, idCaso);

    assertThat(segundaReapertura.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(segundaReapertura)).isEqualTo("APELACION_NO_ACEPTADA");
    assertThat(json(consultarExpediente(admin, idCaso)).get("apelacion").asText())
        .isEqualTo("SIN_APELACION");
  }

  @Test
  @DisplayName("El recorrido de apelación deja una versión por decisión, encadenadas y sin solapes")
  void elRecorridoDeApelacionMantieneElScd2() {
    long idCaso = casoConApelacion();
    assertThat(resolverApelacion(admin, idCaso, true).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(reabrir(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(iniciarRevision(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(eventosEnOrden(idCaso))
        .containsExactly(
            "CASO_ABIERTO",
            "RESPONSABLE_ASIGNADO",
            "ESTADO_CASO_CAMBIADO",
            "RESOLUCION_REGISTRADA",
            "APELACION_PRESENTADA",
            "APELACION_ACEPTADA",
            "CASO_REABIERTO",
            "ESTADO_CASO_CAMBIADO");
    comprobarCadenaScd2(idCaso);
  }

  @Test
  @DisplayName("Después de reabrir se puede registrar una apelación nueva del ciclo siguiente")
  void unCicloNuevoAdmiteSuPropiaApelacion() {
    long idCaso = casoConApelacion();
    assertThat(resolverApelacion(admin, idCaso, true).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(reabrir(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(iniciarRevision(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(cerrar(admin, idCaso, "PROCEDENTE").statusCode()).isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> nueva = registrarApelacion(admin, idCaso);

    assertThat(nueva.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(nueva).get("apelacion").asText()).isEqualTo("PENDIENTE");
  }
}
