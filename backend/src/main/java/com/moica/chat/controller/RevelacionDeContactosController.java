package com.moica.chat.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.chat.service.RevelacionDeContactosService;
import com.moica.prestador.dto.ContactoRevelado;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los contactos externos del prestador, revelados al cliente de una solicitud aceptada.
 *
 * <p>Es de solo lectura y cuelga de la solicitud, no del prestador: no existe ninguna ruta para
 * consultar los contactos de un prestador cualquiera.
 */
@RestController
@RequestMapping("/api/solicitudes/{idSolicitudServicio}")
public class RevelacionDeContactosController {

  private final RevelacionDeContactosService revelacion;

  public RevelacionDeContactosController(RevelacionDeContactosService revelacion) {
    this.revelacion = revelacion;
  }

  @GetMapping("/contactos")
  public List<ContactoRevelado> contactos(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idSolicitudServicio) {
    return revelacion.revelar(sujeto, idSolicitudServicio);
  }
}
