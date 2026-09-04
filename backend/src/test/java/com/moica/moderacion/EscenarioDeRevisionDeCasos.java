package com.moica.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Punto de partida de las pruebas de la revisión administrativa de casos.
 *
 * <p>Hereda el escenario de P9 —hay un servicio, un cliente y las fábricas de solicitudes en cada
 * estado— y añade lo propio de P10A: cuentas administradoras con segundo factor verificado y las
 * llamadas a {@code /api/admin/casos}.
 *
 * <p>Las administradoras se crean por {@link #administradora(String)}, que registra la cuenta, le
 * da el rol por SQL —no hay endpoint que lo conceda— e inicia sesión con el segundo factor
 * verificado. Es exactamente la condición que la cadena de seguridad exige para entrar en el área.
 */
public abstract class EscenarioDeRevisionDeCasos extends EscenarioDeModeracion {

  protected static final String RUTA_CASOS = "/api/admin/casos";
  protected static final String RUTA_ADMINISTRADORES = "/api/admin/administradores";

  protected static final String CORREO_ADMIN = "moderadora@moica.test";
  protected static final String CORREO_OTRO_ADMIN = "moderador.dos@moica.test";

  protected static final String RESOLUCION =
      "Se revisaron los mensajes y la solicitud; la conducta reportada quedó acreditada.";

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

  /** Un caso recién abierto por el cliente sobre una solicitud aceptada. */
  protected long casoAbierto() {
    HttpResponse<String> reporte = reportar(cliente, solicitudAceptada());
    assertThat(reporte.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return idDeCaso(reporte);
  }

  /** Un caso ya asignado a esa administradora y puesto en revisión por ella. */
  protected long casoEnRevisionDe(NavegadorDePrueba admin, String correoDelAdmin) {
    long idCaso = casoAbierto();
    assertThat(asignar(admin, idCaso, idDe(correoDelAdmin)).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(iniciarRevision(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    return idCaso;
  }

  /** El identificador de la cuenta de un correo, que las pruebas necesitan para asignar. */
  protected Long idDe(String correo) {
    return jdbc.queryForObject(
        "SELECT id_usuario FROM usuario WHERE correo_electronico = ?", Long.class, correo);
  }

  /** Un mensaje del hilo de la solicitud, para poblar la evidencia que el caso hará visible. */
  protected HttpResponse<String> enviarMensaje(
      NavegadorDePrueba desde, long idSolicitud, String contenido) {
    return desde.post(
        RUTA_SOLICITUDES + "/" + idSolicitud + "/mensajes", Map.of("contenido", contenido));
  }

  protected String rutaDeExpediente(long idCaso) {
    return RUTA_CASOS + "/" + idCaso;
  }

  protected HttpResponse<String> consultarBandeja(NavegadorDePrueba desde) {
    return desde.get(RUTA_CASOS);
  }

  protected HttpResponse<String> consultarBandeja(NavegadorDePrueba desde, String consulta) {
    return desde.get(RUTA_CASOS + "?" + consulta);
  }

  protected HttpResponse<String> consultarExpediente(NavegadorDePrueba desde, long idCaso) {
    return desde.get(rutaDeExpediente(idCaso));
  }

  protected HttpResponse<String> consultarMensajes(NavegadorDePrueba desde, long idCaso) {
    return desde.get(rutaDeExpediente(idCaso) + "/mensajes");
  }

  protected HttpResponse<String> asignar(
      NavegadorDePrueba desde, long idCaso, long idAdministrador) {
    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("idAdministrador", idAdministrador);
    return desde.post(rutaDeExpediente(idCaso) + "/asignacion", cuerpo);
  }

  protected HttpResponse<String> iniciarRevision(NavegadorDePrueba desde, long idCaso) {
    return desde.post(rutaDeExpediente(idCaso) + "/revision", Map.of());
  }

  protected HttpResponse<String> cerrar(NavegadorDePrueba desde, long idCaso, String resultado) {
    return cerrar(desde, idCaso, resultado, RESOLUCION);
  }

  protected HttpResponse<String> cerrar(
      NavegadorDePrueba desde, long idCaso, String resultado, String resolucion) {
    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("resultado", resultado);
    cuerpo.put("resolucion", resolucion);
    return desde.post(rutaDeExpediente(idCaso) + "/cierre", cuerpo);
  }

  /** Los identificadores de los casos que la bandeja devolvió, en su orden. */
  protected List<Long> idsDeLaBandeja(HttpResponse<String> respuesta) {
    List<Long> ids = new ArrayList<>();
    json(respuesta).forEach(caso -> ids.add(caso.get("idCasoModeracion").asLong()));
    return ids;
  }

  /** Las versiones del historial, de la más antigua a la más reciente. */
  protected List<Map<String, Object>> versionesEnOrden(long idCaso) {
    return jdbc.queryForList(
        """
        SELECT numero_version, tipo_actor, tipo_evento, estado_caso, resultado_caso,
               estado_cuenta, resolucion, id_actor, id_administrador_responsable,
               detalle_cambio, fecha_inicio_vigencia, fecha_fin_vigencia, es_version_actual
        FROM historial_caso
        WHERE id_caso_moderacion = ?
        ORDER BY numero_version
        """,
        idCaso);
  }

  /** Cuántas versiones dicen ser la vigente. El esquema solo admite una. */
  protected int versionesVigentes(long idCaso) {
    Integer total =
        jdbc.queryForObject(
            "SELECT count(*) FROM historial_caso WHERE id_caso_moderacion = ? AND es_version_actual",
            Integer.class,
            idCaso);
    assertThat(total).isNotNull();
    return total;
  }

  /**
   * Cuántas parejas de versiones del caso solapan sus periodos de vigencia.
   *
   * <p>Debe ser siempre cero. Lo comprueba con los mismos rangos semiabiertos que usa {@code
   * ex_historial_caso_vigencia}, de modo que la prueba falle igual aunque alguien retirase la
   * restricción.
   */
  protected int vigenciasSuperpuestas(long idCaso) {
    Integer total =
        jdbc.queryForObject(
            """
            SELECT count(*)
            FROM historial_caso a
            JOIN historial_caso b
              ON a.id_caso_moderacion = b.id_caso_moderacion
             AND a.id_historial_caso < b.id_historial_caso
            WHERE a.id_caso_moderacion = ?
              AND tstzrange(a.fecha_inicio_vigencia, a.fecha_fin_vigencia, '[)')
               && tstzrange(b.fecha_inicio_vigencia, b.fecha_fin_vigencia, '[)')
            """,
            Integer.class,
            idCaso);
    assertThat(total).isNotNull();
    return total;
  }
}
