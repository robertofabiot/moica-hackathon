package com.moica.solicitud;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.servicio.EscenarioDeServicio;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Punto de partida de las pruebas del ciclo de solicitudes.
 *
 * <p>Parte del escenario de servicios: el prestador de la sesión ya tiene perfil. Aquí se publica
 * un servicio contratable y se abre un cliente distinto.
 */
public abstract class EscenarioDeSolicitud extends EscenarioDeServicio {

  protected static final String RUTA_SOLICITUDES = "/api/solicitudes";
  protected static final String CORREO_CLIENTE = "cliente@moica.test";
  protected static final String CORREO_TERCERO = "tercero@moica.test";

  protected long publicarServicioActivo() {
    aprobarBasica(administradora(CORREO_ADMIN));
    long id = idDe(crearServicio("Reparación de fugas"));
    assertThat(activar(id).statusCode()).isEqualTo(HttpStatus.OK.value());
    return id;
  }

  protected NavegadorDePrueba clienteAutenticado() {
    return cuentaAutenticada(CORREO_CLIENTE);
  }

  protected NavegadorDePrueba cuentaAutenticada(String correo) {
    NavegadorDePrueba persona = abrirNavegador();
    registrar(persona, correo, CLAVE);
    assertThat(iniciarSesion(persona, correo, CLAVE).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    return persona;
  }

  protected Map<String, Object> pedidoDeSolicitud(long idServicio) {
    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("idServicioPublicado", idServicio);
    cuerpo.put("descripcionNecesidad", "Se fugará el lavamanos del baño principal.");
    cuerpo.put("idMunicipio", idMunicipioManagua());
    cuerpo.put("indicacionUbicacion", "De la UCA dos cuadras al lago, portón verde.");
    cuerpo.put("fechaPreferida", LocalDate.of(2026, 9, 15).toString());
    return cuerpo;
  }

  protected HttpResponse<String> enviarSolicitud(NavegadorDePrueba desde, long idServicio) {
    HttpResponse<String> respuesta = desde.post(RUTA_SOLICITUDES, pedidoDeSolicitud(idServicio));
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return respuesta;
  }

  protected long idDeSolicitud(HttpResponse<String> respuesta) {
    return json(respuesta).get("idSolicitudServicio").asLong();
  }

  protected String estadoActualEnBase(long idSolicitud) {
    return jdbc.queryForObject(
        "SELECT estado_actual FROM solicitud_servicio WHERE id_solicitud_servicio = ?",
        String.class,
        idSolicitud);
  }

  protected String ultimoEstadoDelHistorial(long idSolicitud) {
    return jdbc.queryForObject(
        """
        SELECT estado_nuevo FROM cambio_estado_solicitud
        WHERE id_solicitud_servicio = ?
        ORDER BY fecha_cambio DESC, id_cambio_estado_solicitud DESC
        LIMIT 1
        """,
        String.class,
        idSolicitud);
  }

  protected int cambiosRegistrados(long idSolicitud) {
    Integer cambios =
        jdbc.queryForObject(
            "SELECT count(*) FROM cambio_estado_solicitud WHERE id_solicitud_servicio = ?",
            Integer.class,
            idSolicitud);
    assertThat(cambios).isNotNull();
    return cambios;
  }

  protected HttpResponse<String> aceptar(NavegadorDePrueba desde, long idSolicitud) {
    return desde.post(RUTA_SOLICITUDES + "/" + idSolicitud + "/aceptacion", Map.of());
  }

  protected HttpResponse<String> rechazar(NavegadorDePrueba desde, long idSolicitud) {
    return desde.post(RUTA_SOLICITUDES + "/" + idSolicitud + "/rechazo", Map.of());
  }

  protected HttpResponse<String> cancelar(NavegadorDePrueba desde, long idSolicitud) {
    return desde.post(RUTA_SOLICITUDES + "/" + idSolicitud + "/cancelacion", Map.of());
  }

  protected HttpResponse<String> cancelarConMotivo(
      NavegadorDePrueba desde, long idSolicitud, String motivo) {
    return desde.post(
        RUTA_SOLICITUDES + "/" + idSolicitud + "/cancelacion", Map.of("motivo", motivo));
  }

  protected HttpResponse<String> completar(NavegadorDePrueba desde, long idSolicitud) {
    return desde.post(RUTA_SOLICITUDES + "/" + idSolicitud + "/completado", Map.of());
  }
}
