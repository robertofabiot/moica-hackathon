package com.moica.comun.configuracion;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj de la aplicación.
 *
 * <p>Existe para que nada lea la hora de un método estático cuando esa hora forma parte de una
 * regla: una prueba necesita situarse en el instante que le convenga, y esperar treinta segundos
 * reales no es una prueba. Hoy lo usa el segundo factor, cuyos códigos duran justamente eso.
 *
 * <p>Vive en {@code comun} porque el tiempo no pertenece a ninguna capacidad concreta.
 */
@Configuration
public class ConfiguracionDelReloj {

  @Bean
  public Clock relojDeLaAplicacion() {
    return Clock.systemUTC();
  }
}
