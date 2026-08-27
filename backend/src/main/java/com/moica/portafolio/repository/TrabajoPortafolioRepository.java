package com.moica.portafolio.repository;

import com.moica.portafolio.entity.TrabajoPortafolio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrabajoPortafolioRepository extends JpaRepository<TrabajoPortafolio, Long> {

  /** El desempate por identificador da un orden estable cuando dos filas comparten posición. */
  List<TrabajoPortafolio> findByIdPrestadorOrderByOrdenVisualizacionAscIdTrabajoAsc(
      Long idPrestador);

  /** Buscar por clave y propietario a la vez es lo que impide operar sobre trabajos ajenos. */
  Optional<TrabajoPortafolio> findByIdTrabajoAndIdPrestador(Long idTrabajo, Long idPrestador);
}
