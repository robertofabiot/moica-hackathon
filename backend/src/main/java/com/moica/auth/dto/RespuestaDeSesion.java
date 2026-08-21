package com.moica.auth.dto;

import com.moica.auth.entity.Sesion;
import com.moica.usuario.dto.DatosDeUsuario;
import java.time.OffsetDateTime;

/**
 * Lo que la API cuenta sobre la sesión en curso.
 *
 * <p>Describe quién es la persona y hasta cuándo vale su sesión. No incluye el token, ni el
 * identificador que lo vincula con la fila, ni ningún dato con el que se pudiera reconstruir el
 * acceso.
 */
public record RespuestaDeSesion(DatosDeUsuario usuario, DatosDeSesion sesion) {

  /** Vigencia de la sesión, tal como la necesita la interfaz para avisar cuando vence. */
  public record DatosDeSesion(
      OffsetDateTime fechaInicio, OffsetDateTime fechaExpiracion, boolean segundoFactorVerificado) {

    public static DatosDeSesion de(Sesion sesion) {
      return new DatosDeSesion(
          sesion.getFechaInicio(), sesion.getFechaExpiracion(), sesion.isSegundoFactorVerificado());
    }
  }

  public static RespuestaDeSesion de(DatosDeUsuario usuario, Sesion sesion) {
    return new RespuestaDeSesion(usuario, DatosDeSesion.de(sesion));
  }
}
