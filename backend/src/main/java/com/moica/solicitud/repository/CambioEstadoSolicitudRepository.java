package com.moica.solicitud.repository;

import com.moica.solicitud.entity.CambioEstadoSolicitud;
import com.moica.solicitud.entity.EstadoSolicitud;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CambioEstadoSolicitudRepository
    extends JpaRepository<CambioEstadoSolicitud, Long> {

  List<CambioEstadoSolicitud>
      findByIdSolicitudServicioOrderByFechaCambioAscIdCambioEstadoSolicitudAsc(
          Long idSolicitudServicio);

  /**
   * Si la solicitud alcanzó ese estado alguna vez.
   *
   * <p>El estado vigente no basta para saberlo: una {@code CANCELADA} puede venir de {@code
   * PENDIENTE}, y entonces nunca hubo hilo, o de {@code ACEPTADA}, y entonces el historial del chat
   * queda visible en solo lectura.
   */
  boolean existsByIdSolicitudServicioAndEstadoNuevo(
      Long idSolicitudServicio, EstadoSolicitud estadoNuevo);
}
