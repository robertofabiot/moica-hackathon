package com.moica.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.moica.demo.service.DemoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class BootstrapDeDemoTest {
  private final DemoService demo = mock(DemoService.class);
  private final ApplicationContextRunner contexto =
      new ApplicationContextRunner()
          .withUserConfiguration(Config.class)
          .withBean(DemoService.class, () -> demo)
          .withBean(BootstrapDeDemo.class);

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(PropiedadesDeDemo.class)
  static class Config {}

  @Test
  void sinPropiedadNoSeInvocaLaPersistencia() {
    contexto.run(
        c -> {
          assertThat(c.getBean(PropiedadesDeDemo.class).enabled()).isFalse();
          c.getBean(BootstrapDeDemo.class).run(null);
          verifyNoInteractions(demo);
        });
  }

  @Test
  void falseExplicitoNoInvocaLaPersistencia() {
    contexto
        .withPropertyValues("moica.seed.demo.enabled=false")
        .run(
            c -> {
              c.getBean(BootstrapDeDemo.class).run(null);
              verifyNoInteractions(demo);
            });
  }

  @Test
  void trueActivaElBootstrapYEnlazaElMapeo() {
    contexto
        .withPropertyValues(
            "moica.seed.demo.enabled=true",
            "moica.seed.demo.imagenes.computadoras="
                + ImagenesDeDemostracion.LAPTOP
                + ","
                + ImagenesDeDemostracion.PROCESADOR)
        .run(
            c -> {
              c.getBean(BootstrapDeDemo.class).run(null);
              verify(demo).sincronizar();
              assertThat(c.getBean(PropiedadesDeDemo.class).clavesDe("computadoras"))
                  .containsExactly(
                      ImagenesDeDemostracion.LAPTOP, ImagenesDeDemostracion.PROCESADOR);
            });
  }

  @Test
  void imagenesRecuperadasSoloSeAsocianAlBucketComprobado() {
    assertThat(
            ImagenesDeDemostracion.existentes(
                "computadoras", ImagenesDeDemostracion.BASE_COMPROBADA))
        .containsExactly(ImagenesDeDemostracion.LAPTOP, ImagenesDeDemostracion.PROCESADOR);
    assertThat(
            ImagenesDeDemostracion.existentes("computadoras", "https://otro-bucket.example.test"))
        .isEmpty();
    assertThat(
            ImagenesDeDemostracion.existentes("plomeria", ImagenesDeDemostracion.BASE_COMPROBADA))
        .isEmpty();
  }

  @Test
  void mapeosVaciosDeLaConfiguracionNoExigenImagenes() {
    contexto
        .withPropertyValues(
            "moica.seed.demo.imagenes.plomeria=", "moica.seed.demo.imagenes.computadoras=")
        .run(
            c -> {
              assertThat(c.getBean(PropiedadesDeDemo.class).clavesDe("plomeria")).isEmpty();
              assertThat(c.getBean(PropiedadesDeDemo.class).clavesDe("computadoras")).isEmpty();
            });
  }
}
