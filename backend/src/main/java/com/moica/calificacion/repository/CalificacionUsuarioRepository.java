package com.moica.calificacion.repository;

import com.moica.calificacion.entity.CalificacionUsuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalificacionUsuarioRepository extends JpaRepository<CalificacionUsuario, Long> {

  /** La calificación que una persona emitió en una solicitud, si la emitió. */
  Optional<CalificacionUsuario> findByIdSolicitudServicioAndIdCalificador(
      Long idSolicitudServicio, Long idCalificador);

  boolean existsByIdSolicitudServicioAndIdCalificador(Long idSolicitudServicio, Long idCalificador);
}
