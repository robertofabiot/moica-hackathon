package com.moica.catalogo.controller;

import com.moica.catalogo.dto.DatosDeDepartamento;
import com.moica.catalogo.service.CatalogoTerritorialService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogos territoriales que consumen los formularios.
 *
 * <p>Exige sesión plena como cualquier ruta no declarada: hoy solo lo necesita quien crea o edita
 * su perfil de prestador. Cuando llegue el descubrimiento público (P5) se decidirá si se abre.
 */
@RestController
@RequestMapping("/api/catalogos")
public class CatalogoTerritorialController {

  private final CatalogoTerritorialService servicio;

  public CatalogoTerritorialController(CatalogoTerritorialService servicio) {
    this.servicio = servicio;
  }

  /** Departamentos habilitados con sus municipios, listos para un selector. */
  @GetMapping("/departamentos")
  public List<DatosDeDepartamento> departamentos() {
    return servicio.departamentosHabilitados();
  }
}
