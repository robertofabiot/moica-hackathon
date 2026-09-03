package com.moica.moderacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lo que un participante envía para reportar a la contraparte.
 *
 * <p>Solo lleva el motivo y la descripción. Ni el reportado ni el reportante viajan en el cuerpo:
 * el servicio los deriva de la sesión y de la solicitud, de modo que reportar a alguien con quien
 * no se tiene una solicitud aceptada no es una petición que se pueda formular.
 *
 * <p>Los dos textos se recortan antes de validarse, así que uno formado solo por espacios se
 * rechaza como vacío en lugar de guardarse en blanco.
 *
 * @param motivo clasificación breve del hecho reportado; el tope de {@value #MAXIMO_MOTIVO}
 *     caracteres es el de la columna {@code varchar(120)} del diccionario
 * @param descripcion explicación de los hechos. El diccionario la modela como {@code TEXT} sin
 *     máximo; el tope de {@value #MAXIMO_DESCRIPCION} caracteres es un límite de la aplicación,
 *     documentado en el contrato y el mismo que ya usa la descripción de una solicitud, que es el
 *     otro texto largo obligatorio de Moica
 */
public record ReporteAPresentar(
    @NotBlank @Size(max = ReporteAPresentar.MAXIMO_MOTIVO) String motivo,
    @NotBlank @Size(max = ReporteAPresentar.MAXIMO_DESCRIPCION) String descripcion) {

  public static final int MAXIMO_MOTIVO = 120;
  public static final int MAXIMO_DESCRIPCION = 3000;

  public ReporteAPresentar {
    motivo = normalizar(motivo);
    descripcion = normalizar(descripcion);
  }

  private static String normalizar(String texto) {
    return (texto == null) ? null : texto.strip();
  }
}
