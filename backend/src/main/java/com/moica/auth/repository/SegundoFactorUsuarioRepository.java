package com.moica.auth.repository;

import com.moica.auth.entity.SegundoFactorUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos del segundo factor de las cuentas.
 *
 * <p>La clave primaria es la de la cuenta, así que {@code findById} devuelve el segundo factor de
 * esa cuenta, si registró alguno.
 */
public interface SegundoFactorUsuarioRepository extends JpaRepository<SegundoFactorUsuario, Long> {}
