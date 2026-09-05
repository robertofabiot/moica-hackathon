package com.moica.comun.configuracion;

import com.moica.auth.seguridad.PropiedadesDeSeguridad;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Impide publicar por accidente la configuracion de desarrollo. */
@Configuration
@Profile("prod")
public class ConfiguracionDeProduccion {

  public ConfiguracionDeProduccion(PropiedadesDeSeguridad seguridad, PropiedadesDeSoporte soporte) {
    if (!seguridad.cookieSegura()) {
      throw new IllegalStateException("El perfil prod exige MOICA_COOKIE_SEGURA=true.");
    }
    if ("soporte@moica.ni".equalsIgnoreCase(soporte.canal())) {
      throw new IllegalStateException("El perfil prod exige un MOICA_SOPORTE_CANAL atendido.");
    }
  }
}
