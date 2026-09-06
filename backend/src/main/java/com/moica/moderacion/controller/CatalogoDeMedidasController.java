package com.moica.moderacion.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.moderacion.dto.DatosDeMedidaAdministrativa;
import com.moica.moderacion.dto.HabilitacionDeMedida;
import com.moica.moderacion.dto.MedidaACrear;
import com.moica.moderacion.dto.MedidaAEditar;
import com.moica.moderacion.service.CatalogoDeMedidasService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El catálogo de medidas administrativas.
 *
 * <p>Cuelga de {@code /api/admin}, así que hereda sus dos condiciones simultáneas: rol
 * administrativo y segundo factor verificado en <b>esa</b> sesión. Las impone la cadena de
 * seguridad, no cada método, y el servicio las repite como última red.
 *
 * <p><b>No hay {@code DELETE}, y no es un olvido.</b> Una medida citada por un caso o por una
 * versión del historial es la evidencia de una decisión, y borrarla dejaría expedientes hablando de
 * una sanción que ya no existe. Lo que el negocio llama «eliminar» es deshabilitarla, y para eso
 * está la habilitación: la medida deja de ofrecerse y sigue describiendo lo que ya pasó.
 *
 * <p>La habilitación tiene recurso propio en lugar de ser un campo más de la edición porque es una
 * decisión de otra naturaleza: retirar una sanción del catálogo no debe colarse dentro de un
 * formulario que también corrige una descripción.
 */
@RestController
@RequestMapping("/api/admin/medidas")
public class CatalogoDeMedidasController {

  private final CatalogoDeMedidasService servicio;

  public CatalogoDeMedidasController(CatalogoDeMedidasService servicio) {
    this.servicio = servicio;
  }

  /** El catálogo entero, de la medida más leve a la más grave, habilitadas y deshabilitadas. */
  @GetMapping
  public List<DatosDeMedidaAdministrativa> consultarCatalogo(
      @AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return servicio.consultarCatalogo(sujeto);
  }

  /** Añade una medida al catálogo. */
  @PostMapping
  public ResponseEntity<DatosDeMedidaAdministrativa> crear(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @Valid @RequestBody MedidaACrear pedido) {
    return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(sujeto, pedido));
  }

  /** Reescribe una medida. El código no viaja: identifica decisiones ya tomadas y no cambia. */
  @PutMapping("/{idMedidaAdministrativa}")
  public DatosDeMedidaAdministrativa editar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Short idMedidaAdministrativa,
      @Valid @RequestBody MedidaAEditar pedido) {
    return servicio.editar(sujeto, idMedidaAdministrativa, pedido);
  }

  /** Habilita o deshabilita la medida para aplicaciones nuevas, sin tocar su historia. */
  @PutMapping("/{idMedidaAdministrativa}/habilitacion")
  public DatosDeMedidaAdministrativa cambiarHabilitacion(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Short idMedidaAdministrativa,
      @Valid @RequestBody HabilitacionDeMedida pedido) {
    return servicio.cambiarHabilitacion(sujeto, idMedidaAdministrativa, pedido.habilitada());
  }
}
