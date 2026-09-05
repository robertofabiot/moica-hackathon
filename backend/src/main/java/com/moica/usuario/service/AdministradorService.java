package com.moica.usuario.service;

import com.moica.usuario.entity.Administrador;
import com.moica.usuario.entity.Usuario;
import com.moica.usuario.repository.AdministradorRepository;
import com.moica.usuario.repository.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rol administrativo de una cuenta.
 *
 * <p>Moica no publica ningún endpoint para conceder este rol: no hay registro público de
 * administradores ni promoción desde la API. La única forma de asignarlo es {@link
 * #asignarloA(String)}, que ejecuta el arranque a partir de {@code MOICA_ADMIN_CORREO} sobre una
 * cuenta ordinaria ya registrada.
 */
@Service
public class AdministradorService {

  private final AdministradorRepository administradores;
  private final UsuarioRepository usuarios;

  public AdministradorService(AdministradorRepository administradores, UsuarioRepository usuarios) {
    this.administradores = administradores;
    this.usuarios = usuarios;
  }

  /** Indica si una cuenta tiene permisos administrativos. */
  @Transactional(readOnly = true)
  public boolean esAdministrador(Long idUsuario) {
    return administradores.existsById(idUsuario);
  }

  /**
   * Cuándo recibió una cuenta sus permisos administrativos.
   *
   * @return vacío si la cuenta no es administradora
   */
  @Transactional(readOnly = true)
  public Optional<OffsetDateTime> fechaDeAsignacion(Long idUsuario) {
    return administradores.findById(idUsuario).map(Administrador::getFechaAsignacion);
  }

  /**
   * Los identificadores de todas las cuentas con rol administrativo.
   *
   * <p>Lo consume la reasignación de casos de moderación, que necesita ofrecer a quién pasar un
   * expediente. Devuelve identificadores y no cuentas: quien los reciba pedirá los nombres a {@link
   * UsuarioService}, y así el rol no se convierte en una vía para leer datos de cuenta.
   *
   * <p>En el MVP el rol se concede solo desde el arranque, así que la lista es corta y no se
   * pagina.
   */
  @Transactional(readOnly = true)
  public List<Long> idsDeAdministradores() {
    return administradores.findAll().stream().map(Administrador::getIdAdministrador).toList();
  }

  /**
   * Concede el rol administrativo a la cuenta de un correo, si esa cuenta existe.
   *
   * <p>Es idempotente en los dos sentidos: si la cuenta ya tiene el rol no se toca nada, y si el
   * correo no corresponde a ninguna cuenta no se crea ninguna. Nunca registra una cuenta.
   *
   * @param correoElectronico correo de la cuenta. Se normaliza igual que al registrarse, para que
   *     la variable de entorno funcione aunque se escriba con mayúsculas o espacios sobrantes
   * @return qué ocurrió, para que quien lo invoque decida qué contar
   */
  @Transactional
  public ResultadoDeAsignacion asignarloA(String correoElectronico) {
    Optional<Usuario> cuenta =
        usuarios.findByCorreoElectronico(correoElectronico.strip().toLowerCase(Locale.ROOT));

    if (cuenta.isEmpty()) {
      return ResultadoDeAsignacion.CUENTA_INEXISTENTE;
    }

    Long idUsuario = cuenta.get().getIdUsuario();
    if (administradores.existsById(idUsuario)) {
      return ResultadoDeAsignacion.YA_LO_TENIA;
    }

    administradores.save(new Administrador(idUsuario));
    return ResultadoDeAsignacion.ASIGNADO;
  }

  /** Lo que pudo pasar al intentar asignar el rol administrativo. */
  public enum ResultadoDeAsignacion {
    /** La cuenta existía sin el rol y acaba de recibirlo. */
    ASIGNADO,
    /** La cuenta ya era administradora; no se cambió nada. */
    YA_LO_TENIA,
    /** Ese correo no corresponde a ninguna cuenta registrada. */
    CUENTA_INEXISTENTE
  }
}
