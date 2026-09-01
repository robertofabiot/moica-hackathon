package com.moica.calificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.solicitud.EscenarioDeSolicitud;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * Punto de partida de las pruebas de calificaciones y reputación.
 *
 * <p>Parte del escenario de solicitudes: hay un servicio contratable del prestador de la sesión y
 * un cliente distinto. Aquí se añaden las fábricas para dejar una solicitud completada —el único
 * estado que habilita calificar— y las llamadas a las tres superficies de P8.
 */
public abstract class EscenarioDeCalificacion extends EscenarioDeSolicitud {

  protected long idServicio;
  protected NavegadorDePrueba cliente;

  @BeforeEach
  protected void prepararCalificaciones() {
    idServicio = publicarServicioActivo();
    cliente = clienteAutenticado();
  }

  protected String rutaDeCalificacion(long idSolicitud) {
    return RUTA_SOLICITUDES + "/" + idSolicitud + "/calificacion";
  }

  protected String rutaDeReputacionDelCliente(long idSolicitud) {
    return RUTA_SOLICITUDES + "/" + idSolicitud + "/reputacion-del-cliente";
  }

  /** Una solicitud del cliente del escenario que el prestador aceptó y luego completó. */
  protected long solicitudCompletada() {
    long idSolicitud = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(aceptar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(completar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());
    return idSolicitud;
  }

  /** Una solicitud que el prestador aceptó pero todavía no cerró. */
  protected long solicitudAceptada() {
    long idSolicitud = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(aceptar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());
    return idSolicitud;
  }

  protected HttpResponse<String> leerCalificacion(NavegadorDePrueba desde, long idSolicitud) {
    return desde.get(rutaDeCalificacion(idSolicitud));
  }

  protected HttpResponse<String> calificar(
      NavegadorDePrueba desde, long idSolicitud, int puntuacion) {
    return calificar(desde, idSolicitud, puntuacion, null);
  }

  protected HttpResponse<String> calificar(
      NavegadorDePrueba desde, long idSolicitud, int puntuacion, String comentario) {
    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("puntuacion", puntuacion);
    cuerpo.put("comentario", comentario);
    return desde.post(rutaDeCalificacion(idSolicitud), cuerpo);
  }

  /** Envía el cuerpo tal cual, para poder mandar a propósito algo que no cuadra. */
  protected HttpResponse<String> calificarCon(
      NavegadorDePrueba desde, long idSolicitud, Map<String, Object> cuerpo) {
    return desde.post(rutaDeCalificacion(idSolicitud), cuerpo);
  }

  protected HttpResponse<String> leerReputacionDelCliente(
      NavegadorDePrueba desde, long idSolicitud) {
    return desde.get(rutaDeReputacionDelCliente(idSolicitud));
  }

  /** El detalle público del servicio, que es una de las superficies que publica la reputación. */
  protected JsonNode detallePublico(long idServicioPublicado) {
    HttpResponse<String> respuesta =
        abrirNavegador().get(RUTA_SERVICIOS_PUBLICOS + "/" + idServicioPublicado);
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    return json(respuesta);
  }

  protected int calificacionesEnBase(long idSolicitud) {
    Integer total =
        jdbc.queryForObject(
            "SELECT count(*) FROM calificacion_usuario WHERE id_solicitud_servicio = ?",
            Integer.class,
            idSolicitud);
    assertThat(total).isNotNull();
    return total;
  }

  /** El rol que quedó guardado, para comprobar que lo derivó el servidor y no el navegador. */
  protected String rolEnBase(long idSolicitud, long idCalificador) {
    return jdbc.queryForObject(
        """
        SELECT rol_calificado FROM calificacion_usuario
        WHERE id_solicitud_servicio = ? AND id_calificador = ?
        """,
        String.class,
        idSolicitud,
        idCalificador);
  }

  protected String comentarioEnBase(long idSolicitud, long idCalificador) {
    return jdbc.queryForObject(
        """
        SELECT comentario FROM calificacion_usuario
        WHERE id_solicitud_servicio = ? AND id_calificador = ?
        """,
        String.class,
        idSolicitud,
        idCalificador);
  }
}
