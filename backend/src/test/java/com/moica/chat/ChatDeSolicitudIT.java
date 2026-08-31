package com.moica.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * El hilo de mensajes de una solicitud, endpoint a endpoint.
 *
 * <p>Cubre quién lee, quién escribe y en qué estados. Lo que la interfaz muestre u oculte no cuenta
 * como control: aquí se comprueba lo que hace el backend ante la petición directa.
 */
class ChatDeSolicitudIT extends EscenarioDeChat {

  @Test
  void losDosParticipantesLeenYEscribenEnUnHiloAceptado() {
    long idSolicitud = solicitudAceptada();

    assertThat(
            enviarMensaje(cliente, idSolicitud, "Buenos días, ¿a qué hora puede llegar?")
                .statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    assertThat(enviarMensaje(navegador, idSolicitud, "Voy a las tres de la tarde.").statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    HttpResponse<String> desdeElCliente = leerMensajes(cliente, idSolicitud);
    HttpResponse<String> desdeElPrestador = leerMensajes(navegador, idSolicitud);

    assertThat(desdeElCliente.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(desdeElPrestador.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(contenidosDe(desdeElCliente))
        .containsExactly("Buenos días, ¿a qué hora puede llegar?", "Voy a las tres de la tarde.");
    assertThat(contenidosDe(desdeElPrestador)).isEqualTo(contenidosDe(desdeElCliente));
  }

  @Test
  void elMensajeIdentificaAQuienLoEscribioConSuNombre() {
    long idSolicitud = solicitudAceptada();
    HttpResponse<String> enviado =
        enviarMensaje(cliente, idSolicitud, "Ya dejé el portón abierto.");

    JsonNode mensaje = json(enviado);
    assertThat(mensaje.get("nombreRemitente").asText()).isEqualTo("Persona de Prueba");
    assertThat(mensaje.get("idSolicitudServicio").asLong()).isEqualTo(idSolicitud);
    assertThat(mensaje.get("fechaEnvio").asText()).isNotBlank();
  }

  @Test
  void unTerceroRecibe404AlLeerYAlEscribir() {
    long idSolicitud = solicitudAceptada();
    NavegadorDePrueba tercero = cuentaAutenticada(CORREO_TERCERO);

    HttpResponse<String> lectura = leerMensajes(tercero, idSolicitud);
    HttpResponse<String> escritura = enviarMensaje(tercero, idSolicitud, "Me cuelo en la charla.");

    assertThat(lectura.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(lectura)).isEqualTo("RECURSO_NO_ENCONTRADO");
    assertThat(escritura.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(mensajesEnBase(idSolicitud)).isZero();
  }

  @Test
  void unaSolicitudPendienteNoTieneHilo() {
    long idSolicitud = solicitudPendiente();

    HttpResponse<String> lectura = leerMensajes(cliente, idSolicitud);
    HttpResponse<String> escritura = enviarMensaje(cliente, idSolicitud, "¿Me la acepta?");

    assertThat(lectura.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(lectura)).isEqualTo("CHAT_NO_HABILITADO");
    assertThat(escritura.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(escritura)).isEqualTo("CHAT_NO_HABILITADO");
    assertThat(mensajesEnBase(idSolicitud)).isZero();
  }

  @Test
  void unaSolicitudRechazadaNoAbreElHilo() {
    long idSolicitud = solicitudPendiente();
    assertThat(rechazar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(codigoDeError(leerMensajes(cliente, idSolicitud))).isEqualTo("CHAT_NO_HABILITADO");
    assertThat(codigoDeError(enviarMensaje(navegador, idSolicitud, "Aun así te escribo.")))
        .isEqualTo("CHAT_NO_HABILITADO");
  }

  @Test
  void unaCancelacionDesdePendienteNoAbreElHilo() {
    long idSolicitud = solicitudPendiente();
    assertThat(cancelar(cliente, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(estadoActualEnBase(idSolicitud)).isEqualTo("CANCELADA");

    HttpResponse<String> lectura = leerMensajes(cliente, idSolicitud);

    assertThat(lectura.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(lectura)).isEqualTo("CHAT_NO_HABILITADO");
    assertThat(codigoDeError(enviarMensaje(cliente, idSolicitud, "Cambié de opinión.")))
        .isEqualTo("CHAT_NO_HABILITADO");
  }

  @Test
  void unaCancelacionPosteriorALaAceptacionDejaElHistorialEnSoloLectura() {
    long idSolicitud = solicitudAceptada();
    assertThat(enviarMensaje(cliente, idSolicitud, "Nos vemos el martes.").statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    assertThat(cancelarConMotivo(cliente, idSolicitud, "Se resolvió solo.").statusCode())
        .isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> lectura = leerMensajes(cliente, idSolicitud);
    HttpResponse<String> escritura = enviarMensaje(navegador, idSolicitud, "Una lástima.");

    assertThat(lectura.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(contenidosDe(lectura)).containsExactly("Nos vemos el martes.");
    assertThat(leerMensajes(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(escritura.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(escritura)).isEqualTo("CHAT_SOLO_LECTURA");
    assertThat(mensajesEnBase(idSolicitud)).isEqualTo(1);
  }

  @Test
  void unaSolicitudCompletadaDejaElHistorialEnSoloLectura() {
    long idSolicitud = solicitudAceptada();
    assertThat(enviarMensaje(navegador, idSolicitud, "Trabajo terminado.").statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    assertThat(completar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> lectura = leerMensajes(cliente, idSolicitud);
    HttpResponse<String> escritura = enviarMensaje(cliente, idSolicitud, "Gracias, quedó bien.");

    assertThat(lectura.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(contenidosDe(lectura)).containsExactly("Trabajo terminado.");
    assertThat(codigoDeError(escritura)).isEqualTo("CHAT_SOLO_LECTURA");
    assertThat(mensajesEnBase(idSolicitud)).isEqualTo(1);
  }

  @Test
  void unMensajeVacioOSoloConEspaciosSeRechaza() {
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> vacio = enviarMensaje(cliente, idSolicitud, "");
    HttpResponse<String> espacios = enviarMensaje(cliente, idSolicitud, "     ");
    HttpResponse<String> saltos = enviarMensaje(cliente, idSolicitud, "\n\t  ");

    assertThat(vacio.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(vacio)).isEqualTo("VALIDACION");
    assertThat(codigoDeError(espacios)).isEqualTo("VALIDACION");
    assertThat(codigoDeError(saltos)).isEqualTo("VALIDACION");
    assertThat(mensajesEnBase(idSolicitud)).isZero();
  }

  @Test
  void unMensajeMasLargoQueElTopeDeLaAplicacionSeRechaza() {
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> respuesta = enviarMensaje(cliente, idSolicitud, "a".repeat(2001));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("VALIDACION");
    assertThat(enviarMensaje(cliente, idSolicitud, "a".repeat(2000)).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
  }

  @Test
  void elRemitenteSaleDeLaSesionYNoDelCuerpo() {
    long idSolicitud = solicitudAceptada();
    long idPrestador = idDeCuenta(CORREO);

    // El cuerpo intenta atribuir el mensaje al prestador; la sesión es la del
    // cliente y es la sesión la que manda.
    HttpResponse<String> respuesta =
        cliente.post(
            rutaDeMensajes(idSolicitud),
            Map.of("contenido", "Escrito por el cliente.", "idRemitente", idPrestador));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(respuesta).get("idRemitente").asLong()).isEqualTo(idDeCuenta(CORREO_CLIENTE));
    assertThat(
            jdbc.queryForObject(
                "SELECT id_remitente FROM mensaje_solicitud WHERE id_solicitud_servicio = ?",
                Long.class,
                idSolicitud))
        .isEqualTo(idDeCuenta(CORREO_CLIENTE));
  }

  @Test
  void unaCuentaRestringidaLeeElHiloPeroNoEscribe() {
    long idSolicitud = solicitudAceptada();
    assertThat(enviarMensaje(navegador, idSolicitud, "Confirmo la visita.").statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    restringirCuenta(CORREO_CLIENTE);

    HttpResponse<String> lectura = leerMensajes(cliente, idSolicitud);
    HttpResponse<String> escritura = enviarMensaje(cliente, idSolicitud, "Perfecto, te espero.");

    assertThat(lectura.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(contenidosDe(lectura)).containsExactly("Confirmo la visita.");
    assertThat(escritura.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(escritura)).isEqualTo("CUENTA_RESTRINGIDA");
    assertThat(mensajesEnBase(idSolicitud)).isEqualTo(1);
  }

  @Test
  void unaCuentaSuspendidaNoLlegaAlHilo() {
    long idSolicitud = solicitudAceptada();
    suspenderCuenta(CORREO_CLIENTE);

    HttpResponse<String> lectura = leerMensajes(cliente, idSolicitud);

    assertThat(lectura.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(lectura)).isEqualTo("ACCESO_DENEGADO");
  }

  @Test
  void sinSesionNoSeLeeNiSeEscribe() {
    long idSolicitud = solicitudAceptada();
    NavegadorDePrueba anonimo = abrirNavegador();

    assertThat(leerMensajes(anonimo, idSolicitud).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(enviarMensaje(anonimo, idSolicitud, "Hola").statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void elHiloConservaUnOrdenEstable() {
    long idSolicitud = solicitudAceptada();
    for (int numero = 1; numero <= 6; numero++) {
      NavegadorDePrueba desde = (numero % 2 == 0) ? navegador : cliente;
      assertThat(enviarMensaje(desde, idSolicitud, "Mensaje " + numero).statusCode())
          .isEqualTo(HttpStatus.CREATED.value());
    }

    assertThat(contenidosDe(leerMensajes(cliente, idSolicitud)))
        .containsExactly(
            "Mensaje 1", "Mensaje 2", "Mensaje 3", "Mensaje 4", "Mensaje 5", "Mensaje 6");
    // Dos lecturas seguidas devuelven exactamente lo mismo: el orden no depende
    // de cómo decida devolver las filas PostgreSQL.
    assertThat(contenidosDe(leerMensajes(navegador, idSolicitud)))
        .containsExactly(
            "Mensaje 1", "Mensaje 2", "Mensaje 3", "Mensaje 4", "Mensaje 5", "Mensaje 6");
  }

  @Test
  void unParticipanteNoAlcanzaElHiloDeOtraSolicitud() {
    long propia = solicitudAceptada();
    NavegadorDePrueba otroCliente = cuentaAutenticada(CORREO_TERCERO);
    long ajena = idDeSolicitud(enviarSolicitud(otroCliente, idServicio));
    assertThat(aceptar(navegador, ajena).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(enviarMensaje(otroCliente, ajena, "Hilo del otro cliente.").statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    assertThat(leerMensajes(cliente, ajena).statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(leerMensajes(otroCliente, propia).statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
    // El prestador sí participa en ambas, y cada hilo trae solo lo suyo.
    assertThat(contenidosDe(leerMensajes(navegador, ajena)))
        .containsExactly("Hilo del otro cliente.");
    assertThat(contenidosDe(leerMensajes(navegador, propia))).isEmpty();
  }

  @Test
  void elHiloDeUnaSolicitudInexistenteResponde404() {
    long idSolicitud = solicitudAceptada();

    assertThat(leerMensajes(cliente, idSolicitud + 10_000).statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(enviarMensaje(cliente, idSolicitud + 10_000, "Hola").statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void noExistenBorradoNiEdicionDeMensajes() {
    long idSolicitud = solicitudAceptada();
    assertThat(enviarMensaje(cliente, idSolicitud, "Sin retoques.").statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    HttpResponse<String> borrado = cliente.delete(rutaDeMensajes(idSolicitud));
    HttpResponse<String> edicion =
        cliente.put(rutaDeMensajes(idSolicitud), Map.of("contenido", "Editado"));

    assertThat(borrado.statusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    assertThat(codigoDeError(borrado)).isEqualTo("METODO_NO_PERMITIDO");
    assertThat(edicion.statusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    assertThat(contenidosDe(leerMensajes(cliente, idSolicitud))).containsExactly("Sin retoques.");
  }

  @Test
  void elCuerpoNoFiltraCorreosHashesNiSecretos() {
    long idSolicitud = solicitudAceptada();
    assertThat(enviarMensaje(cliente, idSolicitud, "Confirmado.").statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    String hilo = leerMensajes(navegador, idSolicitud).body();

    assertThat(hilo)
        .doesNotContain("@moica.test")
        .doesNotContain("correoElectronico")
        .doesNotContain("claveHash")
        .doesNotContain("secretoTotp")
        .doesNotContain("claveAlmacenamiento")
        .doesNotContain("identificadorToken");
  }
}
