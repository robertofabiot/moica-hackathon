package com.moica.auth.repository;

import com.moica.auth.entity.Sesion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de las sesiones abiertas. */
public interface SesionRepository extends JpaRepository<Sesion, Long> {

  /**
   * Busca la sesión a la que apunta el {@code jti} de un JWT.
   *
   * <p>Encontrar la fila no significa que conceda acceso: eso lo decide {@link
   * Sesion#estaVigente(java.time.OffsetDateTime)}.
   */
  Optional<Sesion> findByIdentificadorToken(String identificadorToken);
}
