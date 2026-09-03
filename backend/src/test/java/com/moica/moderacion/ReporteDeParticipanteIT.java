package com.moica.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Quién puede reportar, desde qué solicitudes y una sola vez.
 *
 * <p>Las reglas de participación y de historial de estados se comprueban aquí, sobre la API, porque
 * dependen de la solicitud y no de la tabla. Lo que la base sostiene por su cuenta —participantes
 * distintos, unicidad y coherencia SCD2— se prueba en {@code EsquemaDeCasosDeModeracionIT}.
 */
class ReporteDeParticipanteIT extends EscenarioDeModeracion {

  // --- Quién reporta y a quién --------------------------------------------

  @Test
  void elClienteReportaAlPrestadorYElServidorDerivaAlReportado() {
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> respuesta = reportar(cliente, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(respuesta).get("idReportado").asLong()).isEqualTo(idDe(CORREO));
    assertThat(json(respuesta).get("nombreReportado").asText()).isEqualTo("Taller La Esperanza");
    assertThat(json(respuesta).get("motivo").asText()).isEqualTo(MOTIVO);
    assertThat(json(respuesta).get("descripcion").asText()).isEqualTo(DESCRIPCION);
    assertThat(json(respuesta).get("estadoActual").asText()).isEqualTo("ABIERTO");

    Map<String, Object> caso = casoEnBase(idDeCaso(respuesta));
    assertThat(caso.get("id_reportante")).isEqualTo(idDe(CORREO_CLIENTE));
    assertThat(caso.get("id_reportado")).isEqualTo(idDe(CORREO));
  }

  @Test
  void elPrestadorReportaAlClienteYElServidorDerivaAlReportado() {
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> respuesta = reportar(navegador, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(respuesta).get("idReportado").asLong()).isEqualTo(idDe(CORREO_CLIENTE));
    assertThat(json(respuesta).get("nombreReportado").asText()).isEqualTo("Persona de Prueba");

    Map<String, Object> caso = casoEnBase(idDeCaso(respuesta));
    assertThat(caso.get("id_reportante")).isEqualTo(idDe(CORREO));
    assertThat(caso.get("id_reportado")).isEqualTo(idDe(CORREO_CLIENTE));
  }

  @Test
  void elCuerpoNoPuedeElegirAQuienSeReporta() {
    long idSolicitud = solicitudAceptada();
    cuentaAutenticada(CORREO_TERCERO);
    Map<String, Object> manipulado = new HashMap<>();
    manipulado.put("motivo", MOTIVO);
    manipulado.put("descripcion", DESCRIPCION);
    manipulado.put("idReportado", idDe(CORREO_TERCERO));
    manipulado.put("idReportante", idDe(CORREO));
    manipulado.put("estadoActual", "CERRADO");

    HttpResponse<String> respuesta = reportarCon(cliente, idSolicitud, manipulado);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(respuesta).get("idReportado").asLong()).isEqualTo(idDe(CORREO));
    assertThat(json(respuesta).get("estadoActual").asText()).isEqualTo("ABIERTO");
  }

  @Test
  void cadaParticipanteAbreSuPropioCasoSobreLaMismaSolicitud() {
    long idSolicitud = solicitudAceptada();

    assertThat(reportar(cliente, idSolicitud).statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(reportar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.CREATED.value());

    assertThat(casosEnBase(idSolicitud)).isEqualTo(2);
  }

  // --- Desde qué solicitudes se puede reportar -----------------------------

  @Test
  void seReportaDesdeUnaSolicitudAceptada() {
    long idSolicitud = solicitudAceptada();

    assertThat(reportar(cliente, idSolicitud).statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(casosEnBase(idSolicitud)).isEqualTo(1);
  }

  @Test
  void seReportaDespuesDeCompletarLaSolicitud() {
    long idSolicitud = solicitudCompletada();

    assertThat(reportar(cliente, idSolicitud).statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(casosEnBase(idSolicitud)).isEqualTo(1);
  }

  @Test
  void seReportaDespuesDeCancelarUnaSolicitudQueEstuvoAceptada() {
    long idSolicitud = solicitudCanceladaTrasAceptar();

    assertThat(reportar(cliente, idSolicitud).statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(casosEnBase(idSolicitud)).isEqualTo(1);
  }

  @Test
  void noSeReportaDesdeUnaSolicitudPendiente() {
    long idSolicitud = solicitudPendiente();

    HttpResponse<String> respuesta = reportar(cliente, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SOLICITUD_NO_REPORTABLE");
    assertThat(casosEnBase(idSolicitud)).isZero();
  }

  @Test
  void noSeReportaDesdeUnaSolicitudRechazada() {
    long idSolicitud = solicitudRechazada();

    HttpResponse<String> respuesta = reportar(cliente, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SOLICITUD_NO_REPORTABLE");
    assertThat(casosEnBase(idSolicitud)).isZero();
  }

  @Test
  void noSeReportaDesdeUnaSolicitudCanceladaQueNuncaSeAcepto() {
    long idSolicitud = solicitudCanceladaSinAceptar();

    HttpResponse<String> respuesta = reportar(cliente, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SOLICITUD_NO_REPORTABLE");
    assertThat(casosEnBase(idSolicitud)).isZero();
  }

  // --- Quién no llega al recurso ------------------------------------------

  @Test
  void unTerceroNoDistingueUnaSolicitudAjenaDeUnaInexistente() {
    long idSolicitud = solicitudAceptada();
    var tercero = cuentaAutenticada(CORREO_TERCERO);

    HttpResponse<String> intento = reportar(tercero, idSolicitud);
    assertThat(intento.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(intento)).isEqualTo("RECURSO_NO_ENCONTRADO");

    HttpResponse<String> consulta = leerReporte(tercero, idSolicitud);
    assertThat(consulta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(consulta)).isEqualTo("RECURSO_NO_ENCONTRADO");
    assertThat(casosEnBase(idSolicitud)).isZero();
  }

  @Test
  void unaCuentaRestringidaConservaElReporteYSuConsulta() {
    long idSolicitud = solicitudAceptada();
    restringirCuenta(CORREO_CLIENTE);

    HttpResponse<String> estado = leerReporte(cliente, idSolicitud);
    assertThat(estado.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(estado).get("puedeReportar").asBoolean()).isTrue();

    HttpResponse<String> respuesta = reportar(cliente, idSolicitud);
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(casosEnBase(idSolicitud)).isEqualTo(1);

    HttpResponse<String> despues = leerReporte(cliente, idSolicitud);
    assertThat(despues.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(despues).get("casoAbierto").get("idCasoModeracion").asLong())
        .isEqualTo(idDeCaso(respuesta));
  }

  @Test
  void unaCuentaSuspendidaNoLlegaSiquieraAlRecurso() {
    long idSolicitud = solicitudAceptada();
    suspenderCuenta(CORREO_CLIENTE);

    HttpResponse<String> intento = reportar(cliente, idSolicitud);

    assertThat(intento.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(intento)).isEqualTo("ACCESO_DENEGADO");
    assertThat(leerReporte(cliente, idSolicitud).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(casosEnBase(idSolicitud)).isZero();
  }

  // --- Un solo caso por participante --------------------------------------

  @Test
  void nadieReportaDosVecesLaMismaSolicitud() {
    long idSolicitud = solicitudAceptada();
    assertThat(reportar(cliente, idSolicitud).statusCode()).isEqualTo(HttpStatus.CREATED.value());

    HttpResponse<String> segunda = reportar(cliente, idSolicitud, "Otro motivo", "Otros hechos.");

    assertThat(segunda.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(segunda)).isEqualTo("REPORTE_DUPLICADO");
    assertThat(casosEnBase(idSolicitud)).isEqualTo(1);
  }

  // --- La primera versión del historial ------------------------------------

  @Test
  void laPrimeraVersionSeCreaConLosValoresDeLaApertura() {
    long idSolicitud = solicitudAceptada();
    HttpResponse<String> respuesta = reportar(cliente, idSolicitud);
    long idCaso = idDeCaso(respuesta);

    assertThat(versionesEnBase(idCaso)).isEqualTo(1);

    Map<String, Object> version = versionActual(idCaso);
    assertThat(version.get("numero_version")).isEqualTo(1);
    assertThat(version.get("tipo_actor")).isEqualTo("USUARIO");
    assertThat(version.get("tipo_evento")).isEqualTo("CASO_ABIERTO");
    assertThat(version.get("estado_caso")).isEqualTo("ABIERTO");
    assertThat(version.get("id_usuario_afectado")).isEqualTo(idDe(CORREO));
    assertThat(version.get("id_actor")).isEqualTo(idDe(CORREO_CLIENTE));
    assertThat(version.get("estado_cuenta")).isEqualTo("ACTIVA");
    assertThat(version.get("es_version_actual")).isEqualTo(true);
    assertThat(version.get("fecha_fin_vigencia")).isNull();
    assertThat(version.get("detalle_cambio")).asString().isNotBlank();
    assertThat(version.get("fecha_inicio_vigencia")).isNotNull();
    assertThat(version.get("fecha_registro")).isNotNull();

    // Nada administrativo: reportar no asigna, no sanciona y no resuelve.
    assertThat(version.get("id_administrador_responsable")).isNull();
    assertThat(version.get("id_medida_administrativa")).isNull();
    assertThat(version.get("resultado_caso")).isNull();
    assertThat(version.get("resolucion")).isNull();
    assertThat(version.get("fecha_fin_medida")).isNull();
  }

  @Test
  void laVersionFotografiaElEstadoRealDeLaCuentaReportada() {
    long idSolicitud = solicitudAceptada();
    restringirCuenta(CORREO);

    long idCaso = idDeCaso(reportar(cliente, idSolicitud));

    assertThat(versionActual(idCaso).get("estado_cuenta")).isEqualTo("RESTRINGIDA_TEMPORAL");
    // Y sigue siendo el estado que la cuenta ya tenía: el reporte no lo cambió.
    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("RESTRINGIDA_TEMPORAL");
  }

  @Test
  void laFechaDeAperturaYLaDeInicioDeVigenciaSonElMismoInstante() {
    long idSolicitud = solicitudAceptada();
    long idCaso = idDeCaso(reportar(cliente, idSolicitud));

    assertThat(
            jdbc.queryForObject(
                """
                SELECT c.fecha_apertura = h.fecha_inicio_vigencia
                   AND c.fecha_apertura = c.fecha_actualizacion
                FROM caso_moderacion c
                JOIN historial_caso h ON h.id_caso_moderacion = c.id_caso_moderacion
                WHERE c.id_caso_moderacion = ? AND h.es_version_actual
                """,
                Boolean.class,
                idCaso))
        .isTrue();
  }

  /**
   * El caso y su primera versión se confirman juntos.
   *
   * <p>Lo que se afirma es la invariante que su atomicidad produce: ningún caso existe sin la
   * versión con la que nació. Un reporte rechazado tampoco deja mitades sueltas. Que el reverso
   * también se cumpla —que un fallo posterior no deje el caso solo— lo demuestra {@code
   * ConcurrenciaDeReporteIT}, donde el envío perdedor no deja ninguna de las dos filas.
   */
  @Test
  void ningunCasoQuedaSinSuVersionInicial() {
    long aceptada = solicitudAceptada();
    long completada = solicitudCompletada();
    reportar(cliente, aceptada);
    reportar(cliente, completada);
    reportar(navegador, aceptada);
    // Un reporte rechazado no debe dejar nada a medias.
    reportar(cliente, solicitudPendiente());

    assertThat(
            jdbc.queryForObject(
                """
                SELECT count(*) FROM caso_moderacion c
                WHERE NOT EXISTS (
                    SELECT 1 FROM historial_caso h
                    WHERE h.id_caso_moderacion = c.id_caso_moderacion
                      AND h.numero_version = 1
                      AND h.es_version_actual)
                """,
                Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM caso_moderacion", Integer.class))
        .isEqualTo(3);
  }

  // --- Lo que el reporte no hace ------------------------------------------

  @Test
  void reportarNoCambiaLaSolicitudNiLasCuentasNiCreaMedidasNiAsignaAdministrador() {
    long idSolicitud = solicitudAceptada();
    int cambiosAntes = cambiosRegistrados(idSolicitud);

    long idCaso = idDeCaso(reportar(cliente, idSolicitud));

    assertThat(estadoActualEnBase(idSolicitud)).isEqualTo("ACEPTADA");
    assertThat(cambiosRegistrados(idSolicitud)).isEqualTo(cambiosAntes);
    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
    assertThat(estadoDeCuentaEnBase(CORREO_CLIENTE)).isEqualTo("ACTIVA");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM medida_administrativa", Integer.class))
        .isZero();
    assertThat(sesionesVigentes()).isPositive();

    Map<String, Object> caso = casoEnBase(idCaso);
    assertThat(caso.get("id_administrador_responsable")).isNull();
    assertThat(caso.get("id_medida_administrativa_actual")).isNull();
    assertThat(caso.get("resultado_actual")).isNull();
    assertThat(caso.get("resolucion_actual")).isNull();
    assertThat(caso.get("fecha_cierre_actual")).isNull();
    assertThat(caso.get("fecha_fin_medida_actual")).isNull();
  }

  @Test
  void laRespuestaNoExponeNadaAdministrativo() {
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> respuesta = reportar(cliente, idSolicitud);

    assertThat(json(respuesta).properties().stream().map(Map.Entry::getKey))
        .containsExactlyInAnyOrder(
            "idCasoModeracion",
            "idSolicitudServicio",
            "idReportado",
            "nombreReportado",
            "motivo",
            "descripcion",
            "estadoActual",
            "fechaApertura");
  }

  // --- Consulta del caso propio -------------------------------------------

  @Test
  void antesDeReportarDiceAQuienSePuedeReportarYQueNoHayCaso() {
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> respuesta = leerReporte(cliente, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("solicitudReportable").asBoolean()).isTrue();
    assertThat(json(respuesta).get("puedeReportar").asBoolean()).isTrue();
    assertThat(json(respuesta).get("idReportado").asLong()).isEqualTo(idDe(CORREO));
    assertThat(json(respuesta).get("nombreReportado").asText()).isEqualTo("Taller La Esperanza");
    assertThat(json(respuesta).get("casoAbierto").isNull()).isTrue();
  }

  @Test
  void enUnaSolicitudQueNuncaSeAceptoDiceQueNoEsReportable() {
    long idSolicitud = solicitudPendiente();

    HttpResponse<String> respuesta = leerReporte(cliente, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("solicitudReportable").asBoolean()).isFalse();
    assertThat(json(respuesta).get("puedeReportar").asBoolean()).isFalse();
    assertThat(json(respuesta).get("casoAbierto").isNull()).isTrue();
  }

  @Test
  void despuesDeReportarDevuelveElCasoPropioYCierraLaAccion() {
    long idSolicitud = solicitudAceptada();
    long idCaso = idDeCaso(reportar(cliente, idSolicitud));

    HttpResponse<String> respuesta = leerReporte(cliente, idSolicitud);

    assertThat(json(respuesta).get("puedeReportar").asBoolean()).isFalse();
    assertThat(json(respuesta).get("casoAbierto").get("idCasoModeracion").asLong())
        .isEqualTo(idCaso);
    assertThat(json(respuesta).get("casoAbierto").get("motivo").asText()).isEqualTo(MOTIVO);
    assertThat(json(respuesta).get("casoAbierto").get("estadoActual").asText())
        .isEqualTo("ABIERTO");
  }

  @Test
  void nadieVeElCasoQuePresentoLaContraparte() {
    long idSolicitud = solicitudAceptada();
    assertThat(reportar(cliente, idSolicitud).statusCode()).isEqualTo(HttpStatus.CREATED.value());

    HttpResponse<String> respuesta = leerReporte(navegador, idSolicitud);

    assertThat(json(respuesta).get("casoAbierto").isNull()).isTrue();
    assertThat(json(respuesta).get("puedeReportar").asBoolean()).isTrue();
    // No se busca el valor numérico del caso en todo el JSON: las secuencias
    // de identidad son independientes y ese número puede coincidir, por
    // ejemplo, con el de la solicitud sin que exista una filtración. Lo que
    // protege la privacidad es que el caso ajeno no viaje como recurso ni deje
    // sus campos en el nivel superior.
    assertThat(json(respuesta).has("idCasoModeracion")).isFalse();
    assertThat(respuesta.body()).doesNotContain(MOTIVO).doesNotContain(DESCRIPCION);
  }

  // --- Validación de la entrada -------------------------------------------

  @Test
  void rechazaUnReporteSinMotivoOSinDescripcion() {
    long idSolicitud = solicitudAceptada();

    assertThat(reportar(cliente, idSolicitud, "  ", DESCRIPCION).statusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(reportar(cliente, idSolicitud, MOTIVO, "   ")))
        .isEqualTo("VALIDACION");
    assertThat(reportarCon(cliente, idSolicitud, new HashMap<>()).statusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(casosEnBase(idSolicitud)).isZero();
  }

  @Test
  void rechazaUnMotivoOUnaDescripcionQueSePasanDelMaximo() {
    long idSolicitud = solicitudAceptada();

    assertThat(reportar(cliente, idSolicitud, "M".repeat(121), DESCRIPCION).statusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(reportar(cliente, idSolicitud, MOTIVO, "D".repeat(3001)).statusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(casosEnBase(idSolicitud)).isZero();

    assertThat(reportar(cliente, idSolicitud, "M".repeat(120), "D".repeat(3000)).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
  }

  @Test
  void recortaLosEspaciosDeLosDosTextos() {
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> respuesta =
        reportar(cliente, idSolicitud, "  " + MOTIVO + "  ", "\n" + DESCRIPCION + "\t");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    Map<String, Object> caso = casoEnBase(idDeCaso(respuesta));
    assertThat(caso.get("motivo")).isEqualTo(MOTIVO);
    assertThat(caso.get("descripcion")).isEqualTo(DESCRIPCION);
  }

  @Test
  void unReporteNoSeEditaNiSeRetira() {
    long idSolicitud = solicitudAceptada();
    reportar(cliente, idSolicitud);

    assertThat(cliente.put(rutaDeCaso(idSolicitud), Map.of("motivo", "Otro")).statusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    assertThat(cliente.delete(rutaDeCaso(idSolicitud)).statusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    assertThat(casosEnBase(idSolicitud)).isEqualTo(1);
  }
}
