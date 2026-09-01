package com.moica.calificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * La reputación que se calcula desde las calificaciones, separada por rol.
 *
 * <p>Comprueba el promedio, la cantidad y el desglose; que la reputación como cliente y como
 * prestador no se mezclen; que quien no tiene calificaciones no reciba un cero; que la reputación
 * pública viaje en las tres superficies de descubrimiento sin comentarios ni identidades, y que la
 * reputación como cliente solo se vea desde una solicitud propia del prestador.
 */
class ReputacionPorRolIT extends EscenarioDeCalificacion {

  private static final String CORREO_OTRO_PRESTADOR = "otro.prestador@moica.test";

  @Test
  void elPromedioLaCantidadYElDesgloseSalenDeLasCalificaciones() {
    calificarAlPrestadorCon(5, 4, 4);

    JsonNode reputacion = detallePublico(idServicio).get("reputacionPrestador");

    assertThat(reputacion.get("rol").asText()).isEqualTo("PRESTADOR");
    assertThat(reputacion.get("cantidad").asLong()).isEqualTo(3);
    // 13 / 3 = 4.33..., redondeado a un decimal en el servidor.
    assertThat(reputacion.get("promedio").asText()).isEqualTo("4.3");
    assertThat(cantidadPorEstrellas(reputacion))
        .containsExactly(
            Map.entry(5, 1L),
            Map.entry(4, 2L),
            Map.entry(3, 0L),
            Map.entry(2, 0L),
            Map.entry(1, 0L));
  }

  @Test
  void quienNoTieneCalificacionesNoRecibeUnCeroSinoUnPromedioNulo() {
    JsonNode reputacion = detallePublico(idServicio).get("reputacionPrestador");

    assertThat(reputacion.get("cantidad").asLong()).isZero();
    assertThat(reputacion.get("promedio").isNull()).isTrue();
    assertThat(reputacion.get("desglose")).hasSize(5);
    assertThat(cantidadPorEstrellas(reputacion).values()).containsOnly(0L);
  }

  @Test
  void laReputacionComoClienteYComoPrestadorNoSeMezclan() {
    long idSolicitud = solicitudCompletada();
    assertThat(calificar(cliente, idSolicitud, 5).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    assertThat(calificar(navegador, idSolicitud, 2).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    JsonNode comoPrestador = detallePublico(idServicio).get("reputacionPrestador");
    JsonNode comoCliente = json(leerReputacionDelCliente(navegador, idSolicitud));

    assertThat(comoPrestador.get("rol").asText()).isEqualTo("PRESTADOR");
    assertThat(comoPrestador.get("promedio").asText()).isEqualTo("5.0");
    assertThat(comoPrestador.get("cantidad").asLong()).isEqualTo(1);

    assertThat(comoCliente.get("rol").asText()).isEqualTo("CLIENTE");
    assertThat(comoCliente.get("promedio").asText()).isEqualTo("2.0");
    assertThat(comoCliente.get("cantidad").asLong()).isEqualTo(1);
  }

  @Test
  void laReputacionDelClienteSoloLaVeElPrestadorParticipante() {
    long idSolicitud = solicitudCompletada();
    NavegadorDePrueba tercero = cuentaAutenticada(CORREO_TERCERO);

    assertThat(leerReputacionDelCliente(navegador, idSolicitud).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> desdeElCliente = leerReputacionDelCliente(cliente, idSolicitud);
    assertThat(desdeElCliente.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(desdeElCliente)).isEqualTo("RECURSO_NO_ENCONTRADO");

    assertThat(leerReputacionDelCliente(tercero, idSolicitud).statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(leerReputacionDelCliente(abrirNavegador(), idSolicitud).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void noExisteUnaRutaPublicaParaLaReputacionComoCliente() {
    long idSolicitud = solicitudCompletada();
    assertThat(calificar(navegador, idSolicitud, 5).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    // El cliente no tiene perfil público: pedirlo por su identificador responde
    // 404, igual que antes de P8. La reputación como cliente no lo convierte en
    // una superficie pública.
    HttpResponse<String> perfilDelCliente =
        abrirNavegador().get(RUTA_PRESTADORES_PUBLICOS + "/" + idDe(CORREO_CLIENTE));

    assertThat(perfilDelCliente.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void lasTresSuperficiesPublicasLlevanLaMismaReputacionDelPrestador() {
    calificarAlPrestadorCon(5, 4, 4);

    JsonNode detalle = detallePublico(idServicio).get("reputacionPrestador");
    JsonNode enElListado = listadoPublico().get(0).get("reputacionPrestador");
    JsonNode perfil = perfilPublico(idDe(CORREO));

    assertThat(enElListado.get("promedio").asText()).isEqualTo("4.3");
    assertThat(detalle.get("promedio").asText()).isEqualTo("4.3");
    assertThat(perfil.get("reputacionPrestador").get("promedio").asText()).isEqualTo("4.3");
    assertThat(perfil.get("reputacionPrestador").get("cantidad").asLong()).isEqualTo(3);
  }

  @Test
  void dosServiciosDelMismoPrestadorMuestranElMismoAgregado() {
    calificarAlPrestadorCon(5, 4, 4);
    long segundoServicio = idDe(crearServicio("Instalación de grifería"));
    assertThat(activar(segundoServicio).statusCode()).isEqualTo(HttpStatus.OK.value());

    JsonNode listado = listadoPublico();

    assertThat(listado).hasSize(2);
    assertThat(listado.get(0).get("reputacionPrestador").get("promedio").asText())
        .isEqualTo(listado.get(1).get("reputacionPrestador").get("promedio").asText());
    assertThat(listado.get(1).get("reputacionPrestador").get("cantidad").asLong()).isEqualTo(3);
  }

  @Test
  void unPrestadorSinCalificacionesConviveConOtroQueSiLasTiene() {
    calificarAlPrestadorCon(5, 4, 4);
    publicarServicioDeOtroPrestador();

    JsonNode listado = listadoPublico();

    assertThat(listado).hasSize(2);
    JsonNode conNota = reputacionDe(listado, idDe(CORREO));
    JsonNode sinNota = reputacionDe(listado, idDe(CORREO_OTRO_PRESTADOR));

    assertThat(conNota.get("promedio").asText()).isEqualTo("4.3");
    assertThat(conNota.get("cantidad").asLong()).isEqualTo(3);
    assertThat(sinNota.get("promedio").isNull()).isTrue();
    assertThat(sinNota.get("cantidad").asLong()).isZero();
  }

  @Test
  void laReputacionPublicaNoLlevaComentariosNiIdentidades() {
    long idSolicitud = solicitudCompletada();
    assertThat(calificar(cliente, idSolicitud, 5, "Excelente, muy recomendable.").statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    String detalle = abrirNavegador().get(RUTA_SERVICIOS_PUBLICOS + "/" + idServicio).body();
    String listado = abrirNavegador().get(RUTA_SERVICIOS_PUBLICOS).body();
    String perfil = abrirNavegador().get(RUTA_PRESTADORES_PUBLICOS + "/" + idDe(CORREO)).body();

    for (String cuerpo : new String[] {detalle, listado, perfil}) {
      assertThat(cuerpo)
          .doesNotContain("Excelente, muy recomendable.")
          .doesNotContain("comentario")
          .doesNotContain("idCalificador")
          .doesNotContain("idCalificado")
          .doesNotContain("correoElectronico")
          .doesNotContain(CORREO_CLIENTE);
    }
  }

  /** Califica al prestador del escenario con una puntuación por cada solicitud completada. */
  private void calificarAlPrestadorCon(int... puntuaciones) {
    for (int puntuacion : puntuaciones) {
      long idSolicitud = solicitudCompletada();
      assertThat(calificar(cliente, idSolicitud, puntuacion).statusCode())
          .isEqualTo(HttpStatus.CREATED.value());
    }
  }

  private JsonNode listadoPublico() {
    HttpResponse<String> respuesta = abrirNavegador().get(RUTA_SERVICIOS_PUBLICOS);
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    return json(respuesta);
  }

  private JsonNode perfilPublico(Long idPrestador) {
    HttpResponse<String> respuesta =
        abrirNavegador().get(RUTA_PRESTADORES_PUBLICOS + "/" + idPrestador);
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    return json(respuesta);
  }

  private static JsonNode reputacionDe(JsonNode listado, Long idPrestador) {
    for (JsonNode tarjeta : listado) {
      if (tarjeta.get("prestador").get("idPrestador").asLong() == idPrestador) {
        return tarjeta.get("reputacionPrestador");
      }
    }
    throw new AssertionError("El listado no trae ninguna tarjeta del prestador " + idPrestador);
  }

  private static Map<Integer, Long> cantidadPorEstrellas(JsonNode reputacion) {
    Map<Integer, Long> porEstrellas = new java.util.LinkedHashMap<>();
    for (JsonNode tramo : reputacion.get("desglose")) {
      porEstrellas.put(tramo.get("estrellas").asInt(), tramo.get("cantidad").asLong());
    }
    return porEstrellas;
  }

  /**
   * Un segundo prestador visible en el descubrimiento y todavía sin calificaciones.
   *
   * <p>El nivel de verificación se proyecta con SQL en lugar de recorrer el expediente completo:
   * aquí lo que se prueba es la reputación, no la verificación, que tiene sus propias pruebas.
   */
  private void publicarServicioDeOtroPrestador() {
    NavegadorDePrueba otro = cuentaAutenticada(CORREO_OTRO_PRESTADOR);
    Map<String, Object> perfil = new HashMap<>(solicitudDePerfil());
    perfil.put("nombrePublico", "Electricidad del Norte");
    assertThat(otro.post(RUTA_PERFIL, perfil).statusCode()).isEqualTo(HttpStatus.CREATED.value());

    jdbc.update(
        "UPDATE perfil_prestador SET nivel_verificacion = 'VERIFICADO_BASICO'"
            + " WHERE id_prestador = ?",
        idDe(CORREO_OTRO_PRESTADOR));

    long idOtroServicio = idDe(crearServicio(otro, "Cambio de tomacorrientes", idSubcategoria()));
    assertThat(
            otro.put(
                    RUTA_SERVICIOS_PROPIOS + "/" + idOtroServicio + "/estado",
                    Map.of("estado", "ACTIVO"))
                .statusCode())
        .isEqualTo(HttpStatus.OK.value());
  }

  private Integer idSubcategoria() {
    return idSubcategoria("Electricidad");
  }
}
