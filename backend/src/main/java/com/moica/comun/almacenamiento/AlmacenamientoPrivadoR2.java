package com.moica.comun.almacenamiento;

import com.moica.comun.error.ErrorDeAplicacion;
import java.net.URI;
import java.time.Duration;
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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * El almacén privado de expedientes sobre Cloudflare R2, a través de su compatibilidad con S3.
 *
 * <p>La configuración del cliente es la misma que exige R2 en la superficie pública —endpoint
 * {@code https://<cuenta>.r2.cloudflarestorage.com}, región {@code auto}, acceso por estilo de
 * ruta, sin codificación por trozos y checksums solo cuando el servicio los exige—, pero contra
 * **otro bucket y con otro token**. Los dos almacenes no comparten ni credenciales ni
 * configuración: por eso son dos clases y no una parametrizada.
 *
 * <p>La diferencia de fondo es la lectura. El bucket privado no tiene subdominio {@code r2.dev} ni
 * dominio propio, así que no hay ninguna URL que un navegador pueda pedir sin permiso. Lo que
 * entrega esta clase es una URL **prefirmada** que caduca sola: se genera en cada petición ya
 * autorizada, no se guarda en la base de datos ni en ningún registro y deja de servir al vencer.
 *
 * <p>La aplicación no crea buckets ni pide permisos administrativos: trabaja contra un bucket ya
 * aprovisionado con un token limitado a sus objetos (ver {@code Docs/Dev/Almacenamiento.md}).
 *
 * <p>Un fallo del proveedor se registra en el servidor y hacia fuera sale siempre el mismo error
 * uniforme, sin endpoint, sin credenciales, sin bucket y sin clave.
 */
@Service
public class AlmacenamientoPrivadoR2 implements AlmacenamientoDeDocumentosPrivados {

  private static final Logger LOG = LoggerFactory.getLogger(AlmacenamientoPrivadoR2.class);

  private static final Duration TIEMPO_MAXIMO_DE_CONEXION = Duration.ofSeconds(10);
  private static final Duration TIEMPO_MAXIMO_POR_INTENTO = Duration.ofSeconds(30);
  private static final Duration TIEMPO_MAXIMO_TOTAL = Duration.ofSeconds(60);

  private final PropiedadesDeAlmacenamientoPrivado propiedades;
  private final PropiedadesDeDocumentos documentos;

  /** {@code null} cuando el entorno no trae credenciales; usarlo entonces es el error uniforme. */
  private final S3Client cliente;

  /** {@code null} por el mismo motivo: sin credenciales no hay nada que firmar. */
  private final S3Presigner firmante;

  // La anotación es necesaria porque hay dos constructores y Spring debe saber
  // cuál es el suyo; el otro existe solo para las pruebas.
  @Autowired
  public AlmacenamientoPrivadoR2(
      PropiedadesDeAlmacenamientoPrivado propiedades, PropiedadesDeDocumentos documentos) {
    this(
        propiedades,
        documentos,
        propiedades.estaConfigurado() ? construirCliente(propiedades) : null,
        propiedades.estaConfigurado() ? construirFirmante(propiedades) : null);
  }

  /** Existe para las pruebas unitarias, que entregan piezas que no tocan la red. */
  AlmacenamientoPrivadoR2(
      PropiedadesDeAlmacenamientoPrivado propiedades,
      PropiedadesDeDocumentos documentos,
      S3Client cliente,
      S3Presigner firmante) {
    this.propiedades = propiedades;
    this.documentos = documentos;
    this.cliente = cliente;
    this.firmante = firmante;
  }

  @Override
  public void guardar(String clave, byte[] contenido, String tipoMime) {
    exigirConfigurado();
    try {
      cliente.putObject(
          peticion -> peticion.bucket(propiedades.bucketPrivado()).key(clave).contentType(tipoMime),
          RequestBody.fromBytes(contenido));
    } catch (SdkException fallo) {
      // La clave no se escribe: identifica un documento de identidad concreto
      // dentro del bucket y el diccionario la trata como dato privado.
      LOG.error("No se pudo guardar un documento en el almacenamiento privado", fallo);
      throw almacenamientoNoDisponible();
    }
  }

  @Override
  public void eliminar(String clave) {
    exigirConfigurado();
    try {
      // En S3 y en R2, borrar una clave inexistente responde igual que borrar
      // una real: la operación es idempotente y aquí se conserva así.
      cliente.deleteObject(peticion -> peticion.bucket(propiedades.bucketPrivado()).key(clave));
    } catch (SdkException fallo) {
      LOG.error("No se pudo eliminar un documento del almacenamiento privado", fallo);
      throw almacenamientoNoDisponible();
    }
  }

  @Override
  public URI accesoTemporalDeLectura(String clave) {
    exigirConfigurado();
    try {
      GetObjectRequest lectura =
          GetObjectRequest.builder().bucket(propiedades.bucketPrivado()).key(clave).build();

      GetObjectPresignRequest firma =
          GetObjectPresignRequest.builder()
              .signatureDuration(documentos.duracionUrlTemporal())
              .getObjectRequest(lectura)
              .build();

      return URI.create(firmante.presignGetObject(firma).url().toString());
    } catch (SdkException fallo) {
      LOG.error("No se pudo firmar el acceso temporal a un documento privado", fallo);
      throw almacenamientoNoDisponible();
    }
  }

  private void exigirConfigurado() {
    if (cliente == null || firmante == null) {
      // Aviso para quien opera el despliegue; la respuesta al cliente es la
      // misma que ante un proveedor caído.
      LOG.warn(
          "Se pidió el almacenamiento de expedientes pero las variables MOICA_R2_PRIVADO_* no"
              + " están definidas");
      throw almacenamientoNoDisponible();
    }
  }

  private static ErrorDeAplicacion almacenamientoNoDisponible() {
    return new ErrorDeAplicacion(
        HttpStatus.SERVICE_UNAVAILABLE,
        "ALMACENAMIENTO_NO_DISPONIBLE",
        "No pudimos procesar los documentos en este momento. Inténtalo de nuevo en unos minutos.");
  }

  private static URI endpointDe(PropiedadesDeAlmacenamientoPrivado propiedades) {
    return URI.create("https://" + propiedades.idCuenta() + ".r2.cloudflarestorage.com");
  }

  private static StaticCredentialsProvider credencialesDe(
      PropiedadesDeAlmacenamientoPrivado propiedades) {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(propiedades.accessKeyId(), propiedades.secretAccessKey()));
  }

  private static S3Configuration configuracionDeR2() {
    return S3Configuration.builder()
        .pathStyleAccessEnabled(true)
        .chunkedEncodingEnabled(false)
        .build();
  }

  private static S3Client construirCliente(PropiedadesDeAlmacenamientoPrivado propiedades) {
    return S3Client.builder()
        .endpointOverride(endpointDe(propiedades))
        .region(Region.of("auto"))
        .credentialsProvider(credencialesDe(propiedades))
        .serviceConfiguration(configuracionDeR2())
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

  /**
   * El firmante no habla con R2: calcula la firma con las mismas credenciales y devuelve la URL.
   * Por eso no necesita cliente HTTP ni tiempos de espera, pero sí exactamente el mismo endpoint y
   * el mismo estilo de ruta, o la URL firmada apuntaría a otro sitio.
   */
  private static S3Presigner construirFirmante(PropiedadesDeAlmacenamientoPrivado propiedades) {
    return S3Presigner.builder()
        .endpointOverride(endpointDe(propiedades))
        .region(Region.of("auto"))
        .credentialsProvider(credencialesDe(propiedades))
        .serviceConfiguration(configuracionDeR2())
        .build();
  }
}
