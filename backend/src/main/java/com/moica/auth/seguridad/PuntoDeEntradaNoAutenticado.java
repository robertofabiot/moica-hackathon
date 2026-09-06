package com.moica.auth.seguridad;

import com.moica.comun.error.EscritorDeRespuestaDeError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Responde 401 cuando la petición necesitaba una sesión vigente y no la tenía.
 *
 * <p>Es el mismo 401 sin cookie, con un token inválido, con una sesión expirada y con una sesión
 * revocada: hacia fuera no se distingue por qué falló.
 */
@Component
public class PuntoDeEntradaNoAutenticado implements AuthenticationEntryPoint {

  private final EscritorDeRespuestaDeError escritor;

  public PuntoDeEntradaNoAutenticado(EscritorDeRespuestaDeError escritor) {
    this.escritor = escritor;
  }

  @Override
  public void commence(
      HttpServletRequest peticion, HttpServletResponse respuesta, AuthenticationException excepcion)
      throws IOException {

    escritor.escribir(
        peticion,
        respuesta,
        HttpStatus.UNAUTHORIZED,
        "NO_AUTENTICADO",
        "Tu sesión no está activa. Inicia sesión para continuar.");
  }
}
