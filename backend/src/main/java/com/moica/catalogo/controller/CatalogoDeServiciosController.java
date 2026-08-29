package com.moica.catalogo.controller;

import com.moica.catalogo.dto.DatosDeCategoriaServicio;
import com.moica.catalogo.service.CatalogoDeServiciosService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo de categorías y subcategorías de servicio.
 *
 * <p>Es lectura pública: lo necesitan el descubrimiento y el formulario de publicación. La
 * taxonomía de demostración no se presenta como exhaustiva.
 */
@RestController
@RequestMapping("/api/catalogos")
public class CatalogoDeServiciosController {

  private final CatalogoDeServiciosService servicio;

  public CatalogoDeServiciosController(CatalogoDeServiciosService servicio) {
    this.servicio = servicio;
  }

  /** Categorías con sus subcategorías, listas para un filtro o un selector. */
  @GetMapping("/categorias")
  public List<DatosDeCategoriaServicio> categorias() {
    return servicio.categorias();
  }
}
