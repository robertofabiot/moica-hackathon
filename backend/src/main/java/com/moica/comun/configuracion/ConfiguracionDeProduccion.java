package com.moica.comun.configuracion;

import com.moica.auth.seguridad.PropiedadesDeSeguridad;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Impide publicar por accidente la configuracion de desarrollo.
 *
 * <p>Es {@code final} y sin proxy de metodos {@code @Bean} porque valida lanzando desde el
 * constructor: una clase heredable que aborta a medio construir queda expuesta a un ataque por
 * finalizador. No declara ningun {@code @Bean}, asi que renunciar al proxy CGLIB no cambia nada.
 */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public final class ConfiguracionDeProduccion {

  public ConfiguracionDeProduccion(PropiedadesDeSeguridad seguridad, PropiedadesDeSoporte soporte) {
    if (!seguridad.cookieSegura()) {
      throw new IllegalStateException("El perfil prod exige MOICA_COOKIE_SEGURA=true.");
    }
    if ("soporte@moica.ni".equalsIgnoreCase(soporte.canal())) {
      throw new IllegalStateException("El perfil prod exige un MOICA_SOPORTE_CANAL atendido.");
    }
  }
}
