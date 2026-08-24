package com.moica.auth.repository;

import com.moica.auth.entity.MotivoRevocacionSesion;
import com.moica.auth.entity.Sesion;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a datos de las sesiones abiertas. */
public interface SesionRepository extends JpaRepository<Sesion, Long> {

  /**
   * Busca la sesión a la que apunta el {@code jti} de un JWT.
   *
   * <p>Encontrar la fila no significa que conceda acceso: eso lo decide {@link
   * Sesion#estaVigente(java.time.OffsetDateTime)}.
   */
  Optional<Sesion> findByIdentificadorToken(String identificadorToken);

  /**
   * Revoca de una sola vez todas las sesiones de una cuenta que siguen sin revocar.
   *
   * <p>Es lo que hace efectivo el cambio de credenciales: una a una habría una ventana en la que
   * unas sesiones ya no valen y otras sí. Se escribe como actualización masiva a propósito, y por
   * eso el índice {@code ix_sesion_id_usuario} acompaña a esta consulta.
   *
   * @return cuántas sesiones quedaron revocadas
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE Sesion s
      SET s.fechaRevocacion = :instante, s.motivoRevocacion = :motivo
      WHERE s.idUsuario = :idUsuario AND s.fechaRevocacion IS NULL
      """)
  int revocarLasVigentesDe(
      @Param("idUsuario") Long idUsuario,
      @Param("instante") OffsetDateTime instante,
      @Param("motivo") MotivoRevocacionSesion motivo);
}
