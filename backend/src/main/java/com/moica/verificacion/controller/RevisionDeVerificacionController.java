package com.moica.verificacion.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.verificacion.dto.DatosDeExpediente;
import com.moica.verificacion.dto.SolicitudDeResolucion;
import com.moica.verificacion.entity.EstadoSolicitudVerificacion;
import com.moica.verificacion.entity.NivelVerificacionSolicitado;
import com.moica.verificacion.service.RevisionDeVerificacionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La revisión de expedientes dentro del área administrativa.
 *
 * <p>Cuelga de {@code /api/admin}, así que hereda sus dos condiciones simultáneas: rol
 * administrativo y segundo factor verificado en **esa** sesión. Las impone la cadena de seguridad,
 * no cada método.
 *
 * <p>Cada acción es un recurso propio —{@code /toma}, {@code /aprobacion}, {@code /rechazo}, {@code
 * /revocacion}— en lugar de un campo de estado que se sobrescribe: así una transición inválida no
 * es un valor raro en un cuerpo sino una operación que la solicitud no admite, y el 409 explica
 * cuál es su estado real.
 */
@RestController
@RequestMapping("/api/admin/verificaciones")
public class RevisionDeVerificacionController {

  private final RevisionDeVerificacionService servicio;

  public RevisionDeVerificacionController(RevisionDeVerificacionService servicio) {
    this.servicio = servicio;
  }

  /**
   * La cola de revisión.
   *
   * <p>Sin parámetros devuelve lo que espera decisión: {@code PENDIENTE} y {@code EN_REVISION}. Con
   * {@code estado} se piden otros —{@code APROBADA}, por ejemplo, que es de donde se revoca— y con
   * {@code nivel} se acota a básicas o profesionales.
   */
  @GetMapping
  public List<DatosDeExpediente> consultarCola(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @RequestParam(name = "estado", required = false) List<EstadoSolicitudVerificacion> estados,
      @RequestParam(name = "nivel", required = false) NivelVerificacionSolicitado nivel) {
    return servicio.consultarCola(sujeto, estados, nivel);
  }

  /** El detalle de un expediente con los metadatos de sus documentos. */
  @GetMapping("/{idSolicitudVerificacion}")
  public DatosDeExpediente consultarExpediente(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idSolicitudVerificacion) {
    return servicio.consultarExpediente(sujeto, idSolicitudVerificacion);
  }

  /** Toma una solicitud pendiente; a partir de aquí solo quien la tomó puede resolverla. */
  @PostMapping("/{idSolicitudVerificacion}/toma")
  public DatosDeExpediente tomar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idSolicitudVerificacion) {
    return servicio.tomar(sujeto, idSolicitudVerificacion);
  }

  /** Aprueba la solicitud y proyecta el nivel correspondiente en el perfil. */
  @PostMapping("/{idSolicitudVerificacion}/aprobacion")
  public DatosDeExpediente aprobar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idSolicitudVerificacion) {
    return servicio.aprobar(sujeto, idSolicitudVerificacion);
  }

  /** Rechaza la solicitud. El motivo es obligatorio y lo verá el prestador. */
  @PostMapping("/{idSolicitudVerificacion}/rechazo")
  public DatosDeExpediente rechazar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idSolicitudVerificacion,
      @Valid @RequestBody SolicitudDeResolucion resolucion) {
    return servicio.rechazar(sujeto, idSolicitudVerificacion, resolucion.observacion());
  }

  /** Revoca una verificación ya concedida. El motivo es obligatorio. */
  @PostMapping("/{idSolicitudVerificacion}/revocacion")
  public DatosDeExpediente revocar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idSolicitudVerificacion,
      @Valid @RequestBody SolicitudDeResolucion resolucion) {
    return servicio.revocar(sujeto, idSolicitudVerificacion, resolucion.observacion());
  }

  /**
   * Abre un documento del expediente mediante un acceso temporal.
   *
   * <p>Responde 302 hacia una URL prefirmada de vida corta en lugar de devolverla en un cuerpo
   * JSON. La diferencia importa: así la dirección firmada no pasa por el JavaScript de la
   * aplicación, no entra en la caché de consultas y no queda escrita en ninguna respuesta que
   * alguien pueda copiar sin darse cuenta. El navegador la sigue y la olvida.
   *
   * <p>La autorización se comprueba en **cada** petición, no al abrir el expediente: el enlace no
   * es un permiso permanente, y cuando la URL caduca deja de servir para nadie.
   */
  @GetMapping("/{idSolicitudVerificacion}/documentos/{idDocumentoVerificacion}/acceso")
  public ResponseEntity<Void> abrirDocumento(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idSolicitudVerificacion,
      @PathVariable Long idDocumentoVerificacion) {

    URI accesoTemporal =
        servicio.abrirDocumento(sujeto, idSolicitudVerificacion, idDocumentoVerificacion);

    return ResponseEntity.status(HttpStatus.FOUND)
        .location(accesoTemporal)
        // La redirección no se guarda: cada apertura vuelve a pedir permiso.
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .build();
  }
}
