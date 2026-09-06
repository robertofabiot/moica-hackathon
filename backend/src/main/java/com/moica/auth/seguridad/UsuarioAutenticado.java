package com.moica.auth.seguridad;

import com.moica.usuario.entity.EstadoCuenta;

/**
 * Quién hace la petición, con qué sesión y hasta dónde llega.
 *
 * <p>Es el sujeto autenticado que {@link FiltroDeSesion} deja en el contexto de seguridad. Lleva la
 * sesión además de la cuenta porque cerrar sesión revoca exactamente la que se está usando, no
 * todas las de la persona.
 *
 * <p>No guarda nombre ni correo a propósito: esos datos se piden a la capacidad {@code usuario}
 * cuando hacen falta, y así no quedan copiados en el contexto de seguridad. Lo que sí lleva es lo
 * que la autorización necesita decidir en cada petición, releído de la base de datos: quitarle el
 * rol o suspender la cuenta tiene efecto en la petición siguiente, sin esperar a que caduque nada.
 *
 * @param administrador si la cuenta tiene permisos administrativos
 * @param estadoCuenta estado operativo vigente de la cuenta
 * @param segundoFactorRequerido si la cuenta tiene el segundo factor en estado {@code ACTIVO}
 * @param segundoFactorVerificado si <em>esta</em> sesión ya presentó un código válido
 */
public record UsuarioAutenticado(
    Long idUsuario,
    Long idSesion,
    boolean administrador,
    EstadoCuenta estadoCuenta,
    boolean segundoFactorRequerido,
    boolean segundoFactorVerificado) {

  /**
   * Sesión abierta con la contraseña correcta a la que todavía le falta el segundo factor.
   *
   * <p>Son las dos condiciones a la vez. Una sesión con {@code segundoFactorVerificado = false} de
   * una cuenta que no usa segundo factor no es provisional: es una sesión normal y completa.
   */
  public boolean esProvisional() {
    return segundoFactorRequerido && !segundoFactorVerificado;
  }

  /** Si la sesión sirve para operar con normalidad en Moica. */
  public boolean esPlena() {
    return !esProvisional() && !estadoCuenta.bloqueaElAcceso();
  }

  /** Si esta sesión puede entrar en el área administrativa. */
  public boolean puedeAdministrar() {
    return esPlena() && administrador && segundoFactorVerificado;
  }
}
