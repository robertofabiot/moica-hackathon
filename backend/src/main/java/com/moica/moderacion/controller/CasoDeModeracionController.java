package com.moica.moderacion.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.moderacion.dto.DatosDeCasoModeracion;
import com.moica.moderacion.dto.EstadoDeReporte;
import com.moica.moderacion.dto.ReporteAPresentar;
import com.moica.moderacion.service.CasoModeracionService;
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
 * El reporte de un participante y el caso que abre, dentro de su solicitud.
 *
 * <p>Cuelga de la solicitud, como el hilo de mensajes, la revelación de contactos y la
 * calificación: no existe una ruta para reportar a una persona cualquiera. El recurso es singular
 * —{@code caso-moderacion} y no {@code casos-moderacion}— porque cada participante abre como máximo
 * uno por solicitud, igual que {@code calificacion}.
 *
 * <p>Solo hay consultar y crear. No existe {@code PUT}, {@code PATCH} ni {@code DELETE}: en el MVP
 * un reporte no se edita ni se retira. Y lo que se consulta es siempre el caso propio: la bandeja
 * administrativa, el expediente completo, el historial y las resoluciones son otra superficie,
 * {@code /api/admin/casos}, que exige rol y segundo factor verificado.
 */
@RestController
@RequestMapping("/api/solicitudes/{idSolicitudServicio}")
public class CasoDeModeracionController {

  private final CasoModeracionService casos;

  public CasoDeModeracionController(CasoModeracionService casos) {
    this.casos = casos;
  }

  /** A quién puede reportar la sesión, si la solicitud lo admite y qué caso abrió, si abrió uno. */
  @GetMapping("/caso-moderacion")
  public EstadoDeReporte consultar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idSolicitudServicio) {
    return casos.consultar(sujeto, idSolicitudServicio);
  }

  /** Abre el caso. El reportado lo pone el servidor a partir de la solicitud, no el cuerpo. */
  @PostMapping("/caso-moderacion")
  public ResponseEntity<DatosDeCasoModeracion> reportar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idSolicitudServicio,
      @Valid @RequestBody ReporteAPresentar pedido) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(casos.reportar(sujeto, idSolicitudServicio, pedido));
  }
}
