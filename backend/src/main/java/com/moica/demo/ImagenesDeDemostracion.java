package com.moica.demo;

import java.util.List;

/**
 * Solo objetos públicos inspeccionados; nunca trasladar sus claves a otro bucket por suposición.
 */
public final class ImagenesDeDemostracion {
  public static final String BASE_COMPROBADA =
      "https://pub-a1129fb1410e4c84b7ac69e79d442ace.r2.dev";
  public static final String LAPTOP = "servicios/1ae971b72b92480d9a84822b145786cb.jpg";
  public static final String PROCESADOR = "servicios/addb04d5b1044a3fbd59eb45e15768ce.jpg";

  private ImagenesDeDemostracion() {}

  public static List<String> existentes(String servicio, String basePublica) {
    return "computadoras".equals(servicio) && BASE_COMPROBADA.equals(basePublica)
        ? List.of(LAPTOP, PROCESADOR)
        : List.of();
  }

  public static String textoAlternativo(String clave, String servicio, int orden) {
    return switch (clave) {
      case LAPTOP ->
          "Aplicación de pasta térmica sobre un procesador de laptop durante su mantenimiento";
      case PROCESADOR ->
          "Aplicación de pasta térmica en un procesador de computadora de escritorio";
      default -> servicio + " — imagen ilustrativa " + (orden + 1);
    };
  }
}
