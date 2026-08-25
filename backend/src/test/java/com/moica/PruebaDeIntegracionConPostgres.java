package com.moica;

import com.moica.comun.almacenamiento.AlmacenamientoDePrueba;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de las pruebas que necesitan la aplicación entera y PostgreSQL real.
 *
 * <p>Se usa Testcontainers y no H2: solo PostgreSQL real demuestra las restricciones {@code CHECK},
 * los índices y las claves foráneas que sí utiliza el diccionario de datos.
 *
 * <p>El contenedor es único para toda la suite. Se arranca una sola vez al cargar esta clase y
 * Testcontainers lo retira cuando termina la ejecución, de modo que añadir pruebas de integración
 * no multiplique el tiempo de arranque.
 *
 * <p>Necesita Docker en ejecución. Las ejecuta {@code ./mvnw verify}.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      // Testcontainers entrega la conexión real mediante @ServiceConnection; estos
      // valores solo evitan que las variables de entorno del despliegue queden sin
      // resolver al construir el contexto.
      "MOICA_DB_NOMBRE=moica_prueba",
      "MOICA_DB_USUARIO=moica_prueba",
      "MOICA_DB_CLAVE=moica_prueba",
      // Secreto de pruebas. No es el de ningún entorno real: los despliegues lo
      // reciben por variable de entorno y nunca se versiona.
      "MOICA_JWT_SECRETO=" + PruebaDeIntegracionConPostgres.SECRETO_JWT,
      // Clave AES de pruebas para cifrar los secretos TOTP. Tampoco es la de
      // ningún entorno real: llega por MOICA_TOTP_CLAVE_CIFRADO y no se versiona.
      "MOICA_TOTP_CLAVE_CIFRADO=" + PruebaDeIntegracionConPostgres.CLAVE_DE_CIFRADO_TOTP,
      // El detalle de salud está cerrado en producción; aquí se abre para poder
      // afirmar que el componente de base de datos es el que responde.
      "management.endpoint.health.show-details=always",
      "management.endpoint.health.show-components=always"
    })
@Import(PruebaDeIntegracionConPostgres.ConfiguracionDeAlmacenamientoDePrueba.class)
public abstract class PruebaDeIntegracionConPostgres {

  /**
   * Sustituye el almacén real por el doble en memoria en toda la suite.
   *
   * <p>Las variables {@code MOICA_R2_*} no existen en las pruebas, así que el bean real arranca sin
   * cliente; este doble es quien recibe las llamadas y permite afirmar sobre ellas. La comprobación
   * contra un bucket R2 real queda como procedimiento manual documentado en {@code
   * Docs/Dev/Almacenamiento.md}.
   */
  @TestConfiguration
  public static class ConfiguracionDeAlmacenamientoDePrueba {

    @Bean
    @Primary
    public AlmacenamientoDePrueba almacenamientoDePrueba() {
      return new AlmacenamientoDePrueba();
    }
  }

  /** Clave con la que se firman los JWT durante las pruebas. */
  public static final String SECRETO_JWT = "secreto-de-pruebas-de-moica-solo-para-testcontainers";

  /** Clave AES-256 en Base64 con la que se cifran los secretos TOTP durante las pruebas. */
  public static final String CLAVE_DE_CIFRADO_TOTP = "Y2xhdmUtZGUtcHJ1ZWJhcy10b3RwLWRlLW1vaWNhISE=";

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

  static {
    POSTGRES.start();
  }
}
