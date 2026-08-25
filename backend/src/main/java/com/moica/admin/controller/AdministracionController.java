package com.moica.admin.controller;

import com.moica.admin.dto.ResumenAdministrativo;
import com.moica.admin.service.AdministracionService;
import com.moica.auth.seguridad.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Área administrativa de Moica.
 *
 * <p>Todo lo que cuelga de {@code /api/admin} exige a la vez rol administrativo y segundo factor
 * verificado en esa sesión; lo impone la cadena de seguridad, no cada método. En P3 hay un único
 * endpoint, el mínimo para demostrar esa protección.
 */
@RestController
@RequestMapping("/api/admin")
public class AdministracionController {

  private final AdministracionService servicio;

  public AdministracionController(AdministracionService servicio) {
    this.servicio = servicio;
  }

  /** Describe la sesión administrativa en curso. */
  @GetMapping("/resumen")
  public ResumenAdministrativo resumen(@AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return servicio.resumirPara(sujeto.idUsuario());
  }
}
