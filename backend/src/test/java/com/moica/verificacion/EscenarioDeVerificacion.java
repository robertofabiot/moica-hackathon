package com.moica.verificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.NavegadorDePrueba.CampoDeFormulario;
import com.moica.NavegadorDePrueba.ParteDeArchivo;
import com.moica.comun.almacenamiento.AlmacenamientoPrivadoDePrueba;
import com.moica.prestador.EscenarioDePrestador;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * Punto de partida común de las pruebas de verificación documental de P4V.
 *
 * <p>Amplía el escenario de prestador con lo que todas necesitan: el doble del almacén privado
 * reiniciado, un perfil ya creado, fábricas de documentos con la firma binaria real de cada formato
 * y la forma de armar un expediente completo en una sola petición multipart.
 *
 * <p>También sabe montar una cuenta administradora con su segundo factor verificado, que es la
 * única que puede tocar {@code /api/admin/verificaciones}.
 */
public abstract class EscenarioDeVerificacion extends EscenarioDePrestador {

  protected static final String RUTA_VERIFICACION_PROPIA = "/api/prestador/verificacion";
  protected static final String RUTA_SOLICITUDES_PROPIAS =
      "/api/prestador/verificacion/solicitudes";
  protected static final String RUTA_REVISION = "/api/admin/verificaciones";

  protected static final String CORREO_ADMIN = "revisora@moica.test";
  protected static final String CORREO_OTRO_ADMIN = "revisor.dos@moica.test";
  protected static final String CORREO_OTRA_PERSONA = "otra.persona@moica.test";

  /** Restricción que una prueba activa para que la persistencia de documentos falle a propósito. */
  private static final String RESTRICCION_QUE_ROMPE = "ck_prueba_rechaza_documentos";

  @Autowired protected AlmacenamientoPrivadoDePrueba documentos;

  @BeforeEach
  void prepararEscenarioDeVerificacion() {
    documentos.reiniciar();
    crearPerfil();
  }

  @AfterEach
  void repararLaPersistenciaDeDocumentos() {
    jdbc.update(
        "ALTER TABLE documento_verificacion_prestador DROP CONSTRAINT IF EXISTS "
            + RESTRICCION_QUE_ROMPE);
  }

  /**
   * Hace que insertar un documento falle, sin tocar el envío ni el almacenamiento.
   *
   * <p>Es la forma realista de provocar «la base de datos rechazó la escritura» después de que los
   * archivos ya viajaron al bucket: una restricción {@code NOT VALID} no revisa las filas
   * existentes pero sí las nuevas.
   */
  protected void romperLaPersistenciaDeDocumentos() {
    jdbc.update(
        "ALTER TABLE documento_verificacion_prestador ADD CONSTRAINT "
            + RESTRICCION_QUE_ROMPE
            + " CHECK (false) NOT VALID");
  }

  /** Un documento tal como lo adjuntaría el formulario del navegador. */
  protected record DocumentoDePrueba(
      String tipoDocumento, String nombreArchivo, String tipoMime, byte[] contenido) {

    public DocumentoDePrueba {
      contenido = contenido.clone();
    }

    @Override
    public byte[] contenido() {
      return contenido.clone();
    }
  }

  /** Envía un expediente completo desde el navegador indicado, en una sola petición. */
  protected HttpResponse<String> enviarExpediente(
      NavegadorDePrueba desde, String nivelSolicitado, DocumentoDePrueba... adjuntos) {

    List<ParteDeArchivo> archivos = new ArrayList<>();
    List<CampoDeFormulario> campos = new ArrayList<>();
    campos.add(new CampoDeFormulario("nivelSolicitado", nivelSolicitado));

    for (DocumentoDePrueba adjunto : adjuntos) {
      archivos.add(
          new ParteDeArchivo(
              "archivo", adjunto.nombreArchivo(), adjunto.tipoMime(), adjunto.contenido()));
      campos.add(new CampoDeFormulario("tipoDocumento", adjunto.tipoDocumento()));
    }

    return desde.postFormulario(RUTA_SOLICITUDES_PROPIAS, archivos, campos);
  }

  /** Envía un expediente desde la sesión del escenario. */
  protected HttpResponse<String> enviarExpediente(
      String nivelSolicitado, DocumentoDePrueba... adjuntos) {
    return enviarExpediente(navegador, nivelSolicitado, adjuntos);
  }

  /** Envía una básica correcta y devuelve el identificador de la solicitud creada. */
  protected long enviarBasicaCorrecta() {
    HttpResponse<String> respuesta = enviarExpediente("BASICA", cedula());
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return json(respuesta).get("idSolicitudVerificacion").asLong();
  }

  /** Una cuenta administradora nueva, con sesión iniciada y segundo factor ya verificado. */
  protected NavegadorDePrueba administradora(String correo) {
    NavegadorDePrueba admin = abrirNavegador();
    registrar(admin, correo, CLAVE);
    darRolAdministrativo(correo);

    assertThat(iniciarSesion(admin, correo, CLAVE).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    activarSegundoFactor(admin);

    return admin;
  }

  /** Recorre el camino completo hasta dejar el perfil con la verificación básica vigente. */
  protected long aprobarBasica(NavegadorDePrueba admin) {
    long solicitud = enviarBasicaCorrecta();
    assertThat(admin.post(RUTA_REVISION + "/" + solicitud + "/toma", Map.of()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(admin.post(RUTA_REVISION + "/" + solicitud + "/aprobacion", Map.of()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    return solicitud;
  }

  protected String nivelDelPerfil() {
    return jdbc.queryForObject(
        "SELECT nivel_verificacion FROM perfil_prestador WHERE id_prestador = ?",
        String.class,
        idDe(CORREO));
  }

  protected String estadoDeLaSolicitud(long idSolicitud) {
    return jdbc.queryForObject(
        "SELECT estado_solicitud FROM solicitud_verificacion_prestador"
            + " WHERE id_solicitud_verificacion = ?",
        String.class,
        idSolicitud);
  }

  protected Long idDe(String correo) {
    return jdbc.queryForObject(
        "SELECT id_usuario FROM usuario WHERE correo_electronico = ?", Long.class, correo);
  }

  protected Integer documentosGuardados() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM documento_verificacion_prestador", Integer.class);
  }

  protected Integer solicitudesGuardadas() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM solicitud_verificacion_prestador", Integer.class);
  }

  protected JsonNode primerDocumentoDe(HttpResponse<String> respuesta) {
    return json(respuesta).get("documentos").get(0);
  }

  /** Un documento de identidad válido: PNG con su firma real. */
  protected static DocumentoDePrueba cedula() {
    return new DocumentoDePrueba("IDENTIDAD", "cedula frente.png", "image/png", documentoPng());
  }

  /** Un respaldo profesional válido: PDF con su firma real. */
  protected static DocumentoDePrueba certificado() {
    return new DocumentoDePrueba(
        "CERTIFICACION", "certificado.pdf", "application/pdf", documentoPdf());
  }

  /** Un PNG mínimo pero con la firma real de ocho bytes. */
  protected static byte[] documentoPng() {
    return conFirma(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, 64);
  }

  /** Un JPEG mínimo pero con la firma real de tres bytes. */
  protected static byte[] documentoJpeg() {
    return conFirma(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, 64);
  }

  /** Un PDF mínimo pero con la firma real {@code %PDF-}. */
  protected static byte[] documentoPdf() {
    return conFirma("%PDF-".getBytes(StandardCharsets.US_ASCII), 64);
  }

  private static byte[] conFirma(byte[] firma, int tamano) {
    byte[] contenido = new byte[tamano];
    System.arraycopy(firma, 0, contenido, 0, firma.length);
    Arrays.fill(contenido, firma.length, tamano, (byte) 0x2A);
    return contenido;
  }
}
