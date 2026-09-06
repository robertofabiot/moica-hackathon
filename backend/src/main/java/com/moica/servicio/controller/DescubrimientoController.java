package com.moica.servicio.controller;

import com.moica.servicio.dto.DetallePublicoDeServicio;
import com.moica.servicio.dto.PerfilPublicoDePrestador;
import com.moica.servicio.dto.ResumenPublicoDeServicio;
import com.moica.servicio.service.DescubrimientoDeServiciosService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Descubrimiento público de servicios y prestadores.
 *
 * <p>No exige sesión. Los matchers de seguridad abren únicamente estos {@code GET}. Un servicio o
 * perfil que no deba verse responde 404.
 */
@RestController
public class DescubrimientoController {

  private final DescubrimientoDeServiciosService descubrimiento;

  public DescubrimientoController(DescubrimientoDeServiciosService descubrimiento) {
    this.descubrimiento = descubrimiento;
  }

  @GetMapping("/api/servicios")
  public List<ResumenPublicoDeServicio> buscar(
      @RequestParam(name = "texto", required = false) String texto,
      @RequestParam(name = "idCategoria", required = false) Short idCategoria,
      @RequestParam(name = "idSubcategoria", required = false) Integer idSubcategoria,
      @RequestParam(name = "idMunicipio", required = false) Integer idMunicipio) {
    return descubrimiento.buscar(texto, idCategoria, idSubcategoria, idMunicipio);
  }

  @GetMapping("/api/servicios/{idServicio}")
  public DetallePublicoDeServicio detallar(@PathVariable Long idServicio) {
    return descubrimiento.detallar(idServicio);
  }

  @GetMapping("/api/prestadores/{idPrestador}")
  public PerfilPublicoDePrestador perfil(@PathVariable Long idPrestador) {
    return descubrimiento.perfilPublico(idPrestador);
  }
}
