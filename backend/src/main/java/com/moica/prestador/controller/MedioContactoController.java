package com.moica.prestador.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.dto.SolicitudDeOrden;
import com.moica.prestador.dto.DatosDeMedioContacto;
import com.moica.prestador.dto.SolicitudDeMedioContacto;
import com.moica.prestador.service.MedioContactoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los medios de contacto del prestador de la sesión.
 *
 * <p>Aquí solo los ve y los administra su propietario. A un cliente se le revelan por {@code GET
 * /api/solicitudes/{id}/contactos} cuando el prestador acepta su solicitud; esa es otra superficie,
 * con su propia autorización, y estas rutas no cambian.
 */
@RestController
@RequestMapping("/api/prestador/contactos")
public class MedioContactoController {

  private final MedioContactoService servicio;

  public MedioContactoController(MedioContactoService servicio) {
    this.servicio = servicio;
  }

  /** Los contactos propios en su orden de visualización. */
  @GetMapping
  public List<DatosDeMedioContacto> listar(@AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return servicio.listar(sujeto);
  }

  /** Agrega un contacto al final de la lista. */
  @PostMapping
  public ResponseEntity<DatosDeMedioContacto> crear(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDeMedioContacto solicitud) {
    return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(sujeto, solicitud));
  }

  /**
   * Deja los contactos en el orden pedido, con la lista completa de identificadores.
   *
   * <p>Declarada antes que {@code /{id}} en la lectura, pero sin ambigüedad real: {@code orden} no
   * es un número y Spring resuelve la ruta literal primero.
   */
  @PutMapping("/orden")
  public List<DatosDeMedioContacto> reordenar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDeOrden orden) {
    return servicio.reordenar(sujeto, orden);
  }

  /** Sustituye el contenido de un contacto propio. */
  @PutMapping("/{idMedioContacto}")
  public DatosDeMedioContacto actualizar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idMedioContacto,
      @Valid @RequestBody SolicitudDeMedioContacto solicitud) {
    return servicio.actualizar(sujeto, idMedioContacto, solicitud);
  }

  /** Elimina un contacto propio. */
  @DeleteMapping("/{idMedioContacto}")
  public ResponseEntity<Void> eliminar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idMedioContacto) {
    servicio.eliminar(sujeto, idMedioContacto);
    return ResponseEntity.noContent().build();
  }
}
