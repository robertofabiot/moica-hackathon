package com.moica.prestador;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * La imagen de perfil contra el doble de almacenamiento: subir, sustituir y eliminar.
 *
 * <p>Además del resultado visible, se afirma sobre el ciclo de vida de los objetos: la clave es
 * opaca y con su prefijo, la sustitución retira el objeto anterior solo después de persistir el
 * nuevo, la compensación retira el objeto recién subido si la persistencia falla y un proveedor
 * caído responde el error uniforme sin tocar la base.
 */
class ImagenDePerfilIT extends EscenarioDePrestador {

  private static final String NOMBRE_ORIGINAL = "mi foto de perfil.png";

  @BeforeEach
  void prepararPerfil() {
    crearPerfil();
  }

  @Test
  void subeUnaImagenYPersisteSoloSuUrlPublica() {
    HttpResponse<String> respuesta =
        navegador.putArchivo(RUTA_IMAGEN, NOMBRE_ORIGINAL, "image/png", imagenPng());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());

    String url = json(respuesta).get("urlImagenPerfil").asText();
    assertThat(url).startsWith("https://imagenes.moica.test/perfiles/");
    assertThat(url).as("la clave es opaca: nada del nombre original").doesNotContain("foto");

    assertThat(jdbc.queryForObject("SELECT url_imagen_perfil FROM perfil_prestador", String.class))
        .isEqualTo(url);

    String clave = almacenamiento.claveDe(url).orElseThrow();
    assertThat(clave).endsWith(".png");
    assertThat(almacenamiento.objeto(clave).orElseThrow().tipoMime()).isEqualTo("image/png");
    assertThat(almacenamiento.objeto(clave).orElseThrow().contenido()).isEqualTo(imagenPng());
  }

  @Test
  void admiteTambienJpegYWebp() {
    assertThat(
            navegador.putArchivo(RUTA_IMAGEN, "foto.jpg", "image/jpeg", imagenJpeg()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(
            navegador.putArchivo(RUTA_IMAGEN, "foto.webp", "image/webp", imagenWebp()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void sustituirLaImagenRetiraElObjetoAnterior() {
    navegador.putArchivo(RUTA_IMAGEN, "primera.png", "image/png", imagenPng());
    String claveAnterior = almacenamiento.clavesGuardadas().get(0);

    HttpResponse<String> respuesta =
        navegador.putArchivo(RUTA_IMAGEN, "segunda.jpg", "image/jpeg", imagenJpeg());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(almacenamiento.contiene(claveAnterior))
        .as("el objeto viejo se retira una vez persistido el nuevo")
        .isFalse();
    assertThat(almacenamiento.cantidadDeObjetos()).isEqualTo(1);
  }

  @Test
  void eliminarLaImagenLimpiaLaBaseYRetiraElObjeto() {
    navegador.putArchivo(RUTA_IMAGEN, "foto.png", "image/png", imagenPng());

    HttpResponse<String> respuesta = navegador.delete(RUTA_IMAGEN);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("urlImagenPerfil").isNull()).isTrue();
    assertThat(jdbc.queryForObject("SELECT url_imagen_perfil FROM perfil_prestador", String.class))
        .isNull();
    assertThat(almacenamiento.cantidadDeObjetos()).isZero();
  }

  @Test
  void rechazaUnArchivoMayorQueElMaximoConfigurado() {
    byte[] enorme = Arrays.copyOf(imagenPng(), 5 * 1024 * 1024 + 1);

    HttpResponse<String> respuesta =
        navegador.putArchivo(RUTA_IMAGEN, "enorme.png", "image/png", enorme);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("IMAGEN_DEMASIADO_GRANDE");
    assertThat(almacenamiento.cantidadDeObjetos()).isZero();
  }

  @Test
  void rechazaUnFormatoQueNoEstaAdmitido() {
    byte[] gif = "GIF89a-lo-que-sea".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    HttpResponse<String> respuesta =
        navegador.putArchivo(RUTA_IMAGEN, "foto.gif", "image/gif", gif);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("IMAGEN_NO_ADMITIDA");
  }

  @Test
  void rechazaUnaCabeceraQueNoCorrespondeConLaFirmaReal() {
    // Cabecera PNG con contenido JPEG: la extensión y el Content-Type los
    // escribe el cliente, la firma no.
    HttpResponse<String> respuesta =
        navegador.putArchivo(RUTA_IMAGEN, "disfrazada.png", "image/png", imagenJpeg());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("IMAGEN_NO_ADMITIDA");
    assertThat(almacenamiento.cantidadDeObjetos()).isZero();
  }

  @Test
  void conElAlmacenamientoCaidoRespondeElErrorUniformeSinTocarLaBase() {
    almacenamiento.simularNoDisponible();

    HttpResponse<String> respuesta =
        navegador.putArchivo(RUTA_IMAGEN, "foto.png", "image/png", imagenPng());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("ALMACENAMIENTO_NO_DISPONIBLE");
    assertThat(respuesta.body())
        .as("el error no revela proveedor, endpoint ni credenciales")
        .doesNotContain("R2")
        .doesNotContain("cloudflare")
        .doesNotContain("S3");
    assertThat(jdbc.queryForObject("SELECT url_imagen_perfil FROM perfil_prestador", String.class))
        .isNull();
  }

  @Test
  void siLaPersistenciaFallaElObjetoRecienSubidoSeCompensa() {
    almacenamiento.simularUrlInutilizable();

    HttpResponse<String> respuesta =
        navegador.putArchivo(RUTA_IMAGEN, "foto.png", "image/png", imagenPng());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(jdbc.queryForObject("SELECT url_imagen_perfil FROM perfil_prestador", String.class))
        .as("la base no quedó apuntando a nada")
        .isNull();
    assertThat(almacenamiento.cantidadDeObjetos())
        .as("la compensación retiró el objeto que acababa de subirse")
        .isZero();
    assertThat(almacenamiento.clavesEliminadas())
        .containsExactlyElementsOf(almacenamiento.clavesGuardadas());
  }

  @Test
  void unaCuentaRestringidaNoPuedeSubirNiQuitarLaImagen() {
    restringirCuenta(CORREO);

    HttpResponse<String> subida =
        navegador.putArchivo(RUTA_IMAGEN, "foto.png", "image/png", imagenPng());
    assertThat(subida.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(subida)).isEqualTo("CUENTA_RESTRINGIDA");

    HttpResponse<String> borrado = navegador.delete(RUTA_IMAGEN);
    assertThat(codigoDeError(borrado)).isEqualTo("CUENTA_RESTRINGIDA");
  }

  @Test
  void subirLaImagenSinTokenCsrfEstaProhibido() {
    HttpResponse<String> respuesta =
        navegador.putArchivoSinTokenCsrf(RUTA_IMAGEN, "foto.png", "image/png", imagenPng());

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(almacenamiento.cantidadDeObjetos()).isZero();
  }
}
