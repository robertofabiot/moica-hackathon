package com.moica.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/** Imágenes de un servicio propio: validación, compensación y limpieza de mejor esfuerzo. */
class ImagenDeServicioIT extends EscenarioDeServicio {

  @Test
  void subeImagenesConTextoAlternativoYPrefijoDeServicios() {
    long idServicio = idDe(crearServicio("Con imágenes"));

    HttpResponse<String> primera =
        navegador.postArchivo(
            rutaImagenes(idServicio),
            "trabajo terminado.png",
            "image/png",
            imagenPng(),
            Map.of("textoAlternativo", "Tubería ya instalada"));

    assertThat(primera.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(primera).get("textoAlternativo").asText()).isEqualTo("Tubería ya instalada");
    assertThat(json(primera).get("urlImagen").asText())
        .startsWith("https://imagenes.moica.test/servicios/")
        .doesNotContain("trabajo");

    HttpResponse<String> segunda =
        navegador.postArchivo(
            rutaImagenes(idServicio), "detalle.jpg", "image/jpeg", imagenJpeg(), Map.of());
    assertThat(json(segunda).get("textoAlternativo").isNull()).isTrue();
    assertThat(json(segunda).get("ordenVisualizacion").asInt()).isEqualTo(1);

    JsonNode imagenes =
        json(navegador.get(RUTA_SERVICIOS_PROPIOS + "/" + idServicio)).get("imagenes");
    assertThat(imagenes).hasSize(2);
    assertThat(almacenamiento.cantidadDeObjetos()).isEqualTo(2);
  }

  @Test
  void rechazaUnFormatoNoAdmitidoAntesDeSubir() {
    long idServicio = idDe(crearServicio("Sin imagen inválida"));

    HttpResponse<String> respuesta =
        navegador.postArchivo(
            rutaImagenes(idServicio),
            "vector.svg",
            "image/svg+xml",
            "<svg></svg>".getBytes(),
            Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("IMAGEN_NO_ADMITIDA");
    assertThat(almacenamiento.cantidadDeObjetos()).isZero();
    assertThat(contarImagenes()).isZero();
  }

  @Test
  void rechazaUnaCabeceraQueNoCoincideConLaFirma() {
    long idServicio = idDe(crearServicio("Firma falsa"));

    HttpResponse<String> respuesta =
        navegador.postArchivo(
            rutaImagenes(idServicio), "falso.png", "image/png", imagenJpeg(), Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("IMAGEN_NO_ADMITIDA");
    assertThat(almacenamiento.cantidadDeObjetos()).isZero();
  }

  @Test
  void reordenaYEditaElTextoAlternativo() {
    long idServicio = idDe(crearServicio("Orden"));
    long primera = idDeImagen(subir(idServicio));
    long segunda = idDeImagen(subir(idServicio));

    HttpResponse<String> orden =
        navegador.put(
            rutaImagenes(idServicio) + "/orden", Map.of("idsEnOrden", List.of(segunda, primera)));
    assertThat(orden.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(orden).get(0).get("idImagenServicioPublicado").asLong()).isEqualTo(segunda);

    HttpResponse<String> texto =
        navegador.put(
            rutaImagenes(idServicio) + "/" + primera,
            Map.of("textoAlternativo", "Detalle del empalme"));
    assertThat(json(texto).get("textoAlternativo").asText()).isEqualTo("Detalle del empalme");
  }

  @Test
  void eliminarUnaImagenRetiraSuFilaYSuObjeto() {
    long idServicio = idDe(crearServicio("Borrar imagen"));
    long idImagen = idDeImagen(subir(idServicio));

    HttpResponse<String> respuesta = navegador.delete(rutaImagenes(idServicio) + "/" + idImagen);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(contarImagenes()).isZero();
    assertThat(almacenamiento.cantidadDeObjetos()).isZero();
  }

  @Test
  void siRegistrarLaImagenFallaElObjetoSeCompensa() {
    long idServicio = idDe(crearServicio("Compensación"));
    almacenamiento.simularUrlInutilizable();

    HttpResponse<String> respuesta =
        navegador.postArchivo(
            rutaImagenes(idServicio), "foto.png", "image/png", imagenPng(), Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(contarImagenes()).isZero();
    assertThat(almacenamiento.cantidadDeObjetos()).isZero();
  }

  @Test
  void conElAlmacenamientoCaidoRespondeElErrorUniforme() {
    long idServicio = idDe(crearServicio("Almacén caído"));
    almacenamiento.simularNoDisponible();

    HttpResponse<String> respuesta =
        navegador.postArchivo(
            rutaImagenes(idServicio), "foto.png", "image/png", imagenPng(), Map.of());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("ALMACENAMIENTO_NO_DISPONIBLE");
    assertThat(respuesta.body())
        .as("el error no revela proveedor, endpoint ni credenciales")
        .doesNotContain("R2")
        .doesNotContain("cloudflare")
        .doesNotContain("S3");
    assertThat(contarImagenes()).isZero();
  }

  @Test
  void unaUrlAjenaNoSeIntentaBorrar() {
    long idServicio = idDe(crearServicio("URL ajena"));
    long idImagen = idDeImagen(subir(idServicio));
    String claveGuardada = almacenamiento.clavesGuardadas().get(0);

    jdbc.update(
        "UPDATE imagen_servicio_publicado SET url_imagen = ? WHERE id_imagen_servicio_publicado = ?",
        "https://dominio-anterior.example/servicios/abc123.png",
        idImagen);

    HttpResponse<String> respuesta = navegador.delete(rutaImagenes(idServicio) + "/" + idImagen);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(contarImagenes()).isZero();
    assertThat(almacenamiento.clavesEliminadas()).isEmpty();
    assertThat(almacenamiento.contiene(claveGuardada)).isTrue();
  }

  @Test
  void unServicioAjenoNoRecibeImagenes() {
    long idAjeno = idDe(crearServicio("De la primera"));

    NavegadorDePrueba otra = abrirNavegador();
    registrar(otra, CORREO_OTRA_PERSONA, CLAVE);
    iniciarSesion(otra, CORREO_OTRA_PERSONA, CLAVE);
    otra.post(RUTA_PERFIL, solicitudDePerfil());

    HttpResponse<String> subida =
        otra.postArchivo(rutaImagenes(idAjeno), "foto.png", "image/png", imagenPng(), Map.of());

    assertThat(subida.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(almacenamiento.cantidadDeObjetos()).isZero();
  }

  private HttpResponse<String> subir(long idServicio) {
    HttpResponse<String> respuesta =
        navegador.postArchivo(
            rutaImagenes(idServicio), "foto.png", "image/png", imagenPng(), Map.of());
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return respuesta;
  }

  private long idDeImagen(HttpResponse<String> respuesta) {
    return json(respuesta).get("idImagenServicioPublicado").asLong();
  }

  private String rutaImagenes(long idServicio) {
    return RUTA_SERVICIOS_PROPIOS + "/" + idServicio + "/imagenes";
  }

  private Integer contarImagenes() {
    return jdbc.queryForObject("SELECT count(*) FROM imagen_servicio_publicado", Integer.class);
  }
}
