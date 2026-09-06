package com.moica.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * La revisión y la resolución administrativa de casos, de extremo a extremo.
 *
 * <p>Cubre lo que fija el criterio de salida de P10A: ninguna lectura ni escritura administrativa
 * sin rol y segundo factor, el chat solo dentro de un caso, las transiciones válidas, quién puede
 * resolver y que cada mutación deje una sola versión vigente sin periodos superpuestos.
 */
class RevisionDeCasosIT extends EscenarioDeRevisionDeCasos {

  private NavegadorDePrueba admin;

  @BeforeEach
  void prepararAdministradora() {
    admin = administradora(CORREO_ADMIN);
  }

  // --- Autorización -------------------------------------------------------

  @Test
  @DisplayName("Sin sesión no se llega a ninguna ruta administrativa de casos")
  void sinSesionNoSeLlegaANingunaRuta() {
    long idCaso = casoAbierto();
    NavegadorDePrueba anonimo = abrirNavegador();

    assertThat(consultarBandeja(anonimo).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(consultarExpediente(anonimo, idCaso).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(consultarMensajes(anonimo, idCaso).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(iniciarRevision(anonimo, idCaso).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  @DisplayName("Una cuenta sin rol administrativo no lee ni escribe nada del área")
  void unaCuentaCorrienteNoEntra() {
    long idCaso = casoAbierto();

    assertThat(consultarBandeja(cliente).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(consultarExpediente(cliente, idCaso).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(consultarMensajes(cliente, idCaso).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(asignar(cliente, idCaso, idDe(CORREO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(cerrar(cliente, idCaso, "PROCEDENTE").statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  @DisplayName("Con rol administrativo pero sin segundo factor verificado tampoco se entra")
  void elRolSinSegundoFactorNoBasta() {
    long idCaso = casoAbierto();

    NavegadorDePrueba aMedias = abrirNavegador();
    registrar(aMedias, "sin.factor@moica.test", CLAVE);
    darRolAdministrativo("sin.factor@moica.test");
    assertThat(iniciarSesion(aMedias, "sin.factor@moica.test", CLAVE).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    // La sesión es plena —la cuenta no tiene segundo factor activo—, pero el
    // área administrativa exige además que esta sesión lo haya verificado.
    assertThat(consultarBandeja(aMedias).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(consultarExpediente(aMedias, idCaso).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(consultarMensajes(aMedias, idCaso).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  @DisplayName("Un caso inexistente responde 404 y no revela nada")
  void unCasoInexistenteNoExiste() {
    HttpResponse<String> respuesta = consultarExpediente(admin, 999_999L);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CASO_NO_ENCONTRADO");
  }

  // --- Bandeja y expediente ----------------------------------------------

  @Test
  @DisplayName("La bandeja muestra lo que espera decisión, del más antiguo al más reciente")
  void laBandejaOrdenaPorAntiguedad() {
    long primero = casoAbierto();
    long segundo = casoAbierto();

    HttpResponse<String> respuesta = consultarBandeja(admin);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(idsDeLaBandeja(respuesta)).containsExactly(primero, segundo);
  }

  @Test
  @DisplayName("Un caso cerrado sale de la bandeja por omisión y se consulta pidiéndolo")
  void unCasoCerradoSaleDeLaBandeja() {
    long idCaso = casoEnRevisionDe(admin, CORREO_ADMIN);
    assertThat(cerrar(admin, idCaso, "DESESTIMADO").statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(idsDeLaBandeja(consultarBandeja(admin))).doesNotContain(idCaso);
    assertThat(idsDeLaBandeja(consultarBandeja(admin, "estado=CERRADO"))).contains(idCaso);
  }

  @Test
  @DisplayName("El filtro «mios» deja solo los casos de quien consulta")
  void elFiltroDeLosPropiosSeparaElTrabajo() {
    NavegadorDePrueba otra = administradora(CORREO_OTRO_ADMIN);
    long mio = casoAbierto();
    long ajeno = casoAbierto();

    assertThat(asignar(admin, mio, idDe(CORREO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(asignar(otra, ajeno, idDe(CORREO_OTRO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(idsDeLaBandeja(consultarBandeja(admin, "mios=true"))).containsExactly(mio);
    assertThat(idsDeLaBandeja(consultarBandeja(otra, "mios=true"))).containsExactly(ajeno);
  }

  @Test
  @DisplayName("El expediente reúne el caso, la solicitud, las evidencias y el historial")
  void elExpedienteReuneLoQueHayVinculadoAlCaso() {
    long idCaso = casoAbierto();

    HttpResponse<String> respuesta = consultarExpediente(admin, idCaso);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    var cuerpo = json(respuesta);
    assertThat(cuerpo.get("caso").get("idCasoModeracion").asLong()).isEqualTo(idCaso);
    assertThat(cuerpo.get("caso").get("motivo").asText()).isEqualTo(MOTIVO);
    assertThat(cuerpo.get("descripcion").asText()).isEqualTo(DESCRIPCION);
    assertThat(cuerpo.get("solicitud").get("historial")).isNotEmpty();
    assertThat(cuerpo.get("imagenesDelServicio").isArray()).isTrue();
    assertThat(cuerpo.get("historial")).hasSize(1);
    assertThat(cuerpo.get("historial").get(0).get("tipoEvento").asText()).isEqualTo("CASO_ABIERTO");
    assertThat(cuerpo.get("puedeResolver").asBoolean()).isFalse();
  }

  @Test
  @DisplayName("El expediente no publica correos ni contactos de los participantes")
  void elExpedienteNoFiltraDatosDeContacto() {
    long idCaso = casoAbierto();

    String cuerpo = consultarExpediente(admin, idCaso).body();

    assertThat(cuerpo).doesNotContain(CORREO_CLIENTE).doesNotContain(CORREO);
    assertThat(cuerpo).doesNotContain("telefono").doesNotContain("correoElectronico");
  }

  // --- Chat dentro del caso ----------------------------------------------

  @Test
  @DisplayName("Los mensajes se leen dentro del caso y no hay ruta administrativa por solicitud")
  void elChatSoloSeAlcanzaDesdeUnCaso() {
    long idSolicitud = solicitudAceptada();
    assertThat(enviarMensaje(cliente, idSolicitud, "¿A qué hora llegas?").statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    // Sin caso, el área administrativa no tiene forma de pedir este hilo: no
    // existe ninguna ruta administrativa colgada de la solicitud, y la propia
    // del participante rechaza a quien no participa.
    assertThat(admin.get("/api/admin/solicitudes/" + idSolicitud + "/mensajes").statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(admin.get(RUTA_SOLICITUDES + "/" + idSolicitud + "/mensajes").statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());

    HttpResponse<String> reporte = reportar(cliente, idSolicitud);
    assertThat(reporte.statusCode()).isEqualTo(HttpStatus.CREATED.value());

    HttpResponse<String> mensajes = consultarMensajes(admin, idDeCaso(reporte));
    assertThat(mensajes.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(mensajes)).hasSize(1);
    assertThat(json(mensajes).get(0).get("contenido").asText()).isEqualTo("¿A qué hora llegas?");
  }

  @Test
  @DisplayName("El área administrativa lee el hilo pero no escribe en él")
  void elAreaAdministrativaNoEscribeEnElChat() {
    long idSolicitud = solicitudAceptada();
    HttpResponse<String> reporte = reportar(cliente, idSolicitud);
    long idCaso = idDeCaso(reporte);

    HttpResponse<String> intento =
        admin.post(rutaDeExpediente(idCaso) + "/mensajes", Map.of("contenido", "Aviso oficial"));

    assertThat(intento.statusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    assertThat(json(consultarMensajes(admin, idCaso))).isEmpty();
  }

  // --- Asignación y reasignación -----------------------------------------

  @Test
  @DisplayName("Asignar deja responsable y versiona sin cambiar el estado")
  void asignarDejaResponsableYVersiona() {
    long idCaso = casoAbierto();

    HttpResponse<String> respuesta = asignar(admin, idCaso, idDe(CORREO_ADMIN));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("caso").get("estadoActual").asText()).isEqualTo("ABIERTO");
    assertThat(json(respuesta).get("caso").get("nombreAdministradorResponsable").asText())
        .isNotBlank();
    assertThat(json(respuesta).get("puedeResolver").asBoolean()).isTrue();

    assertThat(casoEnBase(idCaso).get("id_administrador_responsable"))
        .isEqualTo(idDe(CORREO_ADMIN));
    assertThat(versionesEnBase(idCaso)).isEqualTo(2);
    assertThat(versionActual(idCaso).get("tipo_evento")).isEqualTo("RESPONSABLE_ASIGNADO");
    assertThat(versionActual(idCaso).get("estado_caso")).isEqualTo("ABIERTO");
  }

  @Test
  @DisplayName("Reasignar a otra persona deja una versión más y cambia el responsable")
  void reasignarCambiaElResponsable() {
    administradora(CORREO_OTRO_ADMIN);
    long idCaso = casoAbierto();
    assertThat(asignar(admin, idCaso, idDe(CORREO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(asignar(admin, idCaso, idDe(CORREO_OTRO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(casoEnBase(idCaso).get("id_administrador_responsable"))
        .isEqualTo(idDe(CORREO_OTRO_ADMIN));
    assertThat(versionesEnBase(idCaso)).isEqualTo(3);
    assertThat(versionActual(idCaso).get("detalle_cambio").toString()).contains("reasignó");
  }

  @Test
  @DisplayName("Reasignar a quien ya lo tiene no crea una versión repetida")
  void reasignarAlMismoNoVersiona() {
    long idCaso = casoAbierto();
    assertThat(asignar(admin, idCaso, idDe(CORREO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(asignar(admin, idCaso, idDe(CORREO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(versionesEnBase(idCaso)).isEqualTo(2);
  }

  @Test
  @DisplayName("Un caso no se asigna a una cuenta sin rol administrativo")
  void noSeAsignaAQuienNoEsAdministrador() {
    long idCaso = casoAbierto();

    HttpResponse<String> respuesta = asignar(admin, idCaso, idDe(CORREO_CLIENTE));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("ADMINISTRADOR_NO_VALIDO");
    assertThat(casoEnBase(idCaso).get("id_administrador_responsable")).isNull();
    assertThat(versionesEnBase(idCaso)).isEqualTo(1);
  }

  // --- Transiciones -------------------------------------------------------

  @Test
  @DisplayName("Sin responsable asignado no se inicia la revisión")
  void sinResponsableNoSeInicaLaRevision() {
    long idCaso = casoAbierto();

    HttpResponse<String> respuesta = iniciarRevision(admin, idCaso);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CASO_SIN_RESPONSABLE");
    assertThat(casoEnBase(idCaso).get("estado_actual")).isEqualTo("ABIERTO");
    assertThat(versionesEnBase(idCaso)).isEqualTo(1);
  }

  @Test
  @DisplayName("Solo quien tiene el caso asignado lo revisa y lo resuelve")
  void soloElResponsableDecide() {
    NavegadorDePrueba otra = administradora(CORREO_OTRO_ADMIN);
    long idCaso = casoAbierto();
    assertThat(asignar(admin, idCaso, idDe(CORREO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> intento = iniciarRevision(otra, idCaso);

    assertThat(intento.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(intento)).isEqualTo("CASO_DE_OTRO_ADMINISTRADOR");
    assertThat(casoEnBase(idCaso).get("estado_actual")).isEqualTo("ABIERTO");

    // Y quien sí lo tiene, avanza.
    assertThat(iniciarRevision(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(cerrar(otra, idCaso, "PROCEDENTE").statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  @DisplayName("No se cierra un caso que todavía no está en revisión")
  void noSeCierraSinRevisar() {
    long idCaso = casoAbierto();
    assertThat(asignar(admin, idCaso, idDe(CORREO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> respuesta = cerrar(admin, idCaso, "PROCEDENTE");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(casoEnBase(idCaso).get("estado_actual")).isEqualTo("ABIERTO");
    assertThat(casoEnBase(idCaso).get("resultado_actual")).isNull();
  }

  @Test
  @DisplayName("Un caso ya cerrado no se vuelve a cerrar ni se reasigna")
  void unCasoCerradoNoSeMueve() {
    administradora(CORREO_OTRO_ADMIN);
    long idCaso = casoEnRevisionDe(admin, CORREO_ADMIN);
    assertThat(cerrar(admin, idCaso, "PROCEDENTE").statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(codigoDeError(cerrar(admin, idCaso, "DESESTIMADO")))
        .isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(codigoDeError(iniciarRevision(admin, idCaso))).isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(codigoDeError(asignar(admin, idCaso, idDe(CORREO_OTRO_ADMIN))))
        .isEqualTo("TRANSICION_NO_PERMITIDA");

    assertThat(casoEnBase(idCaso).get("resultado_actual")).isEqualTo("PROCEDENTE");
  }

  // --- Resolución ---------------------------------------------------------

  @Test
  @DisplayName("Cerrar registra resultado, resolución y fecha de cierre juntos")
  void cerrarRegistraLaDecisionCompleta() {
    long idCaso = casoEnRevisionDe(admin, CORREO_ADMIN);

    HttpResponse<String> respuesta = cerrar(admin, idCaso, "DESESTIMADO");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("caso").get("estadoActual").asText()).isEqualTo("CERRADO");
    assertThat(json(respuesta).get("caso").get("resultadoActual").asText())
        .isEqualTo("DESESTIMADO");
    assertThat(json(respuesta).get("resolucionActual").asText()).isEqualTo(RESOLUCION);

    Map<String, Object> caso = casoEnBase(idCaso);
    assertThat(caso.get("estado_actual")).isEqualTo("CERRADO");
    assertThat(caso.get("resultado_actual")).isEqualTo("DESESTIMADO");
    assertThat(caso.get("resolucion_actual")).isEqualTo(RESOLUCION);
    assertThat(caso.get("fecha_cierre_actual")).isNotNull();
  }

  @Test
  @DisplayName("Una resolución vacía o un resultado inventado no cierran nada")
  void laResolucionSeValidaEnLaFrontera() {
    long idCaso = casoEnRevisionDe(admin, CORREO_ADMIN);

    assertThat(cerrar(admin, idCaso, "PROCEDENTE", "   ").statusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(cerrar(admin, idCaso, "CASTIGADO").statusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());

    assertThat(casoEnBase(idCaso).get("estado_actual")).isEqualTo("EN_REVISION");
    assertThat(versionesEnBase(idCaso)).isEqualTo(3);
  }

  @Test
  @DisplayName("Resolver PROCEDENTE no sanciona la cuenta reportada ni aplica una medida")
  void resolverNoSanciona() {
    long idCaso = casoEnRevisionDe(admin, CORREO_ADMIN);

    assertThat(cerrar(admin, idCaso, "PROCEDENTE").statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
    assertThat(casoEnBase(idCaso).get("id_medida_administrativa_actual")).isNull();
    assertThat(versionActual(idCaso).get("estado_cuenta")).isEqualTo("ACTIVA");
    assertThat(versionActual(idCaso).get("id_medida_administrativa")).isNull();
  }

  @Test
  @DisplayName("Cada versión conserva el responsable de entonces, distinto de quien la originó")
  void elHistorialConservaElResponsableDeCadaVersion() {
    NavegadorDePrueba otra = administradora(CORREO_OTRO_ADMIN);
    long idCaso = casoAbierto();

    // El escenario registra todas las cuentas con el mismo nombre, así que sin
    // esto el nombre no distinguiría nada y la prueba pasaría por accidente.
    renombrar(CORREO_ADMIN, "Lucía Moderadora");
    renombrar(CORREO_OTRO_ADMIN, "Carlos Moderador");

    // Quien ejecuta la acción y quien recibe el caso son personas distintas en
    // las dos: si el DTO confundiera actor con responsable, la prueba lo vería.
    assertThat(asignar(admin, idCaso, idDe(CORREO_OTRO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(asignar(otra, idCaso, idDe(CORREO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    var historial = json(consultarExpediente(admin, idCaso)).get("historial");
    assertThat(historial).hasSize(3);

    // La apertura no tiene responsable: nadie respondía por el caso todavía.
    var apertura = historial.get(0);
    assertThat(apertura.get("idAdministradorResponsable").isNull()).isTrue();
    assertThat(apertura.get("nombreAdministradorResponsable").isNull()).isTrue();

    var asignacion = historial.get(1);
    assertThat(asignacion.get("idActor").asLong()).isEqualTo(idDe(CORREO_ADMIN));
    assertThat(asignacion.get("nombreActor").asText()).isEqualTo("Lucía Moderadora");
    assertThat(asignacion.get("idAdministradorResponsable").asLong())
        .isEqualTo(idDe(CORREO_OTRO_ADMIN));
    assertThat(asignacion.get("nombreAdministradorResponsable").asText())
        .isEqualTo("Carlos Moderador");

    var reasignacion = historial.get(2);
    assertThat(reasignacion.get("idActor").asLong()).isEqualTo(idDe(CORREO_OTRO_ADMIN));
    assertThat(reasignacion.get("nombreActor").asText()).isEqualTo("Carlos Moderador");
    assertThat(reasignacion.get("idAdministradorResponsable").asLong())
        .isEqualTo(idDe(CORREO_ADMIN));
    assertThat(reasignacion.get("nombreAdministradorResponsable").asText())
        .isEqualTo("Lucía Moderadora");

    // Y lo que publica la API es lo que quedó guardado, no algo derivado.
    assertThat(versionesEnOrden(idCaso))
        .extracting(fila -> fila.get("id_administrador_responsable"))
        .containsExactly(null, idDe(CORREO_OTRO_ADMIN), idDe(CORREO_ADMIN));
  }

  // --- Historial SCD2 -----------------------------------------------------

  @Test
  @DisplayName("El recorrido completo deja una versión por evento, encadenadas y sin solapes")
  void cadaMutacionCierraLaVersionAnterior() {
    long idCaso = casoAbierto();
    assertThat(asignar(admin, idCaso, idDe(CORREO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(iniciarRevision(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(cerrar(admin, idCaso, "PROCEDENTE").statusCode()).isEqualTo(HttpStatus.OK.value());

    List<Map<String, Object>> versiones = versionesEnOrden(idCaso);

    assertThat(versiones).hasSize(4);
    assertThat(versiones)
        .extracting(fila -> fila.get("numero_version"))
        .containsExactly(1, 2, 3, 4);
    assertThat(versiones)
        .extracting(fila -> fila.get("tipo_evento"))
        .containsExactly(
            "CASO_ABIERTO",
            "RESPONSABLE_ASIGNADO",
            "ESTADO_CASO_CAMBIADO",
            "RESOLUCION_REGISTRADA");
    assertThat(versiones)
        .extracting(fila -> fila.get("estado_caso"))
        .containsExactly("ABIERTO", "ABIERTO", "EN_REVISION", "CERRADO");
    assertThat(versiones)
        .extracting(fila -> fila.get("tipo_actor"))
        .containsExactly("USUARIO", "ADMINISTRADOR", "ADMINISTRADOR", "ADMINISTRADOR");

    // Una sola vigente, y es la última.
    assertThat(versionesVigentes(idCaso)).isEqualTo(1);
    assertThat(versiones.get(3).get("es_version_actual")).isEqualTo(true);
    assertThat(versiones.get(3).get("fecha_fin_vigencia")).isNull();
    assertThat(versiones.get(3).get("resultado_caso")).isEqualTo("PROCEDENTE");

    // Cada versión cerrada termina donde empieza la siguiente: sin huecos ni solapes.
    for (int i = 0; i < versiones.size() - 1; i++) {
      assertThat(versiones.get(i).get("fecha_fin_vigencia"))
          .as("la versión %d cierra al empezar la siguiente", i + 1)
          .isEqualTo(versiones.get(i + 1).get("fecha_inicio_vigencia"));
      assertThat(versiones.get(i).get("es_version_actual")).isEqualTo(false);
    }
    assertThat(vigenciasSuperpuestas(idCaso)).isZero();
  }

  @Test
  @DisplayName("Una mutación rechazada no deja rastro en el historial")
  void unaMutacionRechazadaNoVersiona() {
    long idCaso = casoEnRevisionDe(admin, CORREO_ADMIN);
    int antes = versionesEnBase(idCaso);

    assertThat(cerrar(admin, idCaso, "PROCEDENTE", " ").statusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());

    assertThat(versionesEnBase(idCaso)).isEqualTo(antes);
    assertThat(versionesVigentes(idCaso)).isEqualTo(1);
    assertThat(vigenciasSuperpuestas(idCaso)).isZero();
  }

  // --- Directorio de administradores --------------------------------------

  @Test
  @DisplayName("El directorio de administradores solo publica identificador y nombre")
  void elDirectorioNoPublicaCorreos() {
    administradora(CORREO_OTRO_ADMIN);

    HttpResponse<String> respuesta = admin.get(RUTA_ADMINISTRADORES);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).size()).isGreaterThanOrEqualTo(2);
    assertThat(respuesta.body()).doesNotContain(CORREO_ADMIN).doesNotContain(CORREO_OTRO_ADMIN);
    assertThat(cliente.get(RUTA_ADMINISTRADORES).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
  }
}
