package com.moica.usuario.controller;

import com.moica.usuario.dto.DatosDeUsuario;
import com.moica.usuario.dto.SolicitudDeRegistro;
import com.moica.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de las cuentas de Moica. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

  private final UsuarioService servicio;

  public UsuarioController(UsuarioService servicio) {
    this.servicio = servicio;
  }

  /**
   * Registra una cuenta nueva.
   *
   * <p>El registro no inicia sesión: crear la cuenta y autenticarse son dos pasos distintos.
   */
  @PostMapping
  public ResponseEntity<DatosDeUsuario> registrar(
      @Valid @RequestBody SolicitudDeRegistro solicitud) {
    return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud));
  }
}
