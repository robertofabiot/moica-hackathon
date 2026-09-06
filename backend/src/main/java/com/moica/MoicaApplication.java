package com.moica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada de la API de Moica.
 *
 * <p>El backend es un monolito modular: cada capacidad vive en su propio paquete bajo {@code
 * com.moica} con sus capas clásicas. Los paquetes se crean cuando el incremento que los necesita
 * los implementa.
 *
 * <p>{@link ConfigurationPropertiesScan} permite que cada capacidad declare sus propios parámetros
 * de configuración sin tener que registrarlos aquí.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MoicaApplication {

  public static void main(String[] args) {
    SpringApplication.run(MoicaApplication.class, args);
  }
}
