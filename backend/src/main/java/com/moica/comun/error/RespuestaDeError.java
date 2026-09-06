package com.moica.comun.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Forma única de todos los errores que devuelve la API.
 *
 * <p>El cuerpo describe qué pasó y qué hacer. Nunca lleva trazas, SQL, nombres de clase, valores
 * internos ni datos de la persona que hizo la petición.
 *
 * @param instante momento en que se produjo el error
 * @param estado código de estado HTTP, repetido en el cuerpo para que el cliente no dependa solo de
 *     la cabecera
 * @param codigo identificador estable del error, pensado para que el cliente decida qué mostrar
 * @param mensaje explicación para la persona, en español
 * @param ruta camino de la petición que falló
 * @param errores detalle por campo cuando el error es de validación; ausente en el resto de casos
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaDeError(
    OffsetDateTime instante,
    int estado,
    String codigo,
    String mensaje,
    String ruta,
    List<ErrorDeCampo> errores) {

  /** Motivo por el que un campo concreto de la petición no se admitió. */
  public record ErrorDeCampo(String campo, String mensaje) {}

  public RespuestaDeError {
    // Copia defensiva: la respuesta se comparte y no debe poder cambiar después
    // de construirse.
    errores = (errores == null) ? null : List.copyOf(errores);
  }

  /** Construye un error sin detalle por campo. */
  public static RespuestaDeError de(int estado, String codigo, String mensaje, String ruta) {
    return new RespuestaDeError(OffsetDateTime.now(), estado, codigo, mensaje, ruta, null);
  }
}
