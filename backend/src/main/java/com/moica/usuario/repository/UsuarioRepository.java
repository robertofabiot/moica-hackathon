package com.moica.usuario.repository;

import com.moica.usuario.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de las cuentas registradas. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

  /**
   * Busca una cuenta por su correo.
   *
   * @param correoElectronico correo ya normalizado, tal como se almacena
   */
  Optional<Usuario> findByCorreoElectronico(String correoElectronico);

  /**
   * Indica si ya existe una cuenta con ese correo.
   *
   * @param correoElectronico correo ya normalizado, tal como se almacena
   */
  boolean existsByCorreoElectronico(String correoElectronico);
}
