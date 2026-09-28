package com.moica;

import com.moica.comun.almacenamiento.AlmacenamientoDePrueba;
import com.moica.comun.almacenamiento.AlmacenamientoPrivadoDePrueba;
import com.moica.comun.almacenamiento.PropiedadesDeDocumentos;
import java.time.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Sustituye los dos almacenes reales por dobles en memoria en toda la suite.
 *
 * <p>Ni las variables {@code MOICA_R2_*} ni las {@code MOICA_R2_PRIVADO_*} existen en las pruebas,
 * así que los beans reales arrancan sin cliente; estos dobles son quienes reciben las llamadas y
 * permiten afirmar sobre ellas. Siguen siendo **dos** superficies separadas, igual que en
 * producción: un doble para las imágenes públicas y otro para los expedientes privados. La
 * comprobación contra buckets R2 reales queda como procedimiento manual documentado en {@code
 * Docs/Dev/Almacenamiento.md}.
 *
 * <p>Vive en su propio archivo y no anidada en {@link PruebaDeIntegracionConPostgres}: Spring 7
 * detecta las clases de configuración anidadas como configuración por omisión de cada prueba y
 * avisa de que en 7.1 dejará de ignorarlas.
 */
@TestConfiguration
public class ConfiguracionDeAlmacenamientoDePrueba {

  @Bean
  @Primary
  public AlmacenamientoDePrueba almacenamientoDePrueba() {
    return new AlmacenamientoDePrueba();
  }

  @Bean
  @Primary
  public AlmacenamientoPrivadoDePrueba almacenamientoPrivadoDePrueba(
      PropiedadesDeDocumentos propiedades, Clock reloj) {
    return new AlmacenamientoPrivadoDePrueba(propiedades, reloj);
  }
}
