package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moica.comun.error.ErrorDeAplicacion;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * El adaptador del bucket privado sin tocar la red.
 *
 * <p>La firma de una URL prefirmada es un cálculo local: no hay ninguna llamada a R2. Por eso el
 * acceso temporal se comprueba con un {@link S3Presigner} **real** y no con un doble, y así la
 * prueba demuestra de verdad que la duración configurada llega hasta la URL y que la dirección
 * apunta al bucket privado.
 */
class AlmacenamientoPrivadoR2Test {

  private static final PropiedadesDeAlmacenamientoPrivado PROPIEDADES =
      new PropiedadesDeAlmacenamientoPrivado(
          "cuenta-de-prueba", "access-key-privada", "secreto-de-prueba", "moica-privado");

  private static final PropiedadesDeDocumentos DOCUMENTOS =
      new PropiedadesDeDocumentos(DataSize.ofMegabytes(5), Duration.ofMinutes(5));

  @Test
  void sinConfigurarRespondeElErrorUniformeSinIntentarNada() {
    AlmacenamientoPrivadoR2 almacenamiento =
        new AlmacenamientoPrivadoR2(
            new PropiedadesDeAlmacenamientoPrivado(null, null, null, null), DOCUMENTOS);

    assertThatThrownBy(
            () -> almacenamiento.guardar("expedientes/a.pdf", new byte[] {1}, "application/pdf"))
        .isInstanceOfSatisfying(
            ErrorDeAplicacion.class, AlmacenamientoPrivadoR2Test::esErrorUniforme);
    assertThatThrownBy(() -> almacenamiento.eliminar("expedientes/a.pdf"))
        .isInstanceOfSatisfying(
            ErrorDeAplicacion.class, AlmacenamientoPrivadoR2Test::esErrorUniforme);
    assertThatThrownBy(() -> almacenamiento.accesoTemporalDeLectura("expedientes/a.pdf"))
        .isInstanceOfSatisfying(
            ErrorDeAplicacion.class, AlmacenamientoPrivadoR2Test::esErrorUniforme);
  }

  @Test
  void guardarEnviaElBucketPrivadoLaClaveYElTipo() {
    ClienteDePrueba cliente = new ClienteDePrueba();
    AlmacenamientoPrivadoR2 almacenamiento = almacenamientoCon(cliente);

    almacenamiento.guardar("expedientes/abc.pdf", new byte[] {1, 2, 3}, "application/pdf");

    assertThat(cliente.bucketDelPut).isEqualTo("moica-privado");
    assertThat(cliente.claveDelPut).isEqualTo("expedientes/abc.pdf");
    assertThat(cliente.tipoMimeDelPut).isEqualTo("application/pdf");
  }

  @Test
  void eliminarEnviaElBucketPrivadoYLaClave() {
    ClienteDePrueba cliente = new ClienteDePrueba();

    almacenamientoCon(cliente).eliminar("expedientes/xyz.png");

    assertThat(cliente.bucketDelDelete).isEqualTo("moica-privado");
    assertThat(cliente.claveDelDelete).isEqualTo("expedientes/xyz.png");
  }

  @Test
  void elAccesoTemporalLlevaLaDuracionConfiguradaYApuntaAlBucketPrivado() {
    AlmacenamientoPrivadoR2 almacenamiento = almacenamientoCon(new ClienteDePrueba());

    URI acceso = almacenamiento.accesoTemporalDeLectura("expedientes/abc.pdf");

    assertThat(acceso.toString())
        .as("acceso por estilo de ruta contra el endpoint de R2, como exige el proveedor")
        .startsWith("https://cuenta-de-prueba.r2.cloudflarestorage.com/moica-privado/")
        .contains("expedientes/abc.pdf")
        .as("los 300 segundos de PT5M viajan en la propia firma")
        .contains("X-Amz-Expires=300")
        .contains("X-Amz-Signature=");
    assertThat(acceso.toString())
        .as("la URL firmada no lleva el secreto del token")
        .doesNotContain("secreto-de-prueba");
  }

  @Test
  void otraDuracionConfiguradaCambiaLaCaducidadDeLaFirma() {
    PropiedadesDeDocumentos deQuinceMinutos =
        new PropiedadesDeDocumentos(DataSize.ofMegabytes(5), Duration.ofMinutes(15));
    AlmacenamientoPrivadoR2 almacenamiento =
        new AlmacenamientoPrivadoR2(
            PROPIEDADES, deQuinceMinutos, new ClienteDePrueba(), firmanteReal());

    assertThat(almacenamiento.accesoTemporalDeLectura("expedientes/abc.pdf").toString())
        .contains("X-Amz-Expires=900");
  }

  @Test
  void unFalloDelProveedorSaleComoErrorUniformeSinDetalleInterno() {
    ClienteDePrueba cliente = new ClienteDePrueba();
    cliente.fallo =
        SdkClientException.create(
            "Unable to execute HTTP request to cuenta-de-prueba.r2.cloudflarestorage.com");

    assertThatThrownBy(
            () ->
                almacenamientoCon(cliente)
                    .guardar("expedientes/a.pdf", new byte[] {1}, "application/pdf"))
        .isInstanceOfSatisfying(
            ErrorDeAplicacion.class,
            error -> {
              esErrorUniforme(error);
              assertThat(error.getMessage())
                  .doesNotContain("cloudflarestorage")
                  .doesNotContain("cuenta-de-prueba")
                  .doesNotContain("moica-privado")
                  .doesNotContain("expedientes/");
            });
  }

  private static AlmacenamientoPrivadoR2 almacenamientoCon(S3Client cliente) {
    return new AlmacenamientoPrivadoR2(PROPIEDADES, DOCUMENTOS, cliente, firmanteReal());
  }

  /** Firmante de verdad: calcula la firma en local, sin ninguna llamada a R2. */
  private static S3Presigner firmanteReal() {
    return S3Presigner.builder()
        .endpointOverride(URI.create("https://cuenta-de-prueba.r2.cloudflarestorage.com"))
        .region(Region.of("auto"))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("access-key-privada", "secreto-de-prueba")))
        .serviceConfiguration(
            S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build())
        .build();
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
      return "s3-privado-de-prueba";
    }

    @Override
    public void close() {}
  }
}
