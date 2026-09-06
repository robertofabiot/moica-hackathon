package com.moica.usuario.repository;

import com.moica.usuario.entity.Usuario;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /**
   * La cuenta con su fila bloqueada hasta el final de la transacción.
   *
   * <p>Lo pide la moderación antes de tocar el estado de una cuenta. Es el bloqueo que serializa a
   * dos personas administradoras que aplican una medida a la misma persona <em>desde expedientes
   * distintos</em>: bloquear cada caso no bastaría, porque son filas diferentes y las dos
   * transacciones leerían que la cuenta no tiene ninguna medida vigente. La cuenta afectada es lo
   * único que ambas comparten, así que es lo que hay que bloquear.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT usuario FROM Usuario usuario WHERE usuario.idUsuario = :idUsuario")
  Optional<Usuario> bloquearPorId(@Param("idUsuario") Long idUsuario);
}
