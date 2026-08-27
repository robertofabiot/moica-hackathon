package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moica.comun.error.ErrorDeAplicacion;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * El adaptador de R2 sin tocar la red: qué pide al cliente S3 y cómo traduce sus fallos.
 *
 * <p>Lo importante es la frontera: hacia dentro viajan el bucket, la clave y el tipo reales; hacia
 * fuera solo sale el error uniforme, sin proveedor, sin endpoint y sin credenciales.
 */
class AlmacenamientoR2Test {

  private static final PropiedadesDeAlmacenamiento PROPIEDADES =
      new PropiedadesDeAlmacenamiento(
          "cuenta-de-prueba",
          "access-key",
          "secreto-de-prueba",
          "moica-publico",
          "https://imagenes.moica.ni");

  @Test
  void sinConfigurarRespondeElErrorUniformeSinIntentarNada() {
    AlmacenamientoR2 almacenamiento =
        new AlmacenamientoR2(new PropiedadesDeAlmacenamiento(null, null, null, null, null));

    assertThatThrownBy(() -> almacenamiento.guardar("perfiles/a.png", new byte[] {1}, "image/png"))
        .isInstanceOfSatisfying(ErrorDeAplicacion.class, AlmacenamientoR2Test::esErrorUniforme);
    assertThatThrownBy(() -> almacenamiento.eliminar("perfiles/a.png"))
        .isInstanceOfSatisfying(ErrorDeAplicacion.class, AlmacenamientoR2Test::esErrorUniforme);
  }

  @Test
  void guardarEnviaBucketClaveYTipoYDevuelveLaUrlPublica() {
    ClienteDePrueba cliente = new ClienteDePrueba();
    AlmacenamientoR2 almacenamiento = new AlmacenamientoR2(PROPIEDADES, cliente);

    String url = almacenamiento.guardar("perfiles/abc.png", new byte[] {1, 2, 3}, "image/png");

    assertThat(url).isEqualTo("https://imagenes.moica.ni/perfiles/abc.png");
    assertThat(cliente.bucketDelPut).isEqualTo("moica-publico");
    assertThat(cliente.claveDelPut).isEqualTo("perfiles/abc.png");
    assertThat(cliente.tipoMimeDelPut).isEqualTo("image/png");
  }

  @Test
  void eliminarEnviaBucketYClave() {
    ClienteDePrueba cliente = new ClienteDePrueba();
    AlmacenamientoR2 almacenamiento = new AlmacenamientoR2(PROPIEDADES, cliente);

    almacenamiento.eliminar("trabajos/xyz.webp");

    assertThat(cliente.bucketDelDelete).isEqualTo("moica-publico");
    assertThat(cliente.claveDelDelete).isEqualTo("trabajos/xyz.webp");
  }

  @Test
  void unFalloDelProveedorSaleComoErrorUniformeSinDetalleInterno() {
    ClienteDePrueba cliente = new ClienteDePrueba();
    cliente.fallo =
        SdkClientException.create(
            "Unable to execute HTTP request to cuenta-de-prueba.r2.cloudflarestorage.com");
    AlmacenamientoR2 almacenamiento = new AlmacenamientoR2(PROPIEDADES, cliente);

    assertThatThrownBy(() -> almacenamiento.guardar("perfiles/a.png", new byte[] {1}, "image/png"))
        .isInstanceOfSatisfying(
            ErrorDeAplicacion.class,
            error -> {
              esErrorUniforme(error);
              assertThat(error.getMessage())
                  .doesNotContain("cloudflarestorage")
                  .doesNotContain("cuenta-de-prueba")
                  .doesNotContain("HTTP request");
            });
  }

  private static void esErrorUniforme(ErrorDeAplicacion error) {
    assertThat(error.getEstado()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(error.getCodigo()).isEqualTo("ALMACENAMIENTO_NO_DISPONIBLE");
  }

  /**
   * Un cliente S3 que no toca la red: registra la última petición y falla a demanda.
   *
   * <p>Los métodos del SDK que no se sobrescriben conservan su comportamiento por omisión, que es
   * lanzar {@code UnsupportedOperationException}: si el adaptador llamara otra operación, la prueba
   * lo delataría.
   */
  private static final class ClienteDePrueba implements S3Client {

    // Se guardan los valores y no la petición entera: el objeto del SDK es
    // mutable y conservarlo sería exponer estado ajeno.
    private String bucketDelPut;
    private String claveDelPut;
    private String tipoMimeDelPut;
    private String bucketDelDelete;
    private String claveDelDelete;
    private SdkClientException fallo;

    @Override
    public PutObjectResponse putObject(PutObjectRequest peticion, RequestBody cuerpo) {
      if (fallo != null) {
        throw fallo;
      }
      this.bucketDelPut = peticion.bucket();
      this.claveDelPut = peticion.key();
      this.tipoMimeDelPut = peticion.contentType();
      return PutObjectResponse.builder().build();
    }

    @Override
    public DeleteObjectResponse deleteObject(DeleteObjectRequest peticion) {
      if (fallo != null) {
        throw fallo;
      }
      this.bucketDelDelete = peticion.bucket();
      this.claveDelDelete = peticion.key();
      return DeleteObjectResponse.builder().build();
    }

    @Override
    public String serviceName() {
      return "s3-de-prueba";
    }

    @Override
    public void close() {}
  }
}
