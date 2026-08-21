package com.moica.auth.controller;

import com.moica.auth.dto.RespuestaDeSesion;
import com.moica.auth.dto.SolicitudDeInicioSesion;
import com.moica.auth.seguridad.CookieDeSesion;
import com.moica.auth.service.AutenticacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La sesión de quien usa Moica, como un único recurso.
 *
 * <p>Crearla es iniciar sesión.
 */
@RestController
@RequestMapping("/api/auth/sesion")
public class SesionController {

  private final AutenticacionService servicio;
  private final CookieDeSesion cookie;

  public SesionController(AutenticacionService servicio, CookieDeSesion cookie) {
    this.servicio = servicio;
    this.cookie = cookie;
  }

  /** Inicia sesión: crea la fila {@code sesion} y entrega su JWT en la cookie. */
  @PostMapping
  public ResponseEntity<RespuestaDeSesion> iniciar(
      @Valid @RequestBody SolicitudDeInicioSesion solicitud) {

    AutenticacionService.SesionIniciada iniciada = servicio.iniciarSesion(solicitud);

    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, cookie.conToken(iniciada.token()).toString())
        .body(iniciada.respuesta());
  }
}
