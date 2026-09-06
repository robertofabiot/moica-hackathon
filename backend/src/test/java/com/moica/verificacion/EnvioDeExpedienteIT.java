package com.moica.verificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * El envío y la consulta del expediente propio.
 *
 * <p>Lo que se demuestra aquí es que la solicitud y sus documentos nacen juntos o no nacen: no hay
 * un estado intermedio, un fallo a mitad no deja nada apuntado y los archivos que llegaron a
 * subirse se retiran. Además, que la base guarda clave y metadatos y nunca el binario ni una URL, y
 * que la clave no sale hacia el cliente ni siquiera hacia el propietario.
 */
class EnvioDeExpedienteIT extends EscenarioDeVerificacion {

  @Test
  void enviaUnaBasicaYRegistraSolicitudYExpedienteALaVez() {
    HttpResponse<String> respuesta = enviarExpediente("BASICA", cedula());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());

    JsonNode solicitud = json(respuesta);
    assertThat(solicitud.get("nivelSolicitado").asText()).isEqualTo("BASICA");
    assertThat(solicitud.get("estadoSolicitud").asText()).isEqualTo("PENDIENTE");
    assertThat(solicitud.get("observacionResolucion").isNull()).isTrue();
    assertThat(solicitud.get("documentos")).hasSize(1);

    assertThat(solicitudesGuardadas()).isEqualTo(1);
    assertThat(documentosGuardados()).isEqualTo(1);
    assertThat(nivelDelPerfil())
        .as("el perfil sigue sin verificar hasta que una persona apruebe")
        .isEqualTo("SIN_VERIFICAR");
  }

  @Test
  void persisteLaClaveYLosMetadatosPeroNuncaElBinarioNiUnaUrl() {
    enviarExpediente("BASICA", cedula());

    Map<String, Object> fila = jdbc.queryForMap("SELECT * FROM documento_verificacion_prestador");

    String clave = (String) fila.get("clave_almacenamiento");
    assertThat(clave).startsWith("expedientes/").endsWith(".png");
    assertThat(clave).as("la clave es opaca: nada del nombre original").doesNotContain("cedula");
    assertThat(fila.get("nombre_original")).isEqualTo("cedula frente.png");
    assertThat(fila.get("tipo_mime")).isEqualTo("image/png");
    assertThat(fila.get("tamano_bytes")).isEqualTo(64);
    assertThat(fila.keySet())
        .as("no hay columna de binario ni de URL: solo clave y metadatos")
        .doesNotContain("contenido", "url", "url_documento");

    assertThat(documentos.contiene(clave)).isTrue();
    assertThat(documentos.objeto(clave).orElseThrow().contenido()).isEqualTo(documentoPng());
  }

  @Test
  void laRespuestaNoLlevaLaClaveDeAlmacenamientoNiNingunaUrl() {
    HttpResponse<String> respuesta = enviarExpediente("BASICA", cedula());

    String clave =
        jdbc.queryForObject(
            "SELECT clave_almacenamiento FROM documento_verificacion_prestador", String.class);

    assertThat(respuesta.body())
        .doesNotContain(clave)
        .doesNotContain("claveAlmacenamiento")
        .doesNotContain("expedientes/")
        .doesNotContain("https://");
    assertThat(primerDocumentoDe(respuesta).has("claveAlmacenamiento")).isFalse();
  }

  @Test
  void saneaElNombreOriginalAntesDePersistirlo() {
    // La ruta y los caracteres que no admite un nombre de archivo se quitan
    // antes de guardar. Los de control no llegan hasta aquí: romperían la
    // propia cabecera multipart, y su saneamiento se prueba en
    // `NombreDeArchivoTest`.
    enviarExpediente(
        "BASICA",
        new DocumentoDePrueba(
            "IDENTIDAD", "../../etc/cedula|falsa*.png", "image/png", documentoPng()));

    assertThat(
            jdbc.queryForObject(
                "SELECT nombre_original FROM documento_verificacion_prestador", String.class))
        .isEqualTo("cedulafalsa.png");
  }

  @Test
  void admiteVariosDocumentosDeLosTresFormatos() {
    HttpResponse<String> respuesta =
        enviarExpediente(
            "BASICA",
            cedula(),
            new DocumentoDePrueba("IDENTIDAD", "reverso.jpg", "image/jpeg", documentoJpeg()),
            certificado());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(respuesta).get("documentos")).hasSize(3);
    assertThat(documentos.cantidadDeObjetos()).isEqualTo(3);
    assertThat(
            jdbc.queryForList(
                "SELECT tipo_mime FROM documento_verificacion_prestador ORDER BY"
                    + " id_documento_verificacion",
                String.class))
        .containsExactly("image/png", "image/jpeg", "application/pdf");
  }

  @Test
  void rechazaUnaSolicitudSinNingunDocumento() {
    HttpResponse<String> respuesta =
        navegador.postFormulario(
            RUTA_SOLICITUDES_PROPIAS,
            List.of(),
            List.of(new NavegadorDePrueba.CampoDeFormulario("nivelSolicitado", "BASICA")));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(solicitudesGuardadas()).isZero();
  }

  @Test
  void laBasicaExigeAlMenosUnDocumentoDeIdentidad() {
    HttpResponse<String> respuesta = enviarExpediente("BASICA", certificado());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("EXPEDIENTE_INCOMPLETO");
    assertThat(solicitudesGuardadas()).isZero();
    assertThat(documentos.cantidadDeObjetos())
        .as("no sube ni un byte de un expediente que no puede sostener lo que pide")
        .isZero();
  }

  @Test
  void laProfesionalExigeUnaBasicaVigente() {
    HttpResponse<String> respuesta = enviarExpediente("PROFESIONAL", certificado());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("VERIFICACION_BASICA_REQUERIDA");
    assertThat(solicitudesGuardadas()).isZero();
  }

  @Test
  void laProfesionalExigeUnRespaldoQueNoSeaLaIdentidad() {
    aprobarBasica(administradora(CORREO_ADMIN));

    HttpResponse<String> respuesta = enviarExpediente("PROFESIONAL", cedula());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("EXPEDIENTE_INCOMPLETO");
  }

  @Test
  void noAdmiteDosSolicitudesAbiertasDelMismoNivel() {
    enviarBasicaCorrecta();

    HttpResponse<String> segunda = enviarExpediente("BASICA", cedula());

    assertThat(segunda.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(segunda)).isEqualTo("SOLICITUD_ABIERTA_DUPLICADA");
    assertThat(solicitudesGuardadas()).isEqualTo(1);
    assertThat(documentos.cantidadDeObjetos()).isEqualTo(1);
  }

  @Test
  void unaBasicaYaVigenteNoSeVuelveASolicitar() {
    aprobarBasica(administradora(CORREO_ADMIN));

    HttpResponse<String> respuesta = enviarExpediente("BASICA", cedula());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("NIVEL_YA_VIGENTE");
  }

  @Test
  void despuesDeUnRechazoSePuedeReenviarComoSolicitudNueva() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    long primera = enviarBasicaCorrecta();
    admin.post(RUTA_REVISION + "/" + primera + "/toma", Map.of());
    admin.post(
        RUTA_REVISION + "/" + primera + "/rechazo",
        Map.of("observacion", "El documento está ilegible."));

    HttpResponse<String> reenvio = enviarExpediente("BASICA", cedula());

    assertThat(reenvio.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(reenvio).get("idSolicitudVerificacion").asLong()).isNotEqualTo(primera);
    assertThat(solicitudesGuardadas()).as("la anterior se conserva").isEqualTo(2);
    assertThat(documentosGuardados()).as("y sus documentos también").isEqualTo(2);
    assertThat(estadoDeLaSolicitud(primera)).isEqualTo("RECHAZADA");
  }

  @Test
  void rechazaUnaCabeceraQueNoCorrespondeConLaFirmaReal() {
    HttpResponse<String> respuesta =
        enviarExpediente(
            "BASICA",
            new DocumentoDePrueba("IDENTIDAD", "disfrazada.png", "image/png", documentoPdf()));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("DOCUMENTO_NO_ADMITIDO");
    assertThat(documentos.cantidadDeObjetos()).isZero();
    assertThat(solicitudesGuardadas()).isZero();
  }

  @Test
  void rechazaUnFormatoQueNoEstaAdmitidoEnElExpediente() {
    byte[] webp = new byte[] {0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50};

    HttpResponse<String> respuesta =
        enviarExpediente(
            "BASICA", new DocumentoDePrueba("IDENTIDAD", "foto.webp", "image/webp", webp));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("DOCUMENTO_NO_ADMITIDO");
  }

  @Test
  void rechazaUnArchivoVacioOMayorDeCincoMegas() {
    HttpResponse<String> vacio =
        enviarExpediente(
            "BASICA", new DocumentoDePrueba("IDENTIDAD", "vacia.png", "image/png", new byte[0]));
    assertThat(vacio.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(vacio)).isEqualTo("DOCUMENTO_NO_ADMITIDO");

    byte[] enorme = Arrays.copyOf(documentoPng(), 5 * 1024 * 1024 + 1);
    HttpResponse<String> grande =
        enviarExpediente(
            "BASICA", new DocumentoDePrueba("IDENTIDAD", "enorme.png", "image/png", enorme));
    assertThat(grande.statusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE.value());
    assertThat(codigoDeError(grande)).isEqualTo("DOCUMENTO_DEMASIADO_GRANDE");

    assertThat(solicitudesGuardadas()).isZero();
    assertThat(documentos.cantidadDeObjetos()).isZero();
  }

  @Test
  void rechazaUnTipoDocumentalFueraDelDominio() {
    HttpResponse<String> respuesta =
        enviarExpediente(
            "BASICA", new DocumentoDePrueba("SELFIE", "selfie.png", "image/png", documentoPng()));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SOLICITUD_INVALIDA");
  }

  @Test
  void siFallaUnaCargaSeRetiraLoQueYaSeHabiaSubido() {
    documentos.fallarAlGuardarDesdeLaPosicion(1);

    HttpResponse<String> respuesta = enviarExpediente("BASICA", cedula(), certificado());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("ALMACENAMIENTO_NO_DISPONIBLE");
    assertThat(solicitudesGuardadas()).as("no queda una solicitud a medias").isZero();
    assertThat(documentos.cantidadDeObjetos())
        .as("la compensación retiró el objeto que sí llegó a subirse")
        .isZero();
    assertThat(documentos.clavesEliminadas())
        .containsExactlyElementsOf(documentos.clavesGuardadas());
  }

  @Test
  void siFallaLaBaseSeRetiranTodosLosObjetosDelIntento() {
    romperLaPersistenciaDeDocumentos();

    HttpResponse<String> respuesta = enviarExpediente("BASICA", cedula(), certificado());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(solicitudesGuardadas()).as("la transacción entera se deshizo").isZero();
    assertThat(documentosGuardados()).isZero();
    assertThat(documentos.cantidadDeObjetos()).isZero();
    assertThat(documentos.clavesEliminadas()).hasSize(2);
  }

  @Test
  void conElAlmacenamientoCaidoRespondeElErrorUniformeSinTocarLaBase() {
    documentos.simularNoDisponible();

    HttpResponse<String> respuesta = enviarExpediente("BASICA", cedula());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("ALMACENAMIENTO_NO_DISPONIBLE");
    assertThat(respuesta.body())
        .as("el error no revela proveedor, endpoint, bucket ni clave")
        .doesNotContain("R2")
        .doesNotContain("cloudflare")
        .doesNotContain("S3")
        .doesNotContain("expedientes/");
    assertThat(solicitudesGuardadas()).isZero();
  }

  @Test
  void elEstadoPropioExplicaElNivelVigenteYQuePuedeSolicitar() {
    HttpResponse<String> respuesta = navegador.get(RUTA_VERIFICACION_PROPIA);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());

    JsonNode estado = json(respuesta);
    assertThat(estado.get("nivelVerificacion").asText()).isEqualTo("SIN_VERIFICAR");
    assertThat(estado.get("significado").asText()).isNotBlank();
    assertThat(estado.get("puedeSolicitarBasica").asBoolean()).isTrue();
    assertThat(estado.get("puedeSolicitarProfesional").asBoolean()).isFalse();
    assertThat(estado.get("solicitudAbierta").isNull()).isTrue();
  }

  @Test
  void conUnaSolicitudAbiertaYaNoOfreceEnviarOtraDelMismoNivel() {
    enviarBasicaCorrecta();

    JsonNode estado = json(navegador.get(RUTA_VERIFICACION_PROPIA));

    assertThat(estado.get("puedeSolicitarBasica").asBoolean()).isFalse();
    assertThat(estado.get("solicitudAbierta").get("estadoSolicitud").asText())
        .isEqualTo("PENDIENTE");
  }

  @Test
  void elHistorialPropioDevuelveLasSolicitudesConSusDocumentos() {
    enviarBasicaCorrecta();

    HttpResponse<String> respuesta = navegador.get(RUTA_SOLICITUDES_PROPIAS);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta)).hasSize(1);
    assertThat(json(respuesta).get(0).get("documentos").get(0).get("nombreOriginal").asText())
        .isEqualTo("cedula frente.png");
  }

  @Test
  void unaSolicitudDeOtroPrestadorRespondeComoInexistente() {
    long ajena = enviarBasicaCorrecta();

    NavegadorDePrueba otra = abrirNavegador();
    registrar(otra, CORREO_OTRA_PERSONA, CLAVE);
    assertThat(iniciarSesion(otra, CORREO_OTRA_PERSONA, CLAVE).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    assertThat(otra.post(RUTA_PERFIL, solicitudDePerfil()).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    HttpResponse<String> respuesta = otra.get(RUTA_SOLICITUDES_PROPIAS + "/" + ajena);

    assertThat(respuesta.statusCode())
        .as("404 y no 403: distinguirlos permitiría enumerar expedientes ajenos")
        .isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SOLICITUD_NO_ENCONTRADA");
    assertThat(otra.get(RUTA_SOLICITUDES_PROPIAS).body()).isEqualTo("[]");
  }

  @Test
  void elPropietarioNoTieneNingunaFormaDeAbrirSuPropioDocumento() {
    long solicitud = enviarBasicaCorrecta();
    long idDocumento =
        Objects.requireNonNull(
            jdbc.queryForObject(
                "SELECT id_documento_verificacion FROM documento_verificacion_prestador",
                Long.class));

    HttpResponse<String> propia =
        navegador.get(RUTA_SOLICITUDES_PROPIAS + "/" + solicitud + "/documentos/" + idDocumento);
    assertThat(propia.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

    HttpResponse<String> administrativa =
        navegador.get(RUTA_REVISION + "/" + solicitud + "/documentos/" + idDocumento + "/acceso");
    assertThat(administrativa.statusCode())
        .as("la ruta administrativa existe, pero esta sesión no tiene permisos")
        .isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void unaCuentaRestringidaNoPuedeEnviarPeroConservaLaLecturaDeLoSuyo() {
    enviarBasicaCorrecta();
    restringirCuenta(CORREO);

    HttpResponse<String> envio = enviarExpediente("BASICA", cedula());
    assertThat(envio.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(envio)).isEqualTo("CUENTA_RESTRINGIDA");

    assertThat(navegador.get(RUTA_SOLICITUDES_PROPIAS).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(navegador.get(RUTA_VERIFICACION_PROPIA).statusCode())
        .isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void sinSesionNoSePuedeConsultarNiEnviar() {
    NavegadorDePrueba anonimo = abrirNavegador();

    assertThat(anonimo.get(RUTA_VERIFICACION_PROPIA).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(enviarExpediente(anonimo, "BASICA", cedula()).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void enviarElExpedienteSinTokenCsrfEstaProhibido() {
    HttpResponse<String> respuesta =
        navegador.postFormularioSinTokenCsrf(
            RUTA_SOLICITUDES_PROPIAS,
            List.of(
                new NavegadorDePrueba.ParteDeArchivo(
                    "archivo", "cedula.png", "image/png", documentoPng())),
            List.of(
                new NavegadorDePrueba.CampoDeFormulario("nivelSolicitado", "BASICA"),
                new NavegadorDePrueba.CampoDeFormulario("tipoDocumento", "IDENTIDAD")));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(solicitudesGuardadas()).isZero();
    assertThat(documentos.cantidadDeObjetos()).isZero();
  }
}
