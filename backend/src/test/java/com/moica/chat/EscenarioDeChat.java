package com.moica.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.solicitud.EscenarioDeSolicitud;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpStatus;

/**
 * Punto de partida de las pruebas del hilo de mensajes y de la revelación de contactos.
 *
 * <p>Parte del escenario de solicitudes: hay un servicio contratable del prestador de la sesión y
 * un cliente distinto. Aquí se añaden las fábricas para dejar una solicitud en el estado que cada
 * prueba necesita y las llamadas a las dos superficies de P7.
 */
public abstract class EscenarioDeChat extends EscenarioDeSolicitud {

  protected long idServicio;
  protected NavegadorDePrueba cliente;

  @BeforeEach
  protected void prepararHilo() {
    idServicio = publicarServicioActivo();
    cliente = clienteAutenticado();
  }

  protected String rutaDeMensajes(long idSolicitud) {
    return RUTA_SOLICITUDES + "/" + idSolicitud + "/mensajes";
  }

  protected String rutaDeContactos(long idSolicitud) {
    return RUTA_SOLICITUDES + "/" + idSolicitud + "/contactos";
  }

  /** Una solicitud recién enviada por el cliente del escenario. */
  protected long solicitudPendiente() {
    return idDeSolicitud(enviarSolicitud(cliente, idServicio));
  }

  /** Una solicitud que el prestador del escenario ya aceptó. */
  protected long solicitudAceptada() {
    long idSolicitud = solicitudPendiente();
    assertThat(aceptar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());
    return idSolicitud;
  }

  protected HttpResponse<String> leerMensajes(NavegadorDePrueba desde, long idSolicitud) {
    return desde.get(rutaDeMensajes(idSolicitud));
  }

  protected HttpResponse<String> enviarMensaje(
      NavegadorDePrueba desde, long idSolicitud, String contenido) {
    return desde.post(rutaDeMensajes(idSolicitud), Map.of("contenido", contenido));
  }

  protected HttpResponse<String> leerContactos(NavegadorDePrueba desde, long idSolicitud) {
    return desde.get(rutaDeContactos(idSolicitud));
  }

  /** Agrega un medio de contacto al perfil del prestador del escenario. */
  protected long agregarContacto(String contenido) {
    HttpResponse<String> respuesta = navegador.post(RUTA_CONTACTOS, Map.of("contenido", contenido));
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return json(respuesta).get("idMedioContactoPrestador").asLong();
  }

  /** El identificador de una cuenta registrada, para comprobar quién quedó como remitente. */
  protected long idDeCuenta(String correo) {
    Long idUsuario =
        jdbc.queryForObject(
            "SELECT id_usuario FROM usuario WHERE correo_electronico = ?", Long.class, correo);
    assertThat(idUsuario).isNotNull();
    return idUsuario;
  }

  protected int mensajesEnBase(long idSolicitud) {
    Integer total =
        jdbc.queryForObject(
            "SELECT count(*) FROM mensaje_solicitud WHERE id_solicitud_servicio = ?",
            Integer.class,
            idSolicitud);
    assertThat(total).isNotNull();
    return total;
  }

  /** Los contenidos del hilo tal como los devuelve la API, en el orden en que llegan. */
  protected List<String> contenidosDe(HttpResponse<String> respuesta) {
    return StreamSupport.stream(json(respuesta).spliterator(), false)
        .map(nodo -> nodo.get("contenido").asText())
        .toList();
  }
}
