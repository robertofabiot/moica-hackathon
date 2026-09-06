package com.moica.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * La revelación de los contactos externos del prestador al cliente que lo contrató.
 *
 * <p>La regla que se comprueba aquí es de privacidad, no de presentación: quién recibe 200 y quién
 * no puede siquiera confirmar que el recurso exista.
 */
class RevelacionDeContactosIT extends EscenarioDeChat {

  @Test
  void elClienteRecibeLosContactosDespuesDeLaAceptacion() {
    agregarContacto("WhatsApp 8888-8888");
    agregarContacto("taller.esperanza@correo.test");
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> respuesta = leerContactos(cliente, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(contenidosDeContactos(respuesta))
        .containsExactly("WhatsApp 8888-8888", "taller.esperanza@correo.test");
  }

  @Test
  void losContactosConservanSuOrdenDeVisualizacion() {
    agregarContacto("Primero");
    agregarContacto("Segundo");
    agregarContacto("Tercero");
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> respuesta = leerContactos(cliente, idSolicitud);

    assertThat(contenidosDeContactos(respuesta)).containsExactly("Primero", "Segundo", "Tercero");
    assertThat(
            StreamSupport.stream(json(respuesta).spliterator(), false)
                .map(nodo -> nodo.get("ordenVisualizacion").asInt())
                .toList())
        .containsExactly(0, 1, 2);
  }

  @Test
  void sinContactosConfiguradosLaRespuestaEsUnaListaVacia() {
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> respuesta = leerContactos(cliente, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta)).isEmpty();
  }

  @Test
  void antesDeLaAceptacionNoHayContactos() {
    agregarContacto("WhatsApp 8888-8888");
    long idSolicitud = solicitudPendiente();

    HttpResponse<String> respuesta = leerContactos(cliente, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CONTACTOS_NO_REVELADOS");
    assertThat(respuesta.body()).doesNotContain("8888-8888");
  }

  @Test
  void unaSolicitudRechazadaOCanceladaAntesDeAceptarNoRevelaContactos() {
    agregarContacto("WhatsApp 8888-8888");
    long rechazada = solicitudPendiente();
    assertThat(rechazar(navegador, rechazada).statusCode()).isEqualTo(HttpStatus.OK.value());
    long cancelada = solicitudPendiente();
    assertThat(cancelar(cliente, cancelada).statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(codigoDeError(leerContactos(cliente, rechazada)))
        .isEqualTo("CONTACTOS_NO_REVELADOS");
    assertThat(codigoDeError(leerContactos(cliente, cancelada)))
        .isEqualTo("CONTACTOS_NO_REVELADOS");
    assertThat(leerContactos(cliente, rechazada).body()).doesNotContain("8888-8888");
  }

  @Test
  void cancelarOCompletarDespuesDeAceptarNoVuelveAOcultarLosContactos() {
    agregarContacto("WhatsApp 8888-8888");
    long cancelada = solicitudAceptada();
    assertThat(cancelarConMotivo(cliente, cancelada, "Se resolvió solo.").statusCode())
        .isEqualTo(HttpStatus.OK.value());
    long completada = solicitudAceptada();
    assertThat(completar(navegador, completada).statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(contenidosDeContactos(leerContactos(cliente, cancelada)))
        .containsExactly("WhatsApp 8888-8888");
    assertThat(contenidosDeContactos(leerContactos(cliente, completada)))
        .containsExactly("WhatsApp 8888-8888");
  }

  @Test
  void elPrestadorNoRecibeSusPropiosContactosPorEstaSuperficie() {
    agregarContacto("WhatsApp 8888-8888");
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> respuesta = leerContactos(navegador, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("RECURSO_NO_ENCONTRADO");
    // Sus propios contactos los sigue administrando en su perfil.
    assertThat(navegador.get(RUTA_CONTACTOS).statusCode()).isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void unTerceroRecibe404YNoConfirmaQueElHiloExista() {
    agregarContacto("WhatsApp 8888-8888");
    long idSolicitud = solicitudAceptada();
    NavegadorDePrueba tercero = cuentaAutenticada(CORREO_TERCERO);

    HttpResponse<String> respuesta = leerContactos(tercero, idSolicitud);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("RECURSO_NO_ENCONTRADO");
    assertThat(respuesta.body()).doesNotContain("8888-8888");
    // La misma respuesta que para una solicitud que de verdad no existe.
    assertThat(leerContactos(tercero, idSolicitud + 10_000).statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void unClienteRestringidoConservaLaRevelacion() {
    agregarContacto("WhatsApp 8888-8888");
    long idSolicitud = solicitudAceptada();
    restringirCuenta(CORREO_CLIENTE);

    assertThat(contenidosDeContactos(leerContactos(cliente, idSolicitud)))
        .containsExactly("WhatsApp 8888-8888");
  }

  @Test
  void laRevelacionNoLlevaCorreosDeCuentaNiSecretos() {
    agregarContacto("WhatsApp 8888-8888");
    long idSolicitud = solicitudAceptada();

    String cuerpo = leerContactos(cliente, idSolicitud).body();

    assertThat(cuerpo)
        .contains("8888-8888")
        .doesNotContain("@moica.test")
        .doesNotContain("correoElectronico")
        .doesNotContain("claveHash")
        .doesNotContain("secretoTotp")
        .doesNotContain("claveAlmacenamiento")
        .doesNotContain("fechaCreacion");
  }

  @Test
  void elDetalleDeLaSolicitudSigueSinLlevarContactos() {
    agregarContacto("WhatsApp 8888-8888");
    long idSolicitud = solicitudAceptada();

    String detalle = cliente.get(RUTA_SOLICITUDES + "/" + idSolicitud).body();
    String bandeja = cliente.get(RUTA_SOLICITUDES + "/enviadas").body();

    assertThat(detalle).doesNotContain("8888-8888").doesNotContain("contactos");
    assertThat(bandeja).doesNotContain("8888-8888");
  }

  @Test
  void noHayNingunaRutaPublicaParaLosContactosDeUnPrestador() {
    agregarContacto("WhatsApp 8888-8888");
    long idPrestador = idDeCuenta(CORREO);
    NavegadorDePrueba tercero = cuentaAutenticada(CORREO_TERCERO);

    // El perfil público de P5 no los publica y no existe una ruta por prestador.
    assertThat(tercero.get(RUTA_PRESTADORES_PUBLICOS + "/" + idPrestador).body())
        .doesNotContain("8888-8888");
    assertThat(tercero.get(RUTA_CONTACTOS).statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  private List<String> contenidosDeContactos(HttpResponse<String> respuesta) {
    return StreamSupport.stream(json(respuesta).spliterator(), false)
        .map(nodo -> nodo.get("contenido").asText())
        .toList();
  }
}
