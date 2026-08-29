package com.moica.servicio.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.dto.SolicitudDeOrden;
import com.moica.portafolio.dto.SolicitudDeTextoAlternativo;
import com.moica.servicio.dto.DatosDeImagenDeServicio;
import com.moica.servicio.dto.DatosDeServicioPublicado;
import com.moica.servicio.dto.SolicitudDeEstadoDeServicio;
import com.moica.servicio.dto.SolicitudDeServicio;
import com.moica.servicio.service.ImagenDeServicioService;
import com.moica.servicio.service.ServicioPublicadoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Los servicios del prestador de la sesión, con sus imágenes.
 *
 * <p>Ninguna ruta lleva identificador de cuenta: operar sobre el servicio de otra persona no es una
 * petición que se pueda formular. Un recurso ajeno responde 404. No hay borrado físico.
 */
@RestController
@RequestMapping("/api/prestador/servicios")
public class ServicioPropioController {

  private final ServicioPublicadoService servicios;
  private final ImagenDeServicioService imagenes;

  public ServicioPropioController(
      ServicioPublicadoService servicios, ImagenDeServicioService imagenes) {
    this.servicios = servicios;
    this.imagenes = imagenes;
  }

  @GetMapping
  public List<DatosDeServicioPublicado> listar(@AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return servicios.listar(sujeto);
  }

  @PostMapping
  public ResponseEntity<DatosDeServicioPublicado> crear(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDeServicio solicitud) {
    return ResponseEntity.status(HttpStatus.CREATED).body(servicios.crear(sujeto, solicitud));
  }

  @GetMapping("/{idServicio}")
  public DatosDeServicioPublicado consultar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idServicio) {
    return servicios.consultar(sujeto, idServicio);
  }

  @PutMapping("/{idServicio}")
  public DatosDeServicioPublicado actualizar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idServicio,
      @Valid @RequestBody SolicitudDeServicio solicitud) {
    return servicios.actualizar(sujeto, idServicio, solicitud);
  }

  @PutMapping("/{idServicio}/estado")
  public DatosDeServicioPublicado cambiarEstado(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idServicio,
      @Valid @RequestBody SolicitudDeEstadoDeServicio solicitud) {
    return servicios.cambiarEstado(sujeto, idServicio, solicitud);
  }

  @PostMapping(path = "/{idServicio}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<DatosDeImagenDeServicio> subirImagen(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idServicio,
      @RequestPart("archivo") MultipartFile archivo,
      @RequestParam(name = "textoAlternativo", required = false) String textoAlternativo) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(imagenes.subir(sujeto, idServicio, archivo, textoAlternativo));
  }

  @PutMapping("/{idServicio}/imagenes/orden")
  public List<DatosDeImagenDeServicio> reordenarImagenes(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idServicio,
      @Valid @RequestBody SolicitudDeOrden orden) {
    return servicios.reordenarImagenes(sujeto, idServicio, orden);
  }

  @PutMapping("/{idServicio}/imagenes/{idImagen}")
  public DatosDeImagenDeServicio actualizarTextoAlternativo(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idServicio,
      @PathVariable Long idImagen,
      @Valid @RequestBody SolicitudDeTextoAlternativo solicitud) {
    return servicios.actualizarTextoAlternativo(sujeto, idServicio, idImagen, solicitud);
  }

  @DeleteMapping("/{idServicio}/imagenes/{idImagen}")
  public ResponseEntity<Void> eliminarImagen(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idServicio,
      @PathVariable Long idImagen) {
    imagenes.eliminar(sujeto, idServicio, idImagen);
    return ResponseEntity.noContent().build();
  }
}
