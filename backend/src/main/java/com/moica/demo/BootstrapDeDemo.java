package com.moica.demo;

import com.moica.demo.service.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapDeDemo implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(BootstrapDeDemo.class);
  private final PropiedadesDeDemo propiedades;
  private final DemoService demo;

  public BootstrapDeDemo(PropiedadesDeDemo propiedades, DemoService demo) {
    this.propiedades = propiedades;
    this.demo = demo;
  }

  @Override
  public void run(ApplicationArguments argumentos) {
    if (propiedades.enabled()) {
      // El servicio ya confirmó su transacción: este resumen nunca anuncia un commit pendiente.
      LOG.info("Demo sincronizada: {}", demo.sincronizar());
    }
  }
}
