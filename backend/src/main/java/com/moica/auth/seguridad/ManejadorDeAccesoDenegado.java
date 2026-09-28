package com.moica.auth.seguridad;

import com.moica.comun.error.EscritorDeRespuestaDeError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Responde 403 cuando la petición llega identificada pero no se le permite hacer lo que pide.
 *
 * <p>Atiende todas las denegaciones de la cadena: una operación mutable sin su token CSRF, una
 * sesión provisional o de una cuenta suspendida fuera de lo poco que se le permite, y {@code
 * /api/admin/**} sin rol o sin el segundo factor verificado. El mensaje sugiere recargar porque el
 * único caso que un recargo resuelve es el del CSRF, el más frecuente; los demás los distingue el
 * frontend por el código y el estado de su sesión, no por este texto.
 */
@Component
public class ManejadorDeAccesoDenegado implements AccessDeniedHandler {

  private final EscritorDeRespuestaDeError escritor;

  public ManejadorDeAccesoDenegado(EscritorDeRespuestaDeError escritor) {
    this.escritor = escritor;
  }

  @Override
  public void handle(
      HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException excepcion)
      throws IOException {

    escritor.escribir(
        peticion,
        respuesta,
        HttpStatus.FORBIDDEN,
        "ACCESO_DENEGADO",
        "No pudimos completar la operación. Recarga la página e inténtalo otra vez.");
  }
}
