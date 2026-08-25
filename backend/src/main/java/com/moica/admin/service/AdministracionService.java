package com.moica.admin.service;

import com.moica.admin.dto.ResumenAdministrativo;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.usuario.dto.DatosDeUsuario;
import com.moica.usuario.service.AdministradorService;
import com.moica.usuario.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que ofrece el área administrativa en P3.
 *
 * <p>De momento solo sabe describir a quién entró. Sirve para demostrar que la protección del área
 * funciona: el resto de capacidades administrativas llega con sus propios incrementos.
 *
 * <p>La comprobación de permisos no está aquí, sino en la cadena de seguridad, que exige rol
 * administrativo y segundo factor verificado para todo {@code /api/admin}. La comprobación de esta
 * clase es la última red: si alguien alcanzara el servicio sin el rol, tampoco obtendría datos.
 */
@Service
public class AdministracionService {

  private final UsuarioService usuarios;
  private final AdministradorService administradores;

  public AdministracionService(UsuarioService usuarios, AdministradorService administradores) {
    this.usuarios = usuarios;
    this.administradores = administradores;
  }

  /** Describe la cuenta administradora que hace la petición. */
  @Transactional(readOnly = true)
  public ResumenAdministrativo resumirPara(Long idUsuario) {
    DatosDeUsuario usuario = usuarios.obtener(idUsuario);

    return administradores
        .fechaDeAsignacion(idUsuario)
        .map(
            fecha ->
                new ResumenAdministrativo(
                    usuario.nombreCompleto(), usuario.correoElectronico(), fecha))
        .orElseThrow(
            () ->
                new ErrorDeAplicacion(
                    HttpStatus.FORBIDDEN,
                    "ACCESO_DENEGADO",
                    "Esta cuenta no tiene permisos administrativos."));
  }
}
