package com.moica.comun.almacenamiento;

import com.moica.comun.error.ErrorDeAplicacion;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * El almacén público sobre Cloudflare R2, a través de su compatibilidad con S3.
 *
 * <p>La configuración del cliente es la que exige R2 y documenta Cloudflare para el SDK v2 de AWS:
 * endpoint {@code https://<cuenta>.r2.cloudflarestorage.com}, región {@code auto}, acceso por
 * estilo de ruta, sin codificación por trozos —R2 rechaza la firma de {@code putObject} con ella— y
 * checksums solo cuando el servicio los exige, porque R2 no admite los que el SDK calcula por
 * omisión desde su versión 2.30.
 *
 * <p>La aplicación no crea buckets ni pide permisos administrativos: trabaja contra un bucket ya
 * aprovisionado con un token limitado a sus objetos (ver {@code Docs/Dev/Almacenamiento.md}).
 *
 * <p>Un fallo del proveedor se registra en el servidor y hacia fuera sale siempre el mismo error
 * uniforme, sin endpoint, sin credenciales y sin detalle interno.
 */
@Service
public class AlmacenamientoR2 implements AlmacenamientoDeImagenesPublicas {

  private static final Logger LOG = LoggerFactory.getLogger(AlmacenamientoR2.class);

  private static final Duration TIEMPO_MAXIMO_DE_CONEXION = Duration.ofSeconds(10);
  private static final Duration TIEMPO_MAXIMO_POR_INTENTO = Duration.ofSeconds(30);
  private static final Duration TIEMPO_MAXIMO_TOTAL = Duration.ofSeconds(60);

  private final PropiedadesDeAlmacenamiento propiedades;

  /** {@code null} cuando el entorno no trae credenciales; usarlo entonces es el error uniforme. */
  private final S3Client cliente;

  // La anotación es necesaria porque hay dos constructores y Spring debe saber
  // cuál es el suyo; el otro existe solo para las pruebas.
  @Autowired
  public AlmacenamientoR2(PropiedadesDeAlmacenamiento propiedades) {
    this(propiedades, propiedades.estaConfigurado() ? construirCliente(propiedades) : null);
  }

  /** Existe para las pruebas unitarias, que entregan un cliente que no toca la red. */
  AlmacenamientoR2(PropiedadesDeAlmacenamiento propiedades, S3Client cliente) {
    this.propiedades = propiedades;
    this.cliente = cliente;
  }

  @Override
  public String guardar(String clave, byte[] contenido, String tipoMime) {
    exigirConfigurado();
    try {
      cliente.putObject(
          peticion -> peticion.bucket(propiedades.bucketPublico()).key(clave).contentType(tipoMime),
          RequestBody.fromBytes(contenido));
    } catch (SdkException fallo) {
      LOG.error("No se pudo guardar el objeto {} en el almacenamiento público", clave, fallo);
      throw almacenamientoNoDisponible();
    }
    return propiedades.urlPublicaDe(clave);
  }

  @Override
  public void eliminar(String clave) {
    exigirConfigurado();
    try {
      // En S3 y en R2, borrar una clave inexistente responde igual que borrar
      // una real: la operación es idempotente y aquí se conserva así.
      cliente.deleteObject(peticion -> peticion.bucket(propiedades.bucketPublico()).key(clave));
    } catch (SdkException fallo) {
      LOG.error("No se pudo eliminar el objeto {} del almacenamiento público", clave, fallo);
      throw almacenamientoNoDisponible();
    }
  }

  @Override
  public Optional<String> claveDe(String urlPublica) {
    return propiedades.claveDe(urlPublica);
  }

  private void exigirConfigurado() {
    if (cliente == null) {
      // Aviso para quien opera el despliegue; la respuesta al cliente es la
      // misma que ante un proveedor caído.
      LOG.warn(
          "Se pidió el almacenamiento de imágenes pero las variables MOICA_R2_* no están"
              + " definidas");
      throw almacenamientoNoDisponible();
    }
  }

  private static ErrorDeAplicacion almacenamientoNoDisponible() {
    return new ErrorDeAplicacion(
        HttpStatus.SERVICE_UNAVAILABLE,
        "ALMACENAMIENTO_NO_DISPONIBLE",
        "No pudimos procesar la imagen en este momento. Inténtalo de nuevo en unos minutos.");
  }

  private static S3Client construirCliente(PropiedadesDeAlmacenamiento propiedades) {
    return S3Client.builder()
        .endpointOverride(
            URI.create("https://" + propiedades.idCuenta() + ".r2.cloudflarestorage.com"))
        .region(Region.of("auto"))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    propiedades.accessKeyId(), propiedades.secretAccessKey())))
        .serviceConfiguration(
            S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build())
        .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
        .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
        .overrideConfiguration(
            configuracion ->
                configuracion
                    .apiCallAttemptTimeout(TIEMPO_MAXIMO_POR_INTENTO)
                    .apiCallTimeout(TIEMPO_MAXIMO_TOTAL))
        .httpClientBuilder(Apache5HttpClient.builder().connectionTimeout(TIEMPO_MAXIMO_DE_CONEXION))
        .build();
  }
}
