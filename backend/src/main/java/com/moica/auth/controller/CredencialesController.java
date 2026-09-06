package com.moica.auth.controller;

import com.moica.auth.dto.SolicitudDeCambioDeClave;
import com.moica.auth.seguridad.CookieDeSesion;
import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.auth.service.AutenticacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** La contraseña de quien está autenticado. */
@RestController
@RequestMapping("/api/auth/clave")
public class CredencialesController {

  private final AutenticacionService servicio;
  private final CookieDeSesion cookie;

  public CredencialesController(AutenticacionService servicio, CookieDeSesion cookie) {
    this.servicio = servicio;
    this.cookie = cookie;
  }

  /**
   * Cambia la contraseña y cierra todas las sesiones de la cuenta.
   *
   * <p>La respuesta caduca la cookie porque la sesión desde la que se hizo el cambio también queda
   * revocada: después de cambiar la contraseña hay que volver a iniciar sesión, aquí y en cualquier
   * otro dispositivo.
   */
  @PutMapping
  public ResponseEntity<Void> cambiar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDeCambioDeClave solicitud) {

    servicio.cambiarClave(sujeto, solicitud);

    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookie.caducada().toString())
        .build();
  }
}
