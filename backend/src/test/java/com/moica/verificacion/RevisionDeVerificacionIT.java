package com.moica.verificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * La revisión administrativa: quién entra, qué transiciones existen y cómo se abre un documento.
 *
 * <p>Las dos condiciones del área administrativa se recorren enteras —sin rol, con rol pero sin
 * segundo factor, y el único caso que sí pasa— porque de ellas depende que un documento de
 * identidad no lo abra nadie más.
 */
class RevisionDeVerificacionIT extends EscenarioDeVerificacion {

  @Test
  void sinSesionResponde401() {
    NavegadorDePrueba anonimo = abrirNavegador();

    assertThat(anonimo.get(RUTA_REVISION).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void unaCuentaOrdinariaConSegundoFactorVerificadoSigueRecibiendo403() {
    activarSegundoFactor(navegador);

    HttpResponse<String> respuesta = navegador.get(RUTA_REVISION);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("ACCESO_DENEGADO");
  }

  @Test
  void unAdministradorSinSegundoFactorVerificadoRecibe403() {
    NavegadorDePrueba admin = abrirNavegador();
    registrar(admin, CORREO_ADMIN, CLAVE);
    darRolAdministrativo(CORREO_ADMIN);
    iniciarSesion(admin, CORREO_ADMIN, CLAVE);

    assertThat(admin.get(RUTA_REVISION).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void laColaMuestraLoQueEsperaDecisionYAdmiteFiltros() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    long basica = enviarBasicaCorrecta();

    HttpResponse<String> cola = admin.get(RUTA_REVISION);
    assertThat(cola.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(cola)).hasSize(1);

    JsonNode expediente = json(cola).get(0);
    assertThat(expediente.get("idSolicitudVerificacion").asLong()).isEqualTo(basica);
    assertThat(expediente.get("prestador").get("nombrePublico").asText())
        .isEqualTo("Taller La Esperanza");
    assertThat(expediente.get("prestador").get("correoElectronico").asText()).isEqualTo(CORREO);
    assertThat(expediente.get("prestador").get("nivelVerificacion").asText())
        .isEqualTo("SIN_VERIFICAR");
    assertThat(expediente.get("documentos")).hasSize(1);

    assertThat(json(admin.get(RUTA_REVISION + "?nivel=PROFESIONAL"))).isEmpty();
    assertThat(json(admin.get(RUTA_REVISION + "?nivel=BASICA"))).hasSize(1);
    assertThat(json(admin.get(RUTA_REVISION + "?estado=APROBADA"))).isEmpty();
  }

  @Test
  void laColaNoRevelaClavesDeAlmacenamiento() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    enviarBasicaCorrecta();

    assertThat(admin.get(RUTA_REVISION).body())
        .doesNotContain("claveAlmacenamiento")
        .doesNotContain("expedientes/");
  }

  @Test
  void tomarUnaSolicitudLaPasaARevisionYAsignaAQuienLaTomo() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    long solicitud = enviarBasicaCorrecta();

    HttpResponse<String> respuesta =
        admin.post(RUTA_REVISION + "/" + solicitud + "/toma", Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("estadoSolicitud").asText()).isEqualTo("EN_REVISION");
    assertThat(json(respuesta).get("idAdministradorRevisor").asLong())
        .isEqualTo(idDe(CORREO_ADMIN));
    assertThat(json(respuesta).get("fechaInicioRevision").isNull()).isFalse();
  }

  @Test
  void unaSegundaTomaChocaConLaPrimera() {
    NavegadorDePrueba primera = administradora(CORREO_ADMIN);
    NavegadorDePrueba segunda = administradora(CORREO_OTRO_ADMIN);
    long solicitud = enviarBasicaCorrecta();

    assertThat(primera.post(RUTA_REVISION + "/" + solicitud + "/toma", Map.of()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> choque = segunda.post(RUTA_REVISION + "/" + solicitud + "/toma", Map.of());

    assertThat(choque.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(choque)).isEqualTo("SOLICITUD_YA_TOMADA");
    assertThat(
            jdbc.queryForObject(
                "SELECT id_administrador_revisor FROM solicitud_verificacion_prestador"
                    + " WHERE id_solicitud_verificacion = ?",
                Long.class,
                solicitud))
        .isEqualTo(idDe(CORREO_ADMIN));
  }

  @Test
  void dosAdministradoresQueLaTomanALaVezNoSeLaQuedanLosDos() throws Exception {
    NavegadorDePrueba primera = administradora(CORREO_ADMIN);
    NavegadorDePrueba segunda = administradora(CORREO_OTRO_ADMIN);
    long solicitud = enviarBasicaCorrecta();

    String ruta = RUTA_REVISION + "/" + solicitud + "/toma";
    CyclicBarrier salida = new CyclicBarrier(2);

    Callable<Integer> tomar =
        () -> {
          salida.await();
          return primera.post(ruta, Map.of()).statusCode();
        };
    Callable<Integer> tomarTambien =
        () -> {
          salida.await();
          return segunda.post(ruta, Map.of()).statusCode();
        };

    ExecutorService hilos = Executors.newFixedThreadPool(2);
    try {
      List<Future<Integer>> resultados = hilos.invokeAll(List.of(tomar, tomarTambien));
      List<Integer> estados = List.of(resultados.get(0).get(), resultados.get(1).get());

      assertThat(estados)
          .as("una toma gana y la otra recibe conflicto; nunca ganan las dos")
          .containsExactlyInAnyOrder(HttpStatus.OK.value(), HttpStatus.CONFLICT.value());
    } finally {
      hilos.shutdownNow();
    }

    assertThat(estadoDeLaSolicitud(solicitud)).isEqualTo("EN_REVISION");
  }

  @Test
  void soloQuienTomoLaRevisionPuedeResolverla() {
    NavegadorDePrueba quienLaTomo = administradora(CORREO_ADMIN);
    NavegadorDePrueba otra = administradora(CORREO_OTRO_ADMIN);
    long solicitud = enviarBasicaCorrecta();
    quienLaTomo.post(RUTA_REVISION + "/" + solicitud + "/toma", Map.of());

    HttpResponse<String> ajena =
        otra.post(RUTA_REVISION + "/" + solicitud + "/aprobacion", Map.of());

    assertThat(ajena.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(ajena)).isEqualTo("REVISION_DE_OTRO_ADMINISTRADOR");
    assertThat(estadoDeLaSolicitud(solicitud)).isEqualTo("EN_REVISION");
    assertThat(nivelDelPerfil()).isEqualTo("SIN_VERIFICAR");
  }

  @Test
  void noSePuedeAprobarLoQueNadieHaTomado() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    long solicitud = enviarBasicaCorrecta();

    HttpResponse<String> respuesta =
        admin.post(RUTA_REVISION + "/" + solicitud + "/aprobacion", Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(estadoDeLaSolicitud(solicitud)).isEqualTo("PENDIENTE");
  }

  @Test
  void aprobarLaBasicaDejaElPerfilVerificadoBasico() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    long solicitud = aprobarBasica(admin);

    assertThat(estadoDeLaSolicitud(solicitud)).isEqualTo("APROBADA");
    assertThat(nivelDelPerfil()).isEqualTo("VERIFICADO_BASICO");

    JsonNode estado = json(navegador.get(RUTA_VERIFICACION_PROPIA));
    assertThat(estado.get("nivelVerificacion").asText()).isEqualTo("VERIFICADO_BASICO");
    assertThat(estado.get("puedeSolicitarProfesional").asBoolean()).isTrue();
    assertThat(estado.get("puedeSolicitarBasica").asBoolean()).isFalse();
  }

  @Test
  void aprobarLaProfesionalDejaElPerfilProfesionalVerificado() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    aprobarBasica(admin);

    HttpResponse<String> envio = enviarExpediente("PROFESIONAL", certificado());
    long profesional = json(envio).get("idSolicitudVerificacion").asLong();
    admin.post(RUTA_REVISION + "/" + profesional + "/toma", Map.of());
    admin.post(RUTA_REVISION + "/" + profesional + "/aprobacion", Map.of());

    assertThat(nivelDelPerfil()).isEqualTo("PROFESIONAL_VERIFICADO");
  }

  @Test
  void rechazarExigeUnMotivoNoVacio() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    long solicitud = enviarBasicaCorrecta();
    admin.post(RUTA_REVISION + "/" + solicitud + "/toma", Map.of());

    HttpResponse<String> sinMotivo =
        admin.post(RUTA_REVISION + "/" + solicitud + "/rechazo", Map.of("observacion", "   "));

    assertThat(sinMotivo.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(sinMotivo)).isEqualTo("VALIDACION");
    assertThat(estadoDeLaSolicitud(solicitud)).isEqualTo("EN_REVISION");
  }

  @Test
  void elMotivoDelRechazoLlegaAQuienPresentoElExpediente() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    long solicitud = enviarBasicaCorrecta();
    admin.post(RUTA_REVISION + "/" + solicitud + "/toma", Map.of());
    admin.post(
        RUTA_REVISION + "/" + solicitud + "/rechazo",
        Map.of("observacion", "El documento está ilegible; envía una foto más nítida."));

    JsonNode propia = json(navegador.get(RUTA_SOLICITUDES_PROPIAS + "/" + solicitud));

    assertThat(propia.get("estadoSolicitud").asText()).isEqualTo("RECHAZADA");
    assertThat(propia.get("observacionResolucion").asText())
        .isEqualTo("El documento está ilegible; envía una foto más nítida.");
    assertThat(propia.has("idAdministradorRevisor"))
        .as("al prestador le corresponde saber qué se decidió, no quién lo decidió")
        .isFalse();
    assertThat(nivelDelPerfil()).isEqualTo("SIN_VERIFICAR");
  }

  @Test
  void rechazarUnaProfesionalNoTocaLaBasicaVigente() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    long basica = aprobarBasica(admin);

    long profesional =
        json(enviarExpediente("PROFESIONAL", certificado()))
            .get("idSolicitudVerificacion")
            .asLong();
    admin.post(RUTA_REVISION + "/" + profesional + "/toma", Map.of());
    admin.post(
        RUTA_REVISION + "/" + profesional + "/rechazo",
        Map.of("observacion", "El certificado no corresponde con la actividad declarada."));

    assertThat(estadoDeLaSolicitud(profesional)).isEqualTo("RECHAZADA");
    assertThat(estadoDeLaSolicitud(basica)).isEqualTo("APROBADA");
    assertThat(nivelDelPerfil()).isEqualTo("VERIFICADO_BASICO");
  }

  @Test
  void unAdministradorConSegundoFactorObtieneUnAccesoTemporalQueCaduca() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    long solicitud = enviarBasicaCorrecta();
    long idDocumento = idDelPrimerDocumento();

    HttpResponse<String> respuesta =
        admin.get(RUTA_REVISION + "/" + solicitud + "/documentos/" + idDocumento + "/acceso");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    assertThat(respuesta.headers().firstValue("Cache-Control")).contains("no-store");

    URI acceso = URI.create(respuesta.headers().firstValue("Location").orElseThrow());
    assertThat(documentos.sigueVigente(acceso, Instant.now()))
        .as("el enlace sirve mientras dura")
        .isTrue();
    assertThat(documentos.sigueVigente(acceso, Instant.now().plusSeconds(600)))
        .as("y deja de servir pasados los cinco minutos configurados")
        .isFalse();
  }

  @Test
  void unDocumentoDeOtroExpedienteNoSeAbreDesdeAqui() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    long solicitud = enviarBasicaCorrecta();

    HttpResponse<String> respuesta =
        admin.get(RUTA_REVISION + "/" + solicitud + "/documentos/999999/acceso");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("DOCUMENTO_NO_ENCONTRADO");
  }

  @Test
  void conElAlmacenamientoCaidoElAccesoFallaSinRevelarNada() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    long solicitud = enviarBasicaCorrecta();
    long idDocumento = idDelPrimerDocumento();
    documentos.simularNoDisponible();

    HttpResponse<String> respuesta =
        admin.get(RUTA_REVISION + "/" + solicitud + "/documentos/" + idDocumento + "/acceso");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    assertThat(respuesta.body())
        .doesNotContain("R2")
        .doesNotContain("cloudflare")
        .doesNotContain("expedientes/")
        .doesNotContain("X-Amz");
  }

  @Test
  void unaSolicitudInexistenteRespondeComoTal() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);

    HttpResponse<String> respuesta = admin.get(RUTA_REVISION + "/999999");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SOLICITUD_NO_ENCONTRADA");
  }

  @Test
  void unEstadoQueNoPerteneceAlDominioNoSeAdmiteComoFiltro() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);

    assertThat(admin.get(RUTA_REVISION + "?estado=ARCHIVADA").statusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  private long idDelPrimerDocumento() {
    return Objects.requireNonNull(
        jdbc.queryForObject(
            "SELECT min(id_documento_verificacion) FROM documento_verificacion_prestador",
            Long.class));
  }
}
