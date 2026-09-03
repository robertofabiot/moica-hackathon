package com.moica.moderacion.repository;

import com.moica.moderacion.entity.CasoModeracion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CasoModeracionRepository extends JpaRepository<CasoModeracion, Long> {

  /**
   * El caso que una persona abrió en una solicitud, si lo abrió.
   *
   * <p>Es la única forma en que el reportante llega a su expediente: se busca por la pareja
   * solicitud–reportante y nunca por identificador de caso. Así ninguna petición puede alcanzar un
   * caso ajeno, ni siquiera el que la contraparte presentó sobre la misma solicitud.
   *
   * <p>La resuelve el índice de {@code uq_caso_moderacion_solicitud_reportante}.
   */
  Optional<CasoModeracion> findByIdSolicitudServicioAndIdReportante(
      Long idSolicitudServicio, Long idReportante);

  boolean existsByIdSolicitudServicioAndIdReportante(Long idSolicitudServicio, Long idReportante);
}
