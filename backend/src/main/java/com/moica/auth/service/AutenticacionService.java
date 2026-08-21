package com.moica.auth.service;

import com.moica.auth.dto.RespuestaDeSesion;
import com.moica.auth.dto.SolicitudDeInicioSesion;
import com.moica.auth.entity.Sesion;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.usuario.dto.DatosDeUsuario;
import com.moica.usuario.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio de la sesión de una persona.
 *
 * <p>Coordina las dos capacidades implicadas: pregunta por las credenciales a {@code usuario} y
 * gestiona la vigencia con {@link SesionService}. Nunca consulta el repositorio de la otra
 * capacidad.
 */
@Service
public class AutenticacionService {

  private final UsuarioService usuarios;
  private final SesionService sesiones;
  private final TokenDeSesionService tokens;

  public AutenticacionService(
      UsuarioService usuarios, SesionService sesiones, TokenDeSesionService tokens) {
    this.usuarios = usuarios;
    this.sesiones = sesiones;
    this.tokens = tokens;
  }

  /**
   * Comprueba las credenciales y abre una sesión.
   *
   * @throws ErrorDeAplicacion si las credenciales no son correctas, con un mensaje que no revela si
   *     el correo existe
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

    Sesion sesion = sesiones.abrir(usuario.idUsuario());

    return new SesionIniciada(tokens.emitir(sesion), RespuestaDeSesion.de(usuario, sesion));
  }

  /**
   * Resultado de iniciar sesión: el token que se entrega en la cookie y lo que se cuenta en el
   * cuerpo de la respuesta.
   */
  public record SesionIniciada(String token, RespuestaDeSesion respuesta) {}
}
