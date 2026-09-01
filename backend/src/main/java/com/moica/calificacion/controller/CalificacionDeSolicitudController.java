package com.moica.calificacion.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.calificacion.dto.CalificacionAEmitir;
import com.moica.calificacion.dto.DatosDeCalificacion;
import com.moica.calificacion.dto.EstadoDeCalificacion;
import com.moica.calificacion.dto.ReputacionPorRol;
import com.moica.calificacion.service.CalificacionDeSolicitudService;
import jakarta.validation.Valid;
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
 * La calificación de una solicitud completada y la reputación que solo se ve desde ella.
 *
 * <p>Cuelga de la solicitud, como el hilo de mensajes y la revelación de contactos: no existe una
 * ruta para calificar a una persona cualquiera, ni para consultar la reputación como cliente fuera
 * de una solicitud propia. La reputación pública del prestador no vive aquí; viaja en las
 * superficies de descubrimiento que ya la muestran.
 *
 * <p>Solo hay consultar y crear. No existe {@code PUT}, {@code PATCH} ni {@code DELETE}: en el MVP
 * una calificación no se edita ni se borra.
 */
@RestController
@RequestMapping("/api/solicitudes/{idSolicitudServicio}")
public class CalificacionDeSolicitudController {

  private final CalificacionDeSolicitudService calificaciones;

  public CalificacionDeSolicitudController(CalificacionDeSolicitudService calificaciones) {
    this.calificaciones = calificaciones;
  }

  /** A quién califica la sesión, en qué rol, si todavía puede y qué escribió si ya calificó. */
  @GetMapping("/calificacion")
  public EstadoDeCalificacion consultar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idSolicitudServicio) {
    return calificaciones.consultar(sujeto, idSolicitudServicio);
  }

  /** Registra la calificación. El calificado y su rol los pone el servidor, no el cuerpo. */
  @PostMapping("/calificacion")
  public ResponseEntity<DatosDeCalificacion> calificar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idSolicitudServicio,
      @Valid @RequestBody CalificacionAEmitir pedido) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(calificaciones.calificar(sujeto, idSolicitudServicio, pedido));
  }

  /** La reputación como cliente de quien contrató, visible solo para el prestador participante. */
  @GetMapping("/reputacion-del-cliente")
  public ReputacionPorRol reputacionDelCliente(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idSolicitudServicio) {
    return calificaciones.reputacionDelCliente(sujeto, idSolicitudServicio);
  }
}
