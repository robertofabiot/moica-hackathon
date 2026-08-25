package com.moica.auth.dto;

import com.moica.auth.entity.Sesion;
import com.moica.usuario.dto.DatosDeUsuario;
import java.time.OffsetDateTime;

/**
 * Lo que la API cuenta sobre la sesión en curso.
 *
 * <p>Describe quién es la persona, hasta cuándo vale su sesión y si todavía le falta algo para
 * poder usarla. No incluye el token, ni el identificador que lo vincula con la fila, ni ningún dato
 * con el que se pudiera reconstruir el acceso.
 */
public record RespuestaDeSesion(DatosDeUsuario usuario, DatosDeSesion sesion) {

  /**
   * Vigencia de la sesión y estado de su segundo factor.
   *
   * @param segundoFactorRequerido si la cuenta tiene el segundo factor activo
   * @param segundoFactorVerificado si esta sesión ya presentó un código válido
   * @param pendienteDeSegundoFactor las dos cosas a la vez: la cuenta lo exige y esta sesión aún no
   *     lo ha superado. Es la señal explícita de que la sesión es provisional y de que la interfaz
   *     debe llevar a la pantalla de verificación; se calcula aquí para que ningún cliente tenga
   *     que deducir la regla por su cuenta
   */
  public record DatosDeSesion(
      OffsetDateTime fechaInicio,
      OffsetDateTime fechaExpiracion,
      boolean segundoFactorRequerido,
      boolean segundoFactorVerificado,
      boolean pendienteDeSegundoFactor) {

    public static DatosDeSesion de(Sesion sesion, boolean segundoFactorRequerido) {
      boolean verificado = sesion.isSegundoFactorVerificado();

      return new DatosDeSesion(
          sesion.getFechaInicio(),
          sesion.getFechaExpiracion(),
          segundoFactorRequerido,
          verificado,
          segundoFactorRequerido && !verificado);
    }
  }

  public static RespuestaDeSesion de(
      DatosDeUsuario usuario, Sesion sesion, boolean segundoFactorRequerido) {
    return new RespuestaDeSesion(usuario, DatosDeSesion.de(sesion, segundoFactorRequerido));
  }
}
