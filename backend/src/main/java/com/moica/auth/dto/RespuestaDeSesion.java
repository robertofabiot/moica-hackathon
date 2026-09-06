package com.moica.auth.dto;

import com.moica.auth.entity.Sesion;
import com.moica.usuario.dto.DatosDeUsuario;
import com.moica.usuario.entity.EstadoCuenta;
import java.time.OffsetDateTime;

/**
 * Lo que la API cuenta sobre la sesión en curso.
 *
 * <p>Describe quién es la persona, hasta cuándo vale su sesión y si todavía le falta algo para
 * poder usarla. No incluye el token, ni el identificador que lo vincula con la fila, ni ningún dato
 * con el que se pudiera reconstruir el acceso.
 *
 * @param avisoDeCuenta lo que hay que decirle a quien arrastra una medida administrativa: hasta
 *     cuándo dura y a dónde escribir para apelarla. Nulo cuando la cuenta está {@link
 *     EstadoCuenta#ACTIVA}, que es el caso normal y no necesita ningún aviso
 */
public record RespuestaDeSesion(
    DatosDeUsuario usuario, DatosDeSesion sesion, AvisoDeCuenta avisoDeCuenta) {

  /**
   * El aviso que acompaña a una cuenta restringida o suspendida.
   *
   * <p>Solo lleva lo que la persona afectada necesita para entender su situación y reaccionar. No
   * dice qué medida se le aplicó, ni desde qué caso, ni quién la decidió: eso es información
   * administrativa del expediente y no sale de {@code /api/admin}.
   *
   * <p>En la práctica lo lee una cuenta {@link EstadoCuenta#RESTRINGIDA_TEMPORAL}, que conserva su
   * sesión. Una suspendida no llega hasta aquí porque no puede abrir sesión; a ella se le explica
   * lo mismo en el mensaje con el que se le rechaza el acceso.
   *
   * @param fechaFin cuándo termina la restricción; nulo si no termina sola
   * @param canalDeSoporte a dónde escribir para apelar. La apelación se presenta fuera de Moica
   */
  public record AvisoDeCuenta(OffsetDateTime fechaFin, String canalDeSoporte) {}

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
      DatosDeUsuario usuario,
      Sesion sesion,
      boolean segundoFactorRequerido,
      String canalDeSoporte) {

    return new RespuestaDeSesion(
        usuario,
        DatosDeSesion.de(sesion, segundoFactorRequerido),
        avisoPara(usuario, canalDeSoporte));
  }

  private static AvisoDeCuenta avisoPara(DatosDeUsuario usuario, String canalDeSoporte) {
    return usuario.estadoCuenta() == EstadoCuenta.ACTIVA
        ? null
        : new AvisoDeCuenta(usuario.fechaFinEstadoCuenta(), canalDeSoporte);
  }
}
