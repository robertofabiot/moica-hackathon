package com.moica.calificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Quién puede calificar, cuándo y una sola vez.
 *
 * <p>Las reglas de participación y de estado se comprueban aquí, sobre la API, porque dependen de
 * la solicitud y no de la tabla. Lo que la base sostiene por su cuenta —rango, participantes
 * distintos y unicidad— se prueba en {@code EsquemaDeCalificacionesIT}.
 */
class CalificacionDeSolicitudIT extends EscenarioDeCalificacion {

  @Test
  void elClienteCalificaAlPrestadorYQuedaRegistradoComoPrestador() {
    long idSolicitud = solicitudCompletada();

    HttpResponse<String> respuesta = calificar(cliente, idSolicitud, 5, "Trabajo impecable.");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(respuesta).get("rolCalificado").asText()).isEqualTo("PRESTADOR");
    assertThat(json(respuesta).get("idCalificado").asLong()).isEqualTo(idDe(CORREO));
    assertThat(json(respuesta).get("idCalificador").asLong()).isEqualTo(idDe(CORREO_CLIENTE));
    assertThat(json(respuesta).get("puntuacion").asInt()).isEqualTo(5);
    assertThat(json(respuesta).get("comentario").asText()).isEqualTo("Trabajo impecable.");
    assertThat(rolEnBase(idSolicitud, idDe(CORREO_CLIENTE))).isEqualTo("PRESTADOR");
  }

  @Test
  void elPrestadorCalificaAlClienteYQuedaRegistradoComoCliente() {
    long idSolicitud = solicitudCompletada();

    HttpResponse<String> respuesta = calificar(navegador, idSolicitud, 4, "Todo claro.");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(respuesta).get("rolCalificado").asText()).isEqualTo("CLIENTE");
    assertThat(json(respuesta).get("idCalificado").asLong()).isEqualTo(idDe(CORREO_CLIENTE));
    assertThat(rolEnBase(idSolicitud, idDe(CORREO))).isEqualTo("CLIENTE");
  }

  @Test
  void admiteLaPuntuacionMinimaYLaMaxima() {
    long primera = solicitudCompletada();
    long segunda = solicitudCompletada();

    assertThat(calificar(cliente, primera, 1).statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(calificar(cliente, segunda, 5).statusCode()).isEqualTo(HttpStatus.CREATED.value());
  }

  @Test
  void rechazaUnaPuntuacionFueraDelRango() {
    long idSolicitud = solicitudCompletada();

    assertThat(calificar(cliente, idSolicitud, 0).statusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(calificar(cliente, idSolicitud, 6).statusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(calificar(cliente, idSolicitud, -3))).isEqualTo("VALIDACION");
    assertThat(calificacionesEnBase(idSolicitud)).isZero();
  }

  @Test
  void rechazaUnaCalificacionSinPuntuacion() {
    long idSolicitud = solicitudCompletada();

    HttpResponse<String> respuesta =
        calificarCon(cliente, idSolicitud, Map.of("comentario", "Sin estrellas."));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("VALIDACION");
    assertThat(calificacionesEnBase(idSolicitud)).isZero();
  }

  @Test
  void elComentarioEsOpcionalYUnoDeEspaciosSeGuardaComoNulo() {
    long sinComentario = solicitudCompletada();
    long conEspacios = solicitudCompletada();

    assertThat(calificar(cliente, sinComentario, 5).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    assertThat(comentarioEnBase(sinComentario, idDe(CORREO_CLIENTE))).isNull();

    HttpResponse<String> respuesta = calificar(cliente, conEspacios, 5, "   \t  ");

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(respuesta).get("comentario").isNull()).isTrue();
    assertThat(comentarioEnBase(conEspacios, idDe(CORREO_CLIENTE))).isNull();
  }

  @Test
  void noSePuedeCalificarAntesDeCompletar() {
    long pendiente = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    long aceptada = solicitudAceptada();

    assertThat(codigoDeError(calificar(cliente, pendiente, 5)))
        .isEqualTo("SOLICITUD_NO_COMPLETADA");
    assertThat(calificar(cliente, pendiente, 5).statusCode())
        .isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(calificar(cliente, aceptada, 5).statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(calificacionesEnBase(pendiente)).isZero();
    assertThat(calificacionesEnBase(aceptada)).isZero();
  }

  @Test
  void unTerceroRecibe404YNoPuedeConfirmarQueLaSolicitudExista() {
    long idSolicitud = solicitudCompletada();
    NavegadorDePrueba tercero = cuentaAutenticada(CORREO_TERCERO);

    assertThat(leerCalificacion(tercero, idSolicitud).statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
    HttpResponse<String> intento = calificar(tercero, idSolicitud, 1);
    assertThat(intento.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(intento)).isEqualTo("RECURSO_NO_ENCONTRADO");
    assertThat(calificacionesEnBase(idSolicitud)).isZero();
  }

  @Test
  void unaCuentaRestringidaLeeSuEstadoPeroNoPuedeCalificar() {
    long idSolicitud = solicitudCompletada();
    restringirCuenta(CORREO_CLIENTE);

    HttpResponse<String> estado = leerCalificacion(cliente, idSolicitud);
    assertThat(estado.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(estado).get("puedeCalificar").asBoolean()).isFalse();

    HttpResponse<String> intento = calificar(cliente, idSolicitud, 5);
    assertThat(intento.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(intento)).isEqualTo("CUENTA_RESTRINGIDA");
    assertThat(calificacionesEnBase(idSolicitud)).isZero();
  }

  @Test
  void unaCuentaSuspendidaNoLlegaSiquieraAlRecurso() {
    long idSolicitud = solicitudCompletada();
    suspenderCuenta(CORREO_CLIENTE);

    HttpResponse<String> intento = calificar(cliente, idSolicitud, 5);

    assertThat(intento.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(intento)).isEqualTo("ACCESO_DENEGADO");
    assertThat(leerCalificacion(cliente, idSolicitud).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(calificacionesEnBase(idSolicitud)).isZero();
  }

  @Test
  void nadieCalificaDosVecesLaMismaSolicitud() {
    long idSolicitud = solicitudCompletada();
    assertThat(calificar(cliente, idSolicitud, 5).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    HttpResponse<String> segunda = calificar(cliente, idSolicitud, 1, "Me arrepentí.");

    assertThat(segunda.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(segunda)).isEqualTo("CALIFICACION_DUPLICADA");
    assertThat(calificacionesEnBase(idSolicitud)).isEqualTo(1);
  }

  @Test
  void unaSolicitudAdmiteComoMuchoDosCalificaciones() {
    long idSolicitud = solicitudCompletada();

    assertThat(calificar(cliente, idSolicitud, 5).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    assertThat(calificar(navegador, idSolicitud, 4).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    assertThat(calificar(cliente, idSolicitud, 3).statusCode())
        .isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(calificar(navegador, idSolicitud, 3).statusCode())
        .isEqualTo(HttpStatus.CONFLICT.value());

    assertThat(calificacionesEnBase(idSolicitud)).isEqualTo(2);
  }

  @Test
  void elCalificadoYElRolSalenDeLaSolicitudYNoDelCuerpo() {
    long idSolicitud = solicitudCompletada();
    Map<String, Object> cuerpoManipulado = new HashMap<>();
    cuerpoManipulado.put("puntuacion", 5);
    cuerpoManipulado.put("idCalificado", idDe(CORREO_CLIENTE));
    cuerpoManipulado.put("rolCalificado", "CLIENTE");
    cuerpoManipulado.put("idCalificador", idDe(CORREO));

    HttpResponse<String> respuesta = calificarCon(cliente, idSolicitud, cuerpoManipulado);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(respuesta).get("rolCalificado").asText()).isEqualTo("PRESTADOR");
    assertThat(json(respuesta).get("idCalificado").asLong()).isEqualTo(idDe(CORREO));
    assertThat(json(respuesta).get("idCalificador").asLong()).isEqualTo(idDe(CORREO_CLIENTE));
  }

  @Test
  void elEstadoDescribeAQuienSeCalificaYEnQueRol() {
    long idSolicitud = solicitudCompletada();

    HttpResponse<String> desdeElCliente = leerCalificacion(cliente, idSolicitud);
    assertThat(desdeElCliente.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(desdeElCliente).get("rolCalificado").asText()).isEqualTo("PRESTADOR");
    assertThat(json(desdeElCliente).get("nombreCalificado").asText())
        .isEqualTo("Taller La Esperanza");
    assertThat(json(desdeElCliente).get("puedeCalificar").asBoolean()).isTrue();
    assertThat(json(desdeElCliente).get("calificacionEmitida").isNull()).isTrue();

    HttpResponse<String> desdeElPrestador = leerCalificacion(navegador, idSolicitud);
    assertThat(json(desdeElPrestador).get("rolCalificado").asText()).isEqualTo("CLIENTE");
    assertThat(json(desdeElPrestador).get("nombreCalificado").asText())
        .isEqualTo("Persona de Prueba");
  }

  @Test
  void elEstadoAvisaQueTodaviaNoSePuedeCalificarAntesDeCompletar() {
    long idSolicitud = solicitudAceptada();

    HttpResponse<String> estado = leerCalificacion(cliente, idSolicitud);

    assertThat(estado.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(estado).get("solicitudCompletada").asBoolean()).isFalse();
    assertThat(json(estado).get("puedeCalificar").asBoolean()).isFalse();
    assertThat(json(estado).get("calificacionEmitida").isNull()).isTrue();
  }

  @Test
  void despuesDeCalificarElEstadoDevuelveLoEscritoYCierraLaAccion() {
    long idSolicitud = solicitudCompletada();
    assertThat(calificar(cliente, idSolicitud, 4, "Puntual y ordenado.").statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    HttpResponse<String> estado = leerCalificacion(cliente, idSolicitud);

    assertThat(json(estado).get("puedeCalificar").asBoolean()).isFalse();
    assertThat(json(estado).get("calificacionEmitida").get("puntuacion").asInt()).isEqualTo(4);
    assertThat(json(estado).get("calificacionEmitida").get("comentario").asText())
        .isEqualTo("Puntual y ordenado.");
    assertThat(json(estado).get("calificacionEmitida").get("fechaCreacion").isNull()).isFalse();
  }

  @Test
  void sinSesionNoSeConsultaNiSeCalifica() {
    long idSolicitud = solicitudCompletada();
    NavegadorDePrueba visitante = abrirNavegador();

    assertThat(leerCalificacion(visitante, idSolicitud).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(calificar(visitante, idSolicitud, 5).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(calificacionesEnBase(idSolicitud)).isZero();
  }

  @Test
  void noExistenEdicionNiBorradoDeUnaCalificacion() {
    long idSolicitud = solicitudCompletada();
    assertThat(calificar(cliente, idSolicitud, 5).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    assertThat(cliente.put(rutaDeCalificacion(idSolicitud), Map.of("puntuacion", 1)).statusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    assertThat(cliente.delete(rutaDeCalificacion(idSolicitud)).statusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    assertThat(calificacionesEnBase(idSolicitud)).isEqualTo(1);
  }
}
