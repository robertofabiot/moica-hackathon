package com.moica.comun.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

/**
 * Escribe un {@link RespuestaDeError} directamente sobre la respuesta HTTP.
 *
 * <p>Hace falta porque la cadena de filtros de seguridad rechaza peticiones antes de llegar al
 * controlador, así que esos errores no pasan por {@link ManejadorGlobalDeErrores}. Con este
 * componente el cuerpo es el mismo se rechace donde se rechace.
 */
@Component
public class EscritorDeRespuestaDeError {

  private final ObjectWriter escritor;

  public EscritorDeRespuestaDeError(ObjectMapper mapeador) {
    // Se guarda un `ObjectWriter`, que es inmutable, en lugar del `ObjectMapper`
    // compartido de la aplicación.
    this.escritor = mapeador.writerFor(RespuestaDeError.class);
  }

  public void escribir(
      HttpServletRequest peticion,
      HttpServletResponse respuesta,
      HttpStatus estado,
      String codigo,
      String mensaje)
      throws IOException {

    RespuestaDeError cuerpo =
        RespuestaDeError.de(estado.value(), codigo, mensaje, peticion.getRequestURI());

    respuesta.setStatus(estado.value());
    respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
    respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
    escritor.writeValue(respuesta.getOutputStream(), cuerpo);
  }
}
