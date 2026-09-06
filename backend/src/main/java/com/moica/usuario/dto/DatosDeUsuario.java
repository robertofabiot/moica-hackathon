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
 *
 * @param esAdministrador si la cuenta tiene permisos administrativos. La interfaz lo necesita para
 *     saber si debe ofrecer el área {@code /admin}; quien decide si puede entrar es siempre el
 *     backend
 * @param fechaFinEstadoCuenta cuándo termina el estado vigente, cuando es temporal. Nulo en {@link
 *     EstadoCuenta#ACTIVA} y en {@link EstadoCuenta#SUSPENDIDA_PERMANENTE}, que no terminan solos.
 *     Es lo que permite al aviso decir «hasta cuándo» en lugar de dejar a la persona sin plazo
 */
public record DatosDeUsuario(
    Long idUsuario,
    String nombreCompleto,
    String correoElectronico,
    EstadoCuenta estadoCuenta,
    OffsetDateTime fechaFinEstadoCuenta,
    boolean esAdministrador,
    OffsetDateTime fechaRegistro) {

  public static DatosDeUsuario de(Usuario usuario, boolean esAdministrador) {
    return new DatosDeUsuario(
        usuario.getIdUsuario(),
        usuario.getNombreCompleto(),
        usuario.getCorreoElectronico(),
        usuario.getEstadoCuenta(),
        usuario.getFechaFinEstadoCuenta(),
        esAdministrador,
        usuario.getFechaRegistro());
  }
}
