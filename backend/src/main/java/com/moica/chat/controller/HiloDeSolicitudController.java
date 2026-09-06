package com.moica.chat.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.chat.dto.DatosDeMensajeSolicitud;
import com.moica.chat.dto.MensajeAEnviar;
import com.moica.chat.service.HiloDeSolicitudService;
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
 * El hilo de mensajes de una solicitud de servicio.
 *
 * <p>Vive aparte de {@code SolicitudServicioController} porque son dos responsabilidades: aquel
 * atiende el ciclo del compromiso y este la conversación que ese compromiso habilita.
 *
 * <p>Solo hay leer y escribir. No existe {@code PUT}, {@code PATCH} ni {@code DELETE}: el MVP no
 * admite editar ni borrar mensajes.
 */
@RestController
@RequestMapping("/api/solicitudes/{idSolicitudServicio}")
public class HiloDeSolicitudController {

  private final HiloDeSolicitudService hilo;

  public HiloDeSolicitudController(HiloDeSolicitudService hilo) {
    this.hilo = hilo;
  }

  /** El hilo completo en orden cronológico, para cualquiera de los dos participantes. */
  @GetMapping("/mensajes")
  public List<DatosDeMensajeSolicitud> mensajes(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idSolicitudServicio) {
    return hilo.listarMensajes(sujeto, idSolicitudServicio);
  }

  /** Agrega un mensaje. El remitente sale de la sesión, nunca del cuerpo. */
  @PostMapping("/mensajes")
  public ResponseEntity<DatosDeMensajeSolicitud> enviar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idSolicitudServicio,
      @Valid @RequestBody MensajeAEnviar pedido) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(hilo.enviarMensaje(sujeto, idSolicitudServicio, pedido));
  }
}
