package com.moica.solicitud.repository;

import com.moica.solicitud.entity.CambioEstadoSolicitud;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CambioEstadoSolicitudRepository
    extends JpaRepository<CambioEstadoSolicitud, Long> {

  List<CambioEstadoSolicitud>
      findByIdSolicitudServicioOrderByFechaCambioAscIdCambioEstadoSolicitudAsc(
          Long idSolicitudServicio);
}
