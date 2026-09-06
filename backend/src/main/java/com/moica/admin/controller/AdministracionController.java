package com.moica.admin.controller;

import com.moica.admin.dto.DatosDeAdministrador;
import com.moica.admin.dto.ResumenAdministrativo;
import com.moica.admin.service.AdministracionService;
import com.moica.auth.seguridad.UsuarioAutenticado;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Área administrativa de Moica.
 *
 * <p>Todo lo que cuelga de {@code /api/admin} exige a la vez rol administrativo y segundo factor
 * verificado en esa sesión; lo impone la cadena de seguridad, no cada método. Aquí vive lo
 * transversal del área: con qué cuenta se entró y quiénes más pueden recibir trabajo. Cada
 * capacidad administrativa publica sus propias rutas bajo el mismo prefijo.
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

  /**
   * Las personas administradoras entre las que puede repartirse el trabajo.
   *
   * <p>La consume la reasignación de casos de moderación. Solo devuelve identificador y nombre: es
   * un desplegable para elegir a quién pasar un expediente, no un directorio de cuentas.
   */
  @GetMapping("/administradores")
  public List<DatosDeAdministrador> administradores() {
    return servicio.listarAdministradores();
  }
}
