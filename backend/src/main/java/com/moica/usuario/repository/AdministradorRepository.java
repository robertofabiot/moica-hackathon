package com.moica.usuario.repository;

import com.moica.usuario.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a datos del rol administrativo.
 *
 * <p>La clave primaria es la misma que la de la cuenta, así que {@code existsById} responde si esa
 * cuenta tiene permisos administrativos.
 */
public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

  /**
   * Concede el rol a una cuenta si todavía no lo tiene, en una sola sentencia.
   *
   * <p>Con varias réplicas arrancando a la vez, comprobar y después insertar dejaría pasar a las
   * dos, y la segunda chocaría con la clave primaria y abortaría su arranque. {@code ON CONFLICT DO
   * NOTHING} convierte ese choque en «ya lo tenía».
   *
   * @return 1 si lo concedió, 0 si la cuenta ya lo tenía
   */
  @Modifying
  @Query(
      value =
          """
          INSERT INTO administrador (id_administrador) VALUES (:idUsuario)
          ON CONFLICT (id_administrador) DO NOTHING
          """,
      nativeQuery = true)
  int concederSiFalta(@Param("idUsuario") Long idUsuario);
}
