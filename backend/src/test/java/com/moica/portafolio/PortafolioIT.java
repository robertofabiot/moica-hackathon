package com.moica.portafolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.prestador.EscenarioDePrestador;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * El portafolio propio: trabajos, su orden y sus imágenes con el ciclo de vida de los objetos.
 *
 * <p>La propiedad se prueba con dos cuentas reales, y la limpieza del almacén se afirma contra el
 * doble: eliminar una imagen o un trabajo retira sus objetos, y una fila nunca queda apuntando a un
 * objeto que no existe.
 */
class PortafolioIT extends EscenarioDePrestador {

  @BeforeEach
  void prepararPerfil() {
    crearPerfil();
  }

  @Test
  void agregaTrabajosAlFinalYLosListaEnOrdenConSusCampos() {
    crearTrabajo("Instalación eléctrica", "2024-05-20");
    crearTrabajo("Mantenimiento de aires", null);

    JsonNode trabajos = json(navegador.get(RUTA_TRABAJOS));

    assertThat(trabajos).hasSize(2);
    assertThat(trabajos.get(0).get("titulo").asText()).isEqualTo("Instalación eléctrica");
    assertThat(trabajos.get(0).get("fechaRealizacion").asText()).isEqualTo("2024-05-20");
    assertThat(trabajos.get(0).get("ordenVisualizacion").asInt()).isZero();
    assertThat(trabajos.get(1).get("fechaRealizacion").isNull())
        .as("la fecha solo aparece si el prestador la indica")
        .isTrue();
    assertThat(trabajos.get(1).get("imagenes")).isEmpty();
  }

  @Test
  void editaUnTrabajoPropioYMueveSuFechaDeActualizacion() {
    Long id = crearTrabajo("Título original", null);

    Map<String, Object> cambios = new HashMap<>();
    cambios.put("titulo", "Título corregido");
    cambios.put("descripcion", "Descripción corregida del trabajo.");
    cambios.put("fechaRealizacion", "2023-11-02");

    HttpResponse<String> respuesta = navegador.put(RUTA_TRABAJOS + "/" + id, cambios);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("titulo").asText()).isEqualTo("Título corregido");
    assertThat(
            jdbc.queryForObject(
                "SELECT fecha_actualizacion > fecha_creacion FROM trabajo_portafolio",
                Boolean.class))
        .isTrue();
  }

  @Test
  void reordenaLosTrabajosConLaListaCompleta() {
    Long primero = crearTrabajo("Primero", null);
    Long segundo = crearTrabajo("Segundo", null);

    HttpResponse<String> respuesta =
        navegador.put(RUTA_TRABAJOS + "/orden", Map.of("idsEnOrden", List.of(segundo, primero)));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());

    JsonNode trabajos = json(navegador.get(RUTA_TRABAJOS));
    assertThat(trabajos.get(0).get("titulo").asText()).isEqualTo("Segundo");
    assertThat(trabajos.get(1).get("titulo").asText()).isEqualTo("Primero");
  }

  @Test
  void rechazaUnOrdenQueNoTraigaExactamenteLosTrabajosExistentes() {
    Long unico = crearTrabajo("Único", null);

    HttpResponse<String> conIntruso =
        navegador.put(RUTA_TRABAJOS + "/orden", Map.of("idsEnOrden", List.of(unico, unico + 1000)));

    assertThat(conIntruso.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(conIntruso)).isEqualTo("ORDEN_INVALIDO");
  }

  @Test
  void subeImagenesConTextoAlternativoYLasListaEnOrden() {
    Long idTrabajo = crearTrabajo("Con imágenes", null);

    HttpResponse<String> primera =
        navegador.postArchivo(
            rutaImagenes(idTrabajo),
            "obra terminada.png",
            "image/png",
            imagenPng(),
            Map.of("textoAlternativo", "Fachada pintada de blanco"));
    assertThat(primera.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(primera).get("textoAlternativo").asText())
        .isEqualTo("Fachada pintada de blanco");
    assertThat(json(primera).get("urlImagen").asText())
        .startsWith("https://imagenes.moica.test/trabajos/")
        .doesNotContain("obra");

    HttpResponse<String> segunda =
        navegador.postArchivo(
            rutaImagenes(idTrabajo), "detalle.jpg", "image/jpeg", imagenJpeg(), Map.of());
    assertThat(json(segunda).get("textoAlternativo").isNull()).isTrue();
    assertThat(json(segunda).get("ordenVisualizacion").asInt()).isEqualTo(1);

    JsonNode imagenes = json(navegador.get(RUTA_TRABAJOS)).get(0).get("imagenes");
    assertThat(imagenes).hasSize(2);
    assertThat(almacenamiento.cantidadDeObjetos()).isEqualTo(2);
  }

  @Test
  void editaElTextoAlternativoDeUnaImagen() {
    Long idTrabajo = crearTrabajo("Con imagen", null);
    Long idImagen = subirImagen(idTrabajo);

    HttpResponse<String> respuesta =
        navegador.put(
            rutaImagenes(idTrabajo) + "/" + idImagen,
            Map.of("textoAlternativo", "Vista del comedor terminado"));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("textoAlternativo").asText())
        .isEqualTo("Vista del comedor terminado");
  }

  @Test
  void reordenaLasImagenesDeUnTrabajo() {
    Long idTrabajo = crearTrabajo("Con imágenes", null);
    Long primera = subirImagen(idTrabajo);
    Long segunda = subirImagen(idTrabajo);

    HttpResponse<String> respuesta =
        navegador.put(
            rutaImagenes(idTrabajo) + "/orden", Map.of("idsEnOrden", List.of(segunda, primera)));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    JsonNode imagenes = json(navegador.get(RUTA_TRABAJOS)).get(0).get("imagenes");
    assertThat(imagenes.get(0).get("idImagenTrabajoPortafolio").asLong()).isEqualTo(segunda);
  }

  @Test
  void eliminarUnaImagenRetiraSuFilaYSuObjeto() {
    Long idTrabajo = crearTrabajo("Con imagen", null);
    Long idImagen = subirImagen(idTrabajo);

    HttpResponse<String> respuesta = navegador.delete(rutaImagenes(idTrabajo) + "/" + idImagen);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM imagen_trabajo_portafolio", Integer.class))
        .isZero();
    assertThat(almacenamiento.cantidadDeObjetos()).isZero();
  }

  @Test
  void eliminarUnTrabajoRetiraSusFilasYSusObjetos() {
    Long idTrabajo = crearTrabajo("Se va completo", null);
    subirImagen(idTrabajo);
    subirImagen(idTrabajo);

    HttpResponse<String> respuesta = navegador.delete(RUTA_TRABAJOS + "/" + idTrabajo);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM trabajo_portafolio", Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM imagen_trabajo_portafolio", Integer.class))
        .isZero();
    assertThat(almacenamiento.cantidadDeObjetos()).isZero();
  }

  @Test
  void siRegistrarLaImagenFallaElObjetoSeCompensa() {
    Long idTrabajo = crearTrabajo("Compensación", null);
    almacenamiento.simularUrlInutilizable();

    HttpResponse<String> respuesta =
        navegador.postArchivo(
            rutaImagenes(idTrabajo), "foto.png", "image/png", imagenPng(), Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM imagen_trabajo_portafolio", Integer.class))
        .isZero();
    assertThat(almacenamiento.cantidadDeObjetos())
        .as("la compensación retiró el objeto que acababa de subirse")
        .isZero();
  }

  @Test
  void unTrabajoAjenoNoSeVeNiSePuedeTocar() {
    Long idAjeno = crearTrabajo("De la primera cuenta", null);

    NavegadorDePrueba otraPersona = abrirNavegador();
    registrar(otraPersona, "otra@moica.test", CLAVE);
    iniciarSesion(otraPersona, "otra@moica.test", CLAVE);
    otraPersona.post(RUTA_PERFIL, solicitudDePerfil());

    assertThat(json(otraPersona.get(RUTA_TRABAJOS))).isEmpty();

    HttpResponse<String> edicion =
        otraPersona.put(
            RUTA_TRABAJOS + "/" + idAjeno,
            Map.of("titulo", "Intruso", "descripcion", "No debería poder."));
    assertThat(edicion.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

    HttpResponse<String> subida =
        otraPersona.postArchivo(
            rutaImagenes(idAjeno), "foto.png", "image/png", imagenPng(), Map.of());
    assertThat(subida.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(almacenamiento.cantidadDeObjetos())
        .as("la propiedad se comprueba antes de subir nada")
        .isZero();

    assertThat(otraPersona.delete(RUTA_TRABAJOS + "/" + idAjeno).statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void unaCuentaRestringidaConservaLaLecturaPeroNoLasMutaciones() {
    crearTrabajo("Existente", null);
    restringirCuenta(CORREO);

    assertThat(navegador.get(RUTA_TRABAJOS).statusCode()).isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> creacion =
        navegador.post(
            RUTA_TRABAJOS, Map.of("titulo", "Nuevo", "descripcion", "No debería entrar."));
    assertThat(creacion.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(creacion)).isEqualTo("CUENTA_RESTRINGIDA");
  }

  @Test
  void laValidacionDelTrabajoDetallaLosCamposRechazados() {
    HttpResponse<String> respuesta =
        navegador.post(RUTA_TRABAJOS, Map.of("titulo", "  ", "descripcion", ""));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("VALIDACION");
    assertThat(json(respuesta).get("errores")).isNotEmpty();
  }

  private Long crearTrabajo(String titulo, String fechaRealizacion) {
    Map<String, Object> solicitud = new HashMap<>();
    solicitud.put("titulo", titulo);
    solicitud.put("descripcion", "Descripción de «" + titulo + "» para la prueba.");
    if (fechaRealizacion != null) {
      solicitud.put("fechaRealizacion", fechaRealizacion);
    }

    HttpResponse<String> respuesta = navegador.post(RUTA_TRABAJOS, solicitud);
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return json(respuesta).get("idTrabajo").asLong();
  }

  private Long subirImagen(Long idTrabajo) {
    HttpResponse<String> respuesta =
        navegador.postArchivo(
            rutaImagenes(idTrabajo), "imagen.png", "image/png", imagenPng(), Map.of());
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return json(respuesta).get("idImagenTrabajoPortafolio").asLong();
  }

  private static String rutaImagenes(Long idTrabajo) {
    return RUTA_TRABAJOS + "/" + idTrabajo + "/imagenes";
  }
}
