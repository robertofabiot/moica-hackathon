package com.moica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API de Moica.
 *
 * <p>El backend es un monolito modular: cada capacidad vivirá en su propio paquete bajo {@code
 * com.moica} con sus capas clásicas. Los paquetes se crean cuando el incremento que los necesita
 * los implementa.
 */
@SpringBootApplication
public class MoicaApplication {

  public static void main(String[] args) {
    SpringApplication.run(MoicaApplication.class, args);
  }
}
