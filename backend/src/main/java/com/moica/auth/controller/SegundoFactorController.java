package com.moica.auth.controller;

import com.moica.auth.dto.ActivacionDeSegundoFactor;
import com.moica.auth.dto.RespuestaDeSegundoFactor;
import com.moica.auth.dto.SolicitudDeCodigoTotp;
import com.moica.auth.dto.SolicitudDeDesactivacionDeSegundoFactor;
import com.moica.auth.seguridad.CookieDeSesion;
import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.auth.service.SegundoFactorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El segundo factor de quien está autenticado.
 *
 * <p>Todas estas operaciones exigen una sesión plena: quien todavía no ha superado el segundo
 * factor no puede reconfigurarlo. Ninguna acepta un identificador de cuenta como parámetro; la
 * cuenta es siempre la de la sesión, de modo que no existe forma de tocar el segundo factor ajeno.
 */
@RestController
@RequestMapping("/api/auth/segundo-factor")
public class SegundoFactorController {

  private final SegundoFactorService servicio;
  private final CookieDeSesion cookie;

  public SegundoFactorController(SegundoFactorService servicio, CookieDeSesion cookie) {
    this.servicio = servicio;
    this.cookie = cookie;
  }

  /** Estado del segundo factor de la cuenta. Nunca devuelve el secreto. */
  @GetMapping
  public RespuestaDeSegundoFactor consultar(@AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return servicio.consultar(sujeto.idUsuario());
  }

  /**
   * Empieza la activación y entrega el secreto una única vez.
   *
   * <p>Es la respuesta sensible del ciclo: contiene la clave manual y la URI {@code otpauth://}. En
   * cuanto el segundo factor queda activo, ese valor deja de poder recuperarse.
   */
  @PostMapping
  public ActivacionDeSegundoFactor iniciarActivacion(
      @AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return servicio.iniciarActivacion(sujeto.idUsuario());
  }

  /** Confirma la activación con el primer código válido y verifica esta sesión. */
  @PostMapping("/activacion")
  public RespuestaDeSegundoFactor confirmarActivacion(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDeCodigoTotp solicitud) {

    return servicio.confirmarActivacion(sujeto.idUsuario(), sujeto.idSesion(), solicitud.codigo());
  }

  /**
   * Desactiva el segundo factor y cierra todas las sesiones de la cuenta.
   *
   * <p>Se trata como un cambio de credenciales, así que la respuesta caduca la cookie igual que el
   * cambio de contraseña.
   */
  @PostMapping("/desactivacion")
  public ResponseEntity<Void> desactivar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDeDesactivacionDeSegundoFactor solicitud) {

    servicio.desactivar(sujeto.idUsuario(), solicitud.claveActual(), solicitud.codigo());

    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookie.caducada().toString())
        .build();
  }
}
