package com.moica.usuario.service;

import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.usuario.dto.DatosDeUsuario;
import com.moica.usuario.dto.SolicitudDeRegistro;
import com.moica.usuario.entity.Usuario;
import com.moica.usuario.repository.UsuarioRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de las cuentas de Moica.
 *
 * <p>Es el único punto por el que se crean cuentas y se comprueban credenciales. El hash de la
 * contraseña no sale de aquí: hacia fuera solo viaja {@link DatosDeUsuario}.
 */
@Service
public class UsuarioService {

  private final UsuarioRepository repositorio;
  private final PasswordEncoder codificador;

  /**
   * Hash de un valor aleatorio que nadie conoce, calculado al arrancar.
   *
   * <p>Sirve para que autenticar un correo inexistente cueste lo mismo que autenticar uno real: sin
   * él, la diferencia de tiempo delataría qué correos están registrados.
   */
  private final String hashDeCorreoInexistente;

  public UsuarioService(UsuarioRepository repositorio, PasswordEncoder codificador) {
    this.repositorio = repositorio;
    this.codificador = codificador;
    this.hashDeCorreoInexistente = codificador.encode(UUID.randomUUID().toString());
  }

  /**
   * Crea una cuenta con la contraseña ya convertida en hash.
   *
   * @throws ErrorDeAplicacion si el correo ya pertenece a otra cuenta
   */
  @Transactional
  public DatosDeUsuario registrar(SolicitudDeRegistro solicitud) {
    String correo = normalizar(solicitud.correoElectronico());

    if (repositorio.existsByCorreoElectronico(correo)) {
      throw correoYaRegistrado();
    }

    Usuario usuario =
        new Usuario(
            solicitud.nombreCompleto().strip(), correo, codificador.encode(solicitud.clave()));

    try {
      return DatosDeUsuario.de(repositorio.saveAndFlush(usuario));
    } catch (DataIntegrityViolationException colision) {
      // La comprobación previa no basta: dos registros simultáneos con el mismo
      // correo solo los separa la restricción única de la base de datos.
      throw correoYaRegistrado();
    }
  }

  /**
   * Comprueba unas credenciales.
   *
   * <p>Devuelve vacío tanto si el correo no existe como si la contraseña no coincide, de modo que
   * quien llama no pueda distinguir un caso del otro ni siquiera queriendo.
   */
  @Transactional(readOnly = true)
  public Optional<DatosDeUsuario> autenticar(String correoElectronico, String clave) {
    Optional<Usuario> usuario = repositorio.findByCorreoElectronico(normalizar(correoElectronico));

    if (usuario.isEmpty()) {
      codificador.matches(clave, hashDeCorreoInexistente);
      return Optional.empty();
    }

    return usuario
        .filter(cuenta -> codificador.matches(clave, cuenta.getClaveHash()))
        .map(DatosDeUsuario::de);
  }

  /**
   * Recupera una cuenta por su identificador.
   *
   * @throws ErrorDeAplicacion si la cuenta no existe
   */
  @Transactional(readOnly = true)
  public DatosDeUsuario obtener(Long idUsuario) {
    return repositorio
        .findById(idUsuario)
        .map(DatosDeUsuario::de)
        .orElseThrow(
            () ->
                new ErrorDeAplicacion(
                    HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "La cuenta no existe."));
  }

  /**
   * Deja el correo como se almacena: sin espacios exteriores y en minúsculas.
   *
   * <p>Así « Persona@Moica.NI » y «persona@moica.ni» son la misma cuenta, tanto al registrarse como
   * al iniciar sesión.
   */
  private static String normalizar(String correoElectronico) {
    return correoElectronico.strip().toLowerCase(Locale.ROOT);
  }

  private static ErrorDeAplicacion correoYaRegistrado() {
    return new ErrorDeAplicacion(
        HttpStatus.CONFLICT,
        "CORREO_YA_REGISTRADO",
        "Ese correo ya tiene una cuenta en Moica. Inicia sesión o usa otro correo.");
  }
}
