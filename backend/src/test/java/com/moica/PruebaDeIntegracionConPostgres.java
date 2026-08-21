package com.moica;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
      // El detalle de salud está cerrado en producción; aquí se abre para poder
      // afirmar que el componente de base de datos es el que responde.
      "management.endpoint.health.show-details=always",
      "management.endpoint.health.show-components=always"
    })
public abstract class PruebaDeIntegracionConPostgres {

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

  static {
    POSTGRES.start();
  }
}
