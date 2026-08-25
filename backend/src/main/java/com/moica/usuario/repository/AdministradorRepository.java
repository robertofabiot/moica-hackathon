package com.moica.usuario.repository;

import com.moica.usuario.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos del rol administrativo.
 *
 * <p>La clave primaria es la misma que la de la cuenta, así que {@code existsById} responde si esa
 * cuenta tiene permisos administrativos.
 */
public interface AdministradorRepository extends JpaRepository<Administrador, Long> {}
