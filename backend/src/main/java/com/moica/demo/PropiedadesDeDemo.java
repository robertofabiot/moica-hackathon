package com.moica.demo;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("moica.seed.demo")
public record PropiedadesDeDemo(boolean enabled, Map<String, List<String>> imagenes) {
  public PropiedadesDeDemo {
    imagenes =
        imagenes == null
            ? Map.of()
            : imagenes.entrySet().stream()
                .collect(
                    java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entrada -> List.copyOf(entrada.getValue())));
  }

  public List<String> clavesDe(String servicio) {
    return imagenes.getOrDefault(servicio, List.of()).stream()
        .map(String::strip)
        .filter(clave -> !clave.isEmpty())
        .toList();
  }

  @Override
  public String toString() {
    return "PropiedadesDeDemo[enabled=" + enabled + ", imagenes=(mapeo omitido)]";
  }
}
