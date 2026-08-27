package com.moica.portafolio.repository;

import com.moica.portafolio.entity.ImagenTrabajoPortafolio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagenTrabajoPortafolioRepository
    extends JpaRepository<ImagenTrabajoPortafolio, Long> {

  /** El desempate por identificador da un orden estable cuando dos filas comparten posición. */
  List<ImagenTrabajoPortafolio>
      findByIdTrabajoOrderByOrdenVisualizacionAscIdImagenTrabajoPortafolioAsc(Long idTrabajo);

  List<ImagenTrabajoPortafolio> findByIdTrabajoIn(List<Long> idsDeTrabajo);

  /** Buscar por clave y trabajo a la vez completa la cadena de propiedad hasta la imagen. */
  Optional<ImagenTrabajoPortafolio> findByIdImagenTrabajoPortafolioAndIdTrabajo(
      Long idImagenTrabajoPortafolio, Long idTrabajo);
}
