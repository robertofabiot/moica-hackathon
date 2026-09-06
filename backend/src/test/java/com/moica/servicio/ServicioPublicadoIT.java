package com.moica.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * Gestión propia de servicios: preparación, edición, activación y propiedad.
 *
 * <p>Un perfil sin verificar puede preparar un servicio inactivo. Activar exige cuenta activa,
 * prestador disponible y al menos verificación básica. Un recurso ajeno responde 404.
 */
class ServicioPublicadoIT extends EscenarioDeServicio {

  @Test
  void unPerfilSinVerificarPreparaUnServicioInactivo() {
    HttpResponse<String> respuesta = crearServicio("Destape de cañería");

    JsonNode cuerpo = json(respuesta);
    assertThat(cuerpo.get("nombre").asText()).isEqualTo("Destape de cañería");
    assertThat(cuerpo.get("estado").asText()).isEqualTo("INACTIVO");
    assertThat(cuerpo.get("precioReferencia").isNull()).isTrue();
    assertThat(cuerpo.get("nombreCategoria").asText()).isEqualTo("Hogar y mantenimiento");
    assertThat(cuerpo.get("nombreSubcategoria").asText()).isEqualTo("Plomería");
    assertThat(estadoEnBase(cuerpo.get("idServicioPublicado").asLong())).isEqualTo("INACTIVO");
  }

  @Test
  void listaSoloLosServiciosPropios() {
    crearServicio("Propio");

    NavegadorDePrueba otra = abrirNavegador();
    registrar(otra, CORREO_OTRA_PERSONA, CLAVE);
    iniciarSesion(otra, CORREO_OTRA_PERSONA, CLAVE);
    otra.post("/api/prestador/perfil", solicitudDePerfil());
    otra.post(RUTA_SERVICIOS_PROPIOS, solicitudDeServicio("Ajeno", idSubcategoria("Electricidad")));

    JsonNode propios = json(navegador.get(RUTA_SERVICIOS_PROPIOS));
    assertThat(propios).hasSize(1);
    assertThat(propios.get(0).get("nombre").asText()).isEqualTo("Propio");
  }

  @Test
  void editaNombreDescripcionSubcategoriaYPrecio() {
    long id = idDe(crearServicio("Original"));

    HttpResponse<String> respuesta =
        navegador.put(
            RUTA_SERVICIOS_PROPIOS + "/" + id,
            solicitudDeServicioConPrecio("Corregido", idSubcategoria("Electricidad"), "450.00"));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    JsonNode cuerpo = json(respuesta);
    assertThat(cuerpo.get("nombre").asText()).isEqualTo("Corregido");
    assertThat(cuerpo.get("nombreSubcategoria").asText()).isEqualTo("Electricidad");
    assertThat(cuerpo.get("precioReferencia").decimalValue()).isEqualByComparingTo("450.00");
    assertThat(cuerpo.get("estado").asText()).isEqualTo("INACTIVO");
  }

  @Test
  void rechazaUnaSubcategoriaInexistente() {
    HttpResponse<String> respuesta =
        navegador.post(RUTA_SERVICIOS_PROPIOS, solicitudDeServicio("X", 999999));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SUBCATEGORIA_NO_DISPONIBLE");
  }

  @Test
  void rechazaUnPrecioCeroONegativo() {
    HttpResponse<String> cero =
        navegador.post(
            RUTA_SERVICIOS_PROPIOS,
            solicitudDeServicioConPrecio("Cero", idSubcategoria("Plomería"), "0"));
    HttpResponse<String> negativo =
        navegador.post(
            RUTA_SERVICIOS_PROPIOS,
            solicitudDeServicioConPrecio("Negativo", idSubcategoria("Plomería"), "-1"));

    assertThat(cero.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(cero)).isEqualTo("VALIDACION");
    assertThat(negativo.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(negativo)).isEqualTo("VALIDACION");
  }

  @Test
  void noActivaSinVerificacionBasica() {
    long id = idDe(crearServicio("Sin verificar"));

    HttpResponse<String> respuesta = activar(id);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("VERIFICACION_BASICA_REQUERIDA");
    assertThat(estadoEnBase(id)).isEqualTo("INACTIVO");
  }

  @Test
  void activaConVerificacionBasicaYPrestadorDisponible() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    aprobarBasica(admin);
    long id = idDe(crearServicio("Instalación sanitaria"));

    HttpResponse<String> respuesta = activar(id);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("estado").asText()).isEqualTo("ACTIVO");
    assertThat(estadoEnBase(id)).isEqualTo("ACTIVO");
  }

  @Test
  void noActivaSiElPrestadorNoEstaDisponible() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    aprobarBasica(admin);
    dejarDisponible("NO_DISPONIBLE");
    long id = idDe(crearServicio("No disponible"));

    HttpResponse<String> respuesta = activar(id);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("PRESTADOR_NO_DISPONIBLE");
    assertThat(estadoEnBase(id)).isEqualTo("INACTIVO");
  }

  @Test
  void noActivaConCuentaRestringida() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    aprobarBasica(admin);
    long id = idDe(crearServicio("Restringido"));
    restringirCuenta(CORREO);

    HttpResponse<String> respuesta = activar(id);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CUENTA_RESTRINGIDA");
    assertThat(estadoEnBase(id)).isEqualTo("INACTIVO");
  }

  @Test
  void desactivaUnServicioActivo() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    aprobarBasica(admin);
    long id = idDe(crearServicio("Para desactivar"));
    assertThat(activar(id).statusCode()).isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> respuesta = desactivar(id);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("estado").asText()).isEqualTo("INACTIVO");
  }

  @Test
  void unRecursoAjenoResponde404YNo403() {
    long id = idDe(crearServicio("Ajeno"));

    NavegadorDePrueba otra = abrirNavegador();
    registrar(otra, CORREO_OTRA_PERSONA, CLAVE);
    iniciarSesion(otra, CORREO_OTRA_PERSONA, CLAVE);
    otra.post("/api/prestador/perfil", solicitudDePerfil());

    HttpResponse<String> consulta = otra.get(RUTA_SERVICIOS_PROPIOS + "/" + id);
    HttpResponse<String> edicion =
        otra.put(
            RUTA_SERVICIOS_PROPIOS + "/" + id,
            solicitudDeServicio("Hack", idSubcategoria("Plomería")));
    HttpResponse<String> estado =
        otra.put(RUTA_SERVICIOS_PROPIOS + "/" + id + "/estado", Map.of("estado", "ACTIVO"));

    assertThat(consulta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(consulta)).isEqualTo("RECURSO_NO_ENCONTRADO");
    assertThat(edicion.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(estado.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(estadoEnBase(id)).isEqualTo("INACTIVO");
  }

  @Test
  void sinPerfilNoSePuedeCrearUnServicio() {
    NavegadorDePrueba sinPerfil = abrirNavegador();
    registrar(sinPerfil, "sin.perfil@moica.test", CLAVE);
    iniciarSesion(sinPerfil, "sin.perfil@moica.test", CLAVE);

    HttpResponse<String> respuesta =
        sinPerfil.post(
            RUTA_SERVICIOS_PROPIOS, solicitudDeServicio("X", idSubcategoria("Plomería")));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("PERFIL_NO_ENCONTRADO");
  }

  @Test
  void unaCuentaRestringidaConservaLaLecturaYNoPuedeCrear() {
    crearServicio("Ya existía");
    restringirCuenta(CORREO);

    HttpResponse<String> lectura = navegador.get(RUTA_SERVICIOS_PROPIOS);
    HttpResponse<String> creacion =
        navegador.post(
            RUTA_SERVICIOS_PROPIOS, solicitudDeServicio("Nuevo", idSubcategoria("Plomería")));

    assertThat(lectura.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(lectura)).hasSize(1);
    assertThat(creacion.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(creacion)).isEqualTo("CUENTA_RESTRINGIDA");
  }
}
