package com.moica.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.solicitud.EscenarioDeSolicitud;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpStatus;

/**
 * Punto de partida de las pruebas de reportes y casos de moderación.
 *
 * <p>Parte del escenario de solicitudes: hay un servicio contratable del prestador de la sesión y
 * un cliente distinto. Aquí se añaden las fábricas para dejar una solicitud en cada uno de los
 * estados que deciden si se puede reportar, y las llamadas a las dos superficies de P9.
 *
 * <p>La distinción que más se usa es la que separa {@link #solicitudCanceladaTrasAceptar} de {@link
 * #solicitudCanceladaSinAceptar}: las dos terminan en {@code CANCELADA} y solo la primera admite un
 * reporte, porque solo en ella hubo trato.
 */
public abstract class EscenarioDeModeracion extends EscenarioDeSolicitud {

  protected static final String MOTIVO = "Trato irrespetuoso";
  protected static final String DESCRIPCION =
      "Durante la visita usó insultos y se negó a terminar el trabajo acordado.";

  protected long idServicio;
  protected NavegadorDePrueba cliente;

  @BeforeEach
  protected void prepararModeracion() {
    idServicio = publicarServicioActivo();
    cliente = clienteAutenticado();
  }

  protected String rutaDeCaso(long idSolicitud) {
    return RUTA_SOLICITUDES + "/" + idSolicitud + "/caso-moderacion";
  }

  /** Una solicitud del cliente del escenario que el prestador aceptó y todavía no cerró. */
  protected long solicitudAceptada() {
    long idSolicitud = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(aceptar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());
    return idSolicitud;
  }

  /** Una solicitud aceptada que el prestador marcó después como completada. */
  protected long solicitudCompletada() {
    long idSolicitud = solicitudAceptada();
    assertThat(completar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());
    return idSolicitud;
  }

  /** Una solicitud que el cliente canceló después de que el prestador la aceptara. */
  protected long solicitudCanceladaTrasAceptar() {
    long idSolicitud = solicitudAceptada();
    assertThat(cancelarConMotivo(cliente, idSolicitud, "Ya no lo necesito.").statusCode())
        .isEqualTo(HttpStatus.OK.value());
    return idSolicitud;
  }

  /** Una solicitud que sigue esperando la decisión del prestador. */
  protected long solicitudPendiente() {
    return idDeSolicitud(enviarSolicitud(cliente, idServicio));
  }

  /** Una solicitud que el prestador rechazó sin llegar a aceptarla. */
  protected long solicitudRechazada() {
    long idSolicitud = solicitudPendiente();
    assertThat(rechazar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());
    return idSolicitud;
  }

  /** Una solicitud que el cliente canceló mientras seguía pendiente: nunca hubo trato. */
  protected long solicitudCanceladaSinAceptar() {
    long idSolicitud = solicitudPendiente();
    assertThat(cancelar(cliente, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());
    return idSolicitud;
  }

  protected HttpResponse<String> leerReporte(NavegadorDePrueba desde, long idSolicitud) {
    return desde.get(rutaDeCaso(idSolicitud));
  }

  protected HttpResponse<String> reportar(NavegadorDePrueba desde, long idSolicitud) {
    return reportar(desde, idSolicitud, MOTIVO, DESCRIPCION);
  }

  protected HttpResponse<String> reportar(
      NavegadorDePrueba desde, long idSolicitud, String motivo, String descripcion) {
    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("motivo", motivo);
    cuerpo.put("descripcion", descripcion);
    return desde.post(rutaDeCaso(idSolicitud), cuerpo);
  }

  /** Envía el cuerpo tal cual, para poder mandar a propósito algo que no cuadra. */
  protected HttpResponse<String> reportarCon(
      NavegadorDePrueba desde, long idSolicitud, Map<String, Object> cuerpo) {
    return desde.post(rutaDeCaso(idSolicitud), cuerpo);
  }

  protected long idDeCaso(HttpResponse<String> respuesta) {
    return json(respuesta).get("idCasoModeracion").asLong();
  }

  protected int casosEnBase(long idSolicitud) {
    Integer total =
        jdbc.queryForObject(
            "SELECT count(*) FROM caso_moderacion WHERE id_solicitud_servicio = ?",
            Integer.class,
            idSolicitud);
    assertThat(total).isNotNull();
    return total;
  }

  protected int versionesEnBase(long idCaso) {
    Integer total =
        jdbc.queryForObject(
            "SELECT count(*) FROM historial_caso WHERE id_caso_moderacion = ?",
            Integer.class,
            idCaso);
    assertThat(total).isNotNull();
    return total;
  }

  /** La versión vigente del caso, con las columnas que la apertura debe haber dejado escritas. */
  protected Map<String, Object> versionActual(long idCaso) {
    return jdbc.queryForMap(
        """
        SELECT id_usuario_afectado, id_actor, id_administrador_responsable,
               id_medida_administrativa, numero_version, tipo_actor, tipo_evento,
               estado_caso, resultado_caso, estado_cuenta, resolucion,
               fecha_fin_medida, detalle_cambio, fecha_inicio_vigencia,
               fecha_fin_vigencia, es_version_actual, fecha_registro
        FROM historial_caso
        WHERE id_caso_moderacion = ? AND es_version_actual
        """,
        idCaso);
  }

  /** El caso tal como quedó guardado, para comprobar lo que el servidor derivó por su cuenta. */
  protected Map<String, Object> casoEnBase(long idCaso) {
    return jdbc.queryForMap(
        """
        SELECT id_solicitud_servicio, id_reportante, id_reportado,
               id_administrador_responsable, id_medida_administrativa_actual,
               motivo, descripcion, estado_actual, resultado_actual,
               resolucion_actual, fecha_fin_medida_actual, fecha_apertura,
               fecha_cierre_actual, fecha_actualizacion
        FROM caso_moderacion
        WHERE id_caso_moderacion = ?
        """,
        idCaso);
  }

  protected String estadoDeCuentaEnBase(String correo) {
    return jdbc.queryForObject(
        "SELECT estado_cuenta FROM usuario WHERE correo_electronico = ?", String.class, correo);
  }
}
