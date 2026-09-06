package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

/** El máximo por imagen debe ser positivo y configurable; con otra cosa no se arranca. */
class PropiedadesDeImagenesTest {

  @Test
  void aceptaUnMaximoPositivo() {
    assertThat(new PropiedadesDeImagenes(DataSize.ofMegabytes(5)).tamanoMaximo())
        .isEqualTo(DataSize.ofMegabytes(5));
  }

  @Test
  void rechazaUnMaximoNuloCeroONegativo() {
    assertThatThrownBy(() -> new PropiedadesDeImagenes(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MOICA_IMAGEN_TAMANO_MAXIMO");
    assertThatThrownBy(() -> new PropiedadesDeImagenes(DataSize.ofBytes(0)))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new PropiedadesDeImagenes(DataSize.ofBytes(-1)))
        .isInstanceOf(IllegalStateException.class);
  }
}
