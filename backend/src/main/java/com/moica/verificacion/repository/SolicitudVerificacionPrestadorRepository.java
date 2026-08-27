package com.moica.verificacion.repository;

import com.moica.verificacion.entity.EstadoSolicitudVerificacion;
import com.moica.verificacion.entity.NivelVerificacionSolicitado;
import com.moica.verificacion.entity.SolicitudVerificacionPrestador;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudVerificacionPrestadorRepository
    extends JpaRepository<SolicitudVerificacionPrestador, Long> {

  /** El historial propio, de la más reciente a la más antigua. */
  List<SolicitudVerificacionPrestador>
      findByIdPrestadorOrderByFechaSolicitudDescIdSolicitudVerificacionDesc(Long idPrestador);

  /** Buscar por clave y propietario a la vez es lo que impide leer expedientes ajenos. */
  Optional<SolicitudVerificacionPrestador> findByIdSolicitudVerificacionAndIdPrestador(
      Long idSolicitudVerificacion, Long idPrestador);

  /** Las solicitudes del perfil que siguen esperando decisión, sin importar el nivel. */
  List<SolicitudVerificacionPrestador> findByIdPrestadorAndEstadoSolicitudIn(
      Long idPrestador, Collection<EstadoSolicitudVerificacion> estados);

  /** Las solicitudes de un nivel concreto que están en un estado concreto. */
  List<SolicitudVerificacionPrestador> findByIdPrestadorAndNivelSolicitadoAndEstadoSolicitud(
      Long idPrestador,
      NivelVerificacionSolicitado nivelSolicitado,
      EstadoSolicitudVerificacion estadoSolicitud);

  /** La cola administrativa: lo más antiguo primero, que es el orden en que debe atenderse. */
  List<SolicitudVerificacionPrestador>
      findByEstadoSolicitudInOrderByFechaSolicitudAscIdSolicitudVerificacionAsc(
          Collection<EstadoSolicitudVerificacion> estados);

  /** La misma cola, acotada además a un nivel. */
  List<SolicitudVerificacionPrestador>
      findByEstadoSolicitudInAndNivelSolicitadoOrderByFechaSolicitudAscIdSolicitudVerificacionAsc(
          Collection<EstadoSolicitudVerificacion> estados,
          NivelVerificacionSolicitado nivelSolicitado);

  /**
   * La solicitud con su fila bloqueada hasta el final de la transacción.
   *
   * <p>Es lo que impide que dos administradores tomen la misma solicitud a la vez: el segundo
   * espera al primero y, cuando entra, ya lee el estado {@code EN_REVISION} y su comprobación
   * falla. Sin el bloqueo, ambos leerían {@code PENDIENTE} y el último en escribir se quedaría con
   * la revisión sin que nadie lo notara.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT solicitud FROM SolicitudVerificacionPrestador solicitud
      WHERE solicitud.idSolicitudVerificacion = :idSolicitudVerificacion
      """)
  Optional<SolicitudVerificacionPrestador> bloquearPorId(
      @Param("idSolicitudVerificacion") Long idSolicitudVerificacion);
}
