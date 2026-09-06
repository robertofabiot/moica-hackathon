package com.moica.portafolio.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.dto.SolicitudDeOrden;
import com.moica.portafolio.dto.DatosDeImagenDeTrabajo;
import com.moica.portafolio.dto.DatosDeTrabajo;
import com.moica.portafolio.dto.SolicitudDeTextoAlternativo;
import com.moica.portafolio.dto.SolicitudDeTrabajo;
import com.moica.portafolio.service.ImagenDeTrabajoService;
import com.moica.portafolio.service.TrabajoPortafolioService;
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
 * Los trabajos del portafolio del prestador de la sesión, con sus imágenes.
 *
 * <p>El portafolio es una sección del perfil (definición 5.5): sus rutas viven bajo {@code
 * /api/prestador} aunque la capacidad sea propia. En P4 solo lo ve y administra su propietario; la
 * vista pública llega con el descubrimiento (P5).
 */
@RestController
@RequestMapping("/api/prestador/portafolio/trabajos")
public class PortafolioController {

  private final TrabajoPortafolioService trabajos;
  private final ImagenDeTrabajoService imagenes;

  public PortafolioController(TrabajoPortafolioService trabajos, ImagenDeTrabajoService imagenes) {
    this.trabajos = trabajos;
    this.imagenes = imagenes;
  }

  /** Los trabajos propios en su orden, cada uno con sus imágenes. */
  @GetMapping
  public List<DatosDeTrabajo> listar(@AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return trabajos.listar(sujeto);
  }

  /** Agrega un trabajo al final del portafolio. */
  @PostMapping
  public ResponseEntity<DatosDeTrabajo> crear(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDeTrabajo solicitud) {
    return ResponseEntity.status(HttpStatus.CREATED).body(trabajos.crear(sujeto, solicitud));
  }

  /** Deja los trabajos en el orden pedido, con la lista completa de identificadores. */
  @PutMapping("/orden")
  public List<DatosDeTrabajo> reordenar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDeOrden orden) {
    return trabajos.reordenar(sujeto, orden);
  }

  /** Sustituye título, descripción y fecha de un trabajo propio. */
  @PutMapping("/{idTrabajo}")
  public DatosDeTrabajo actualizar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idTrabajo,
      @Valid @RequestBody SolicitudDeTrabajo solicitud) {
    return trabajos.actualizar(sujeto, idTrabajo, solicitud);
  }

  /** Elimina un trabajo propio, sus imágenes y sus objetos del almacén. */
  @DeleteMapping("/{idTrabajo}")
  public ResponseEntity<Void> eliminar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idTrabajo) {
    imagenes.eliminarTrabajo(sujeto, idTrabajo);
    return ResponseEntity.noContent().build();
  }

  /**
   * Sube una imagen a un trabajo propio.
   *
   * <p>El archivo viaja en la parte {@code archivo}; el texto alternativo, opcional, en el campo
   * {@code textoAlternativo} del mismo formulario.
   */
  @PostMapping(path = "/{idTrabajo}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<DatosDeImagenDeTrabajo> subirImagen(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idTrabajo,
      @RequestPart("archivo") MultipartFile archivo,
      @RequestParam(name = "textoAlternativo", required = false) String textoAlternativo) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(imagenes.subir(sujeto, idTrabajo, archivo, textoAlternativo));
  }

  /** Deja las imágenes de un trabajo propio en el orden pedido. */
  @PutMapping("/{idTrabajo}/imagenes/orden")
  public List<DatosDeImagenDeTrabajo> reordenarImagenes(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idTrabajo,
      @Valid @RequestBody SolicitudDeOrden orden) {
    return trabajos.reordenarImagenes(sujeto, idTrabajo, orden);
  }

  /** Sustituye el texto alternativo de una imagen propia. */
  @PutMapping("/{idTrabajo}/imagenes/{idImagen}")
  public DatosDeImagenDeTrabajo actualizarTextoAlternativo(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idTrabajo,
      @PathVariable Long idImagen,
      @Valid @RequestBody SolicitudDeTextoAlternativo solicitud) {
    return trabajos.actualizarTextoAlternativo(sujeto, idTrabajo, idImagen, solicitud);
  }

  /** Elimina una imagen propia y retira su objeto del almacén. */
  @DeleteMapping("/{idTrabajo}/imagenes/{idImagen}")
  public ResponseEntity<Void> eliminarImagen(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idTrabajo,
      @PathVariable Long idImagen) {
    imagenes.eliminar(sujeto, idTrabajo, idImagen);
    return ResponseEntity.noContent().build();
  }
}
