package com.moica.comun.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Lo que se le quita al nombre que escribió el cliente antes de guardarlo. */
class NombreDeArchivoTest {

  @Test
  void conservaUnNombreNormal() {
    assertThat(NombreDeArchivo.saneado("cedula frente.jpg")).isEqualTo("cedula frente.jpg");
  }

  @Test
  void descartaLaRutaYSeQuedaConElUltimoSegmento() {
    assertThat(NombreDeArchivo.saneado("../../etc/passwd")).isEqualTo("passwd");
    assertThat(NombreDeArchivo.saneado("C:\\Users\\erving\\cedula.pdf")).isEqualTo("cedula.pdf");
    assertThat(NombreDeArchivo.saneado("/var/tmp/constancia.png")).isEqualTo("constancia.png");
  }

  @Test
  void quitaLosCaracteresDeControlQuePartirianUnaLineaDeRegistro() {
    assertThat(NombreDeArchivo.saneado("cedula\r\nINFO fila falsa.pdf"))
        .isEqualTo("cedulaINFO fila falsa.pdf");
    assertThat(NombreDeArchivo.saneado("cedula\u0000.pdf")).isEqualTo("cedula.pdf");
  }

  @Test
  void quitaLosCaracteresQueNoAdmiteUnNombreDeArchivo() {
    assertThat(NombreDeArchivo.saneado("re<port>e:\"?*|.pdf")).isEqualTo("reporte.pdf");
  }

  @Test
  void colapsaLosEspaciosSobrantes() {
    assertThat(NombreDeArchivo.saneado("   cedula    frente.jpg  ")).isEqualTo("cedula frente.jpg");
  }

  @Test
  void loQueNoDejaNadaUtilizableRecibeUnNombrePorOmision() {
    assertThat(NombreDeArchivo.saneado(null)).isEqualTo(NombreDeArchivo.NOMBRE_POR_OMISION);
    assertThat(NombreDeArchivo.saneado("")).isEqualTo(NombreDeArchivo.NOMBRE_POR_OMISION);
    assertThat(NombreDeArchivo.saneado("   ")).isEqualTo(NombreDeArchivo.NOMBRE_POR_OMISION);
    assertThat(NombreDeArchivo.saneado("..")).isEqualTo(NombreDeArchivo.NOMBRE_POR_OMISION);
  }

  @Test
  void seRecortaALoQueCabeEnLaColumna() {
    String largo = "a".repeat(400) + ".pdf";

    assertThat(NombreDeArchivo.saneado(largo)).hasSize(255);
  }
}
