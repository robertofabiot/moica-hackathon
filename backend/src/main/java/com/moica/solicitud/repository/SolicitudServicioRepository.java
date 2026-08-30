package com.moica.solicitud.repository;

import com.moica.solicitud.entity.SolicitudServicio;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudServicioRepository extends JpaRepository<SolicitudServicio, Long> {

  List<SolicitudServicio> findByIdClienteOrderByFechaCreacionDescIdSolicitudServicioDesc(
      Long idCliente);

  List<SolicitudServicio>
      findByIdServicioPublicadoInOrderByFechaCreacionDescIdSolicitudServicioDesc(
          Collection<Long> idsServicioPublicado);

  /**
   * La solicitud con su fila bloqueada hasta el final de la transacción.
   *
   * <p>Toda transición pasa por aquí. Dos acciones simultáneas sobre la misma fila se ponen en
   * cola: la segunda lee el estado que dejó la primera y no puede aplicar un cambio incompatible.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT solicitud FROM SolicitudServicio solicitud"
          + " WHERE solicitud.idSolicitudServicio = :idSolicitudServicio")
  Optional<SolicitudServicio> bloquearPorId(@Param("idSolicitudServicio") Long idSolicitudServicio);
}
