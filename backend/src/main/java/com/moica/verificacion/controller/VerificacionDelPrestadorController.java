package com.moica.verificacion.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.verificacion.dto.DatosDeEstadoDeVerificacion;
import com.moica.verificacion.dto.DatosDeSolicitudVerificacion;
import com.moica.verificacion.service.EnvioDeExpedienteService;
import com.moica.verificacion.service.VerificacionDelPrestadorService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * El expediente de verificación de quien usa Moica, como un recurso propio.
 *
 * <p>Ninguna ruta lleva identificador de cuenta ni de perfil: el propietario es siempre el sujeto
 * de la sesión, de modo que presentar o consultar el expediente de otra persona no es una petición
 * que se pueda formular.
 *
 * <p>Aquí no existe forma de resolver una solicitud ni de cambiar el nivel del perfil: eso vive en
 * {@code /api/admin/verificaciones} y exige rol administrativo con segundo factor verificado.
 * Tampoco existe forma de descargar un documento propio: el binario solo lo abre quien revisa.
 */
@RestController
@RequestMapping("/api/prestador/verificacion")
public class VerificacionDelPrestadorController {

  private final VerificacionDelPrestadorService servicio;
  private final EnvioDeExpedienteService envios;

  public VerificacionDelPrestadorController(
      VerificacionDelPrestadorService servicio, EnvioDeExpedienteService envios) {
    this.servicio = servicio;
    this.envios = envios;
  }

  /** Nivel vigente del perfil propio, qué significa, qué puede solicitar y qué está en curso. */
  @GetMapping
  public DatosDeEstadoDeVerificacion consultarEstado(
      @AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return servicio.consultarEstado(sujeto);
  }

  /** Todas las solicitudes propias con los metadatos de sus documentos, la más reciente primero. */
  @GetMapping("/solicitudes")
  public List<DatosDeSolicitudVerificacion> consultarHistorial(
      @AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return servicio.consultarHistorial(sujeto);
  }

  /** Una solicitud propia; 404 si no existe o es de otro prestador. */
  @GetMapping("/solicitudes/{idSolicitudVerificacion}")
  public DatosDeSolicitudVerificacion consultarSolicitud(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idSolicitudVerificacion) {
    return servicio.consultarSolicitudPropia(sujeto, idSolicitudVerificacion);
  }

  /**
   * Envía una solicitud con su expediente completo, en un solo {@code multipart/form-data}.
   *
   * <p>El formulario lleva el nivel, una parte {@code archivo} por documento y un campo {@code
   * tipoDocumento} por documento, en el mismo orden. Es una sola petición a propósito: la solicitud
   * y su expediente nacen juntos o no nacen, así que no hay un momento en el que exista una
   * solicitud sin documentos.
   *
   * <p>El navegador nunca sube directamente al almacenamiento: siempre pasa por aquí.
   */
  @PostMapping(path = "/solicitudes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<DatosDeSolicitudVerificacion> enviarSolicitud(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @RequestParam("nivelSolicitado") String nivelSolicitado,
      @RequestPart("archivo") List<MultipartFile> archivos,
      @RequestParam("tipoDocumento") List<String> tiposDeDocumento) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(envios.enviar(sujeto, nivelSolicitado, archivos, tiposDeDocumento));
  }
}
