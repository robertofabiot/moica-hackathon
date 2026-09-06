package com.moica.prestador;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * Los medios de contacto propios: crearlos, editarlos, ordenarlos y eliminarlos.
 *
 * <p>La propiedad se comprueba con dos cuentas reales: lo que una guarda, la otra ni lo ve ni lo
 * puede tocar, y la respuesta es la misma que si el recurso no existiera.
 */
class MediosDeContactoIT extends EscenarioDePrestador {

  @BeforeEach
  void prepararPerfil() {
    crearPerfil();
  }

  @Test
  void agregaContactosAlFinalYLosListaEnOrden() {
    crearContacto("WhatsApp 8888-8888");
    crearContacto("taller@moica.test");

    JsonNode contactos = json(navegador.get(RUTA_CONTACTOS));

    assertThat(contactos).hasSize(2);
    assertThat(contactos.get(0).get("contenido").asText()).isEqualTo("WhatsApp 8888-8888");
    assertThat(contactos.get(0).get("ordenVisualizacion").asInt()).isZero();
    assertThat(contactos.get(1).get("contenido").asText()).isEqualTo("taller@moica.test");
    assertThat(contactos.get(1).get("ordenVisualizacion").asInt()).isEqualTo(1);
  }

  @Test
  void editaElContenidoDeUnContactoPropio() {
    Long id = crearContacto("8888-8888");

    HttpResponse<String> respuesta =
        navegador.put(RUTA_CONTACTOS + "/" + id, Map.of("contenido", "5555-5555"));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("contenido").asText()).isEqualTo("5555-5555");
  }

  @Test
  void eliminaUnContactoPropio() {
    Long id = crearContacto("8888-8888");

    assertThat(navegador.delete(RUTA_CONTACTOS + "/" + id).statusCode())
        .isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(json(navegador.get(RUTA_CONTACTOS))).isEmpty();
  }

  @Test
  void reordenaConLaListaCompletaDeIdentificadores() {
    Long primero = crearContacto("Primero");
    Long segundo = crearContacto("Segundo");
    Long tercero = crearContacto("Tercero");

    HttpResponse<String> respuesta =
        navegador.put(
            RUTA_CONTACTOS + "/orden", Map.of("idsEnOrden", List.of(tercero, primero, segundo)));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());

    JsonNode contactos = json(navegador.get(RUTA_CONTACTOS));
    assertThat(contactos.get(0).get("contenido").asText()).isEqualTo("Tercero");
    assertThat(contactos.get(1).get("contenido").asText()).isEqualTo("Primero");
    assertThat(contactos.get(2).get("contenido").asText()).isEqualTo("Segundo");
  }

  @Test
  void rechazaUnOrdenQueNoTraigaExactamenteLosExistentes() {
    Long primero = crearContacto("Primero");
    crearContacto("Segundo");

    HttpResponse<String> incompleto =
        navegador.put(RUTA_CONTACTOS + "/orden", Map.of("idsEnOrden", List.of(primero)));
    assertThat(incompleto.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(incompleto)).isEqualTo("ORDEN_INVALIDO");

    HttpResponse<String> repetido =
        navegador.put(RUTA_CONTACTOS + "/orden", Map.of("idsEnOrden", List.of(primero, primero)));
    assertThat(codigoDeError(repetido)).isEqualTo("ORDEN_INVALIDO");
  }

  @Test
  void unContactoAjenoNoSeVeNiSePuedeTocar() {
    Long idAjeno = crearContacto("Contacto de la primera cuenta");

    NavegadorDePrueba otraPersona = abrirNavegador();
    registrar(otraPersona, "otra@moica.test", CLAVE);
    iniciarSesion(otraPersona, "otra@moica.test", CLAVE);
    otraPersona.post(RUTA_PERFIL, solicitudDePerfil());

    assertThat(json(otraPersona.get(RUTA_CONTACTOS)))
        .as("la lista propia no incluye contactos de otros")
        .isEmpty();

    HttpResponse<String> edicion =
        otraPersona.put(RUTA_CONTACTOS + "/" + idAjeno, Map.of("contenido", "intruso"));
    assertThat(edicion.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

    HttpResponse<String> borrado = otraPersona.delete(RUTA_CONTACTOS + "/" + idAjeno);
    assertThat(borrado.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void sinPerfilLosContactosRespondenPerfilNoEncontrado() {
    NavegadorDePrueba sinPerfil = abrirNavegador();
    registrar(sinPerfil, "sin-perfil@moica.test", CLAVE);
    iniciarSesion(sinPerfil, "sin-perfil@moica.test", CLAVE);

    HttpResponse<String> lectura = sinPerfil.get(RUTA_CONTACTOS);
    assertThat(lectura.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(lectura)).isEqualTo("PERFIL_NO_ENCONTRADO");
  }

  @Test
  void unaCuentaRestringidaConservaLaLecturaPeroNoLasMutaciones() {
    crearContacto("8888-8888");
    restringirCuenta(CORREO);

    assertThat(navegador.get(RUTA_CONTACTOS).statusCode()).isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> creacion = navegador.post(RUTA_CONTACTOS, Map.of("contenido", "nuevo"));
    assertThat(creacion.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(creacion)).isEqualTo("CUENTA_RESTRINGIDA");
  }

  private Long crearContacto(String contenido) {
    HttpResponse<String> respuesta = navegador.post(RUTA_CONTACTOS, Map.of("contenido", contenido));
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return json(respuesta).get("idMedioContactoPrestador").asLong();
  }
}
