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
 * <p>Es lectura pública desde P5: el filtro territorial del descubrimiento no exige sesión. Las
 * escrituras de catálogo no existen; este controlador solo publica departamentos habilitados.
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
