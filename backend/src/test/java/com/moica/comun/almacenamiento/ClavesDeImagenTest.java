package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Las claves de los objetos: prefijo correcto, extensión real y nada adivinable. */
class ClavesDeImagenTest {

  @Test
  void llevaElPrefijoDeSuSuperficieYLaExtensionDelFormatoReal() {
    assertThat(ClavesDeImagen.nueva(ClavesDeImagen.PREFIJO_PERFILES, TipoDeImagen.PNG))
        .startsWith("perfiles/")
        .endsWith(".png");
    assertThat(ClavesDeImagen.nueva(ClavesDeImagen.PREFIJO_TRABAJOS, TipoDeImagen.JPEG))
        .startsWith("trabajos/")
        .endsWith(".jpg");
    assertThat(ClavesDeImagen.nueva(ClavesDeImagen.PREFIJO_SERVICIOS, TipoDeImagen.WEBP))
        .startsWith("servicios/")
        .endsWith(".webp");
  }

  @Test
  void dosClavesNuncaCoinciden() {
    Set<String> claves = new HashSet<>();
    for (int intento = 0; intento < 1000; intento++) {
      claves.add(ClavesDeImagen.nueva(ClavesDeImagen.PREFIJO_PERFILES, TipoDeImagen.PNG));
    }
    assertThat(claves).hasSize(1000);
  }

  @Test
  void laParteAleatoriaEsLargaYSinCaracteresProblematicos() {
    String clave = ClavesDeImagen.nueva(ClavesDeImagen.PREFIJO_PERFILES, TipoDeImagen.WEBP);
    String aleatoria = clave.substring("perfiles/".length(), clave.indexOf(".webp"));

    // 32 dígitos hexadecimales: 128 bits de aleatoriedad criptográfica.
    assertThat(aleatoria).hasSize(32).matches("[0-9a-f]+");
  }
}
