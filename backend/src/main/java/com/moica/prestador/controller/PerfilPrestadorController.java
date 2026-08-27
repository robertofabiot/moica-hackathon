package com.moica.prestador.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.prestador.dto.DatosDePerfilPrestador;
import com.moica.prestador.dto.SolicitudDeDisponibilidad;
import com.moica.prestador.dto.SolicitudDePerfilPrestador;
import com.moica.prestador.service.ImagenDePerfilService;
import com.moica.prestador.service.PerfilPrestadorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * El perfil de prestador de quien usa Moica, como un único recurso propio.
 *
 * <p>Ninguna ruta lleva identificador de cuenta: el propietario es siempre el sujeto de la sesión,
 * de modo que operar sobre el perfil de otra persona no es una petición que se pueda formular.
 *
 * <p>No hay {@code DELETE /api/prestador/perfil} a propósito: la definición vigente solo autoriza
 * crear y actualizar el perfil.
 */
@RestController
@RequestMapping("/api/prestador")
public class PerfilPrestadorController {

  private final PerfilPrestadorService servicio;
  private final ImagenDePerfilService imagenes;

  public PerfilPrestadorController(
      PerfilPrestadorService servicio, ImagenDePerfilService imagenes) {
    this.servicio = servicio;
    this.imagenes = imagenes;
  }

  /** Crea el perfil propio. Todo perfil nace disponible y sin verificar. */
  @PostMapping("/perfil")
  public ResponseEntity<DatosDePerfilPrestador> crear(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDePerfilPrestador solicitud) {
    return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(sujeto, solicitud));
  }

  /** Devuelve el perfil propio; 404 con {@code PERFIL_NO_ENCONTRADO} si aún no existe. */
  @GetMapping("/perfil")
  public DatosDePerfilPrestador consultar(@AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return servicio.consultarPropio(sujeto);
  }

  /** Actualiza los datos editables del perfil propio. */
  @PutMapping("/perfil")
  public DatosDePerfilPrestador actualizar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDePerfilPrestador solicitud) {
    return servicio.actualizar(sujeto, solicitud);
  }

  /** Cambia entre {@code DISPONIBLE} y {@code NO_DISPONIBLE}. */
  @PutMapping("/disponibilidad")
  public DatosDePerfilPrestador cambiarDisponibilidad(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @Valid @RequestBody SolicitudDeDisponibilidad solicitud) {
    return servicio.cambiarDisponibilidad(sujeto, solicitud);
  }

  /** Sube o sustituye la imagen de perfil. El archivo viaja en la parte {@code archivo}. */
  @PutMapping(path = "/perfil/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DatosDePerfilPrestador subirImagen(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @RequestPart("archivo") MultipartFile archivo) {
    return imagenes.subir(sujeto, archivo);
  }

  /** Quita la imagen de perfil y retira su objeto del almacén. */
  @DeleteMapping("/perfil/imagen")
  public DatosDePerfilPrestador eliminarImagen(@AuthenticationPrincipal UsuarioAutenticado sujeto) {
    return imagenes.eliminar(sujeto);
  }
}
