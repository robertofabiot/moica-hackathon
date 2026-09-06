package com.moica.solicitud.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.solicitud.dto.DatosDeSolicitudServicio;
import com.moica.solicitud.dto.ResumenDeSolicitudServicio;
import com.moica.solicitud.dto.SolicitudDeCancelacion;
import com.moica.solicitud.dto.SolicitudDeContratacion;
import com.moica.solicitud.service.SolicitudServicioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ciclo de las solicitudes de servicio de la sesión.
 *
 * <p>Las acciones son explícitas —aceptar, rechazar, cancelar, completar— y no un cambio genérico
 * de estado. Un recurso ajeno responde 404. No hay borrado.
 */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudServicioController {

  private final SolicitudServicioService solicitudes;

  public SolicitudServicioController(SolicitudServicioService solicitudes) {
    this.solicitudes = solicitudes;
  }

  @PostMapping
  public ResponseEntity<DatosDeSolicitudServicio> crear(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDeContratacion pedido) {
    return ResponseEntity.status(HttpStatus.CREATED).body(solicitudes.crear(sujeto, pedido));
  }

  @GetMapping("/enviadas")
  public List<ResumenDeSolicitudServicio> enviadas(
      @AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return solicitudes.enviadas(sujeto);
  }

  @GetMapping("/recibidas")
  public List<ResumenDeSolicitudServicio> recibidas(
      @AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return solicitudes.recibidas(sujeto);
  }

  @GetMapping("/{idSolicitudServicio}")
  public DatosDeSolicitudServicio consultar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idSolicitudServicio) {
    return solicitudes.consultar(sujeto, idSolicitudServicio);
  }

  @PostMapping("/{idSolicitudServicio}/aceptacion")
  public DatosDeSolicitudServicio aceptar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idSolicitudServicio) {
    return solicitudes.aceptar(sujeto, idSolicitudServicio);
  }

  @PostMapping("/{idSolicitudServicio}/rechazo")
  public DatosDeSolicitudServicio rechazar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idSolicitudServicio) {
    return solicitudes.rechazar(sujeto, idSolicitudServicio);
  }

  @PostMapping("/{idSolicitudServicio}/cancelacion")
  public DatosDeSolicitudServicio cancelar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idSolicitudServicio,
      @Valid @RequestBody(required = false) SolicitudDeCancelacion pedido) {
    return solicitudes.cancelar(sujeto, idSolicitudServicio, pedido);
  }

  @PostMapping("/{idSolicitudServicio}/completado")
  public DatosDeSolicitudServicio completar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idSolicitudServicio) {
    return solicitudes.completar(sujeto, idSolicitudServicio);
  }
}
