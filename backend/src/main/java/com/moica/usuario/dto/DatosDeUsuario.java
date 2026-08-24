package com.moica.usuario.dto;

import com.moica.usuario.entity.EstadoCuenta;
import com.moica.usuario.entity.Usuario;
import java.time.OffsetDateTime;

/**
 * Vista pública de una cuenta.
 *
 * <p>Es lo único que la capacidad {@code usuario} entrega, tanto a un endpoint como a otra
 * capacidad. Deja fuera el hash de la contraseña a propósito: así ninguna otra parte de la
 * aplicación puede exponerlo por descuido.
 */
public record DatosDeUsuario(
    Long idUsuario,
    String nombreCompleto,
    String correoElectronico,
    EstadoCuenta estadoCuenta,
    OffsetDateTime fechaRegistro) {

  public static DatosDeUsuario de(Usuario usuario) {
    return new DatosDeUsuario(
        usuario.getIdUsuario(),
        usuario.getNombreCompleto(),
        usuario.getCorreoElectronico(),
        usuario.getEstadoCuenta(),
        usuario.getFechaRegistro());
  }
}
