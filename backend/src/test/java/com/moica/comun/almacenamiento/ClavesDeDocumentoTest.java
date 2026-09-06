package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Las claves del expediente: con su prefijo, opacas y sin rastro de nada. */
class ClavesDeDocumentoTest {

  @Test
  void llevaElPrefijoDelExpedienteYLaExtensionDelFormatoReal() {
    assertThat(ClavesDeDocumento.nueva(TipoDeDocumento.PDF))
        .startsWith("expedientes/")
        .endsWith(".pdf");
    assertThat(ClavesDeDocumento.nueva(TipoDeDocumento.JPEG)).endsWith(".jpg");
    assertThat(ClavesDeDocumento.nueva(TipoDeDocumento.PNG)).endsWith(".png");
  }

  @Test
  void elCuerpoDeLaClaveSonTreintaYDosHexadecimalesAleatorios() {
    String clave = ClavesDeDocumento.nueva(TipoDeDocumento.PDF);
    String cuerpo = clave.substring("expedientes/".length(), clave.length() - ".pdf".length());

    assertThat(cuerpo).hasSize(32).matches("[0-9a-f]{32}");
  }

  @Test
  void dosClavesSeguidasNoSeParecen() {
    Set<String> generadas = new HashSet<>();
    for (int intento = 0; intento < 500; intento++) {
      generadas.add(ClavesDeDocumento.nueva(TipoDeDocumento.PNG));
    }

    assertThat(generadas).hasSize(500);
  }
}
