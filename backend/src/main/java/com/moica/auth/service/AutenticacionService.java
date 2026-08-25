package com.moica.auth.service;

import com.moica.auth.dto.RespuestaDeSesion;
import com.moica.auth.dto.SolicitudDeCambioDeClave;
import com.moica.auth.dto.SolicitudDeInicioSesion;
import com.moica.auth.entity.MotivoRevocacionSesion;
import com.moica.auth.entity.Sesion;
import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.usuario.dto.DatosDeUsuario;
import com.moica.usuario.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio, consulta, verificación y cierre de la sesión de una persona, y cambio de sus
 * credenciales.
 *
 * <p>Coordina las capacidades implicadas: pregunta por las credenciales a {@code usuario} y
 * gestiona la vigencia con {@link SesionService}. Nunca consulta el repositorio de la otra
 * capacidad.
 */
@Service
public class AutenticacionService {

  private final UsuarioService usuarios;
  private final SesionService sesiones;
  private final SegundoFactorService segundoFactor;
  private final TokenDeSesionService tokens;

  public AutenticacionService(
      UsuarioService usuarios,
      SesionService sesiones,
      SegundoFactorService segundoFactor,
      TokenDeSesionService tokens) {
    this.usuarios = usuarios;
    this.sesiones = sesiones;
    this.segundoFactor = segundoFactor;
    this.tokens = tokens;
  }

  /**
   * Comprueba las credenciales y abre una sesión.
   *
   * <p>Cuando la cuenta tiene el segundo factor activo, la sesión nace provisional: existe y su
   * cookie es válida, pero solo sirve para consultarla, presentar el código y cerrarla. La
   * respuesta lo dice explícitamente en {@code sesion.pendienteDeSegundoFactor}.
   *
   * @throws ErrorDeAplicacion si las credenciales no son correctas, con un mensaje que no revela si
   *     el correo existe, o si la cuenta está suspendida
   */
  @Transactional
  public SesionIniciada iniciarSesion(SolicitudDeInicioSesion solicitud) {
    DatosDeUsuario usuario =
        usuarios
            .autenticar(solicitud.correoElectronico(), solicitud.clave())
            .orElseThrow(
                () ->
                    new ErrorDeAplicacion(
                        HttpStatus.UNAUTHORIZED,
                        "CREDENCIALES_INVALIDAS",
                        "El correo o la contraseña no son correctos."));

    // Una cuenta suspendida no abre sesiones nuevas. Se comprueba después de la
    // contraseña: quien no acierta las credenciales no debe averiguar nada
    // sobre el estado de una cuenta ajena.
    if (usuario.estadoCuenta().bloqueaElAcceso()) {
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "CUENTA_SUSPENDIDA",
          "Esta cuenta está suspendida. Escribe al equipo de Moica si crees que es un error.");
    }

    Sesion sesion = sesiones.abrir(usuario.idUsuario());
    boolean requerido = segundoFactor.estaActivoEn(usuario.idUsuario());

    return new SesionIniciada(
        tokens.emitir(sesion), RespuestaDeSesion.de(usuario, sesion, requerido));
  }

  /** Describe la sesión con la que llega la petición en curso. */
  @Transactional(readOnly = true)
  public RespuestaDeSesion consultar(UsuarioAutenticado sujeto) {
    // Se releen ambas filas en lugar de copiarlas en el contexto de seguridad:
    // así la respuesta refleja el estado actual de la cuenta y de la sesión.
    return RespuestaDeSesion.de(
        usuarios.obtener(sujeto.idUsuario()),
        sesiones.obtener(sujeto.idSesion()),
        segundoFactor.estaActivoEn(sujeto.idUsuario()));
  }

  /**
   * Completa la sesión en curso con el código del segundo factor.
   *
   * <p>Completa <em>solo esta</em> sesión: las demás que la cuenta tenga abiertas siguen siendo
   * provisionales hasta que cada una presente el suyo.
   *
   * @throws ErrorDeAplicacion si la cuenta no tiene el segundo factor activo o el código no es
   *     válido
   */
  @Transactional
  public RespuestaDeSesion verificarSegundoFactor(UsuarioAutenticado sujeto, String codigo) {
    segundoFactor.verificarCodigo(sujeto.idUsuario(), codigo);
    sesiones.marcarSegundoFactorVerificado(sujeto.idSesion());

    return consultar(sujeto);
  }

  /** Cierra la sesión en curso, dejando registrado que fue la persona quien la cerró. */
  @Transactional
  public void cerrarSesion(UsuarioAutenticado sujeto) {
    sesiones.revocar(sujeto.idSesion(), MotivoRevocacionSesion.CIERRE_VOLUNTARIO);
  }

  /**
   * Cambia la contraseña y deja fuera a todas las sesiones de la cuenta, incluida la actual.
   *
   * <p>Las dos cosas ocurren en la misma transacción: si la revocación fallara, la contraseña no
   * cambiaría, y no puede existir un instante en el que la clave sea nueva y las sesiones antiguas
   * sigan valiendo.
   *
   * @throws ErrorDeAplicacion si la contraseña actual no es correcta
   */
  @Transactional
  public void cambiarClave(UsuarioAutenticado sujeto, SolicitudDeCambioDeClave solicitud) {
    usuarios.cambiarClave(sujeto.idUsuario(), solicitud.claveActual(), solicitud.claveNueva());
    sesiones.revocarTodasDe(sujeto.idUsuario(), MotivoRevocacionSesion.CAMBIO_CREDENCIALES);
  }

  /**
   * Resultado de iniciar sesión: el token que se entrega en la cookie y lo que se cuenta en el
   * cuerpo de la respuesta.
   */
  public record SesionIniciada(String token, RespuestaDeSesion respuesta) {

    /** Se redefine a propósito: el token es el JWT que abre la sesión recién creada. */
    @Override
    public String toString() {
      return "SesionIniciada[token=(oculto), respuesta=" + respuesta + "]";
    }
  }
}
