package com.moica.moderacion.repository;

import com.moica.moderacion.entity.CasoModeracion;
import com.moica.moderacion.entity.EstadoCasoModeracion;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /**
   * La bandeja administrativa, de la más antigua a la más reciente.
   *
   * <p>Ordena por apertura ascendente a propósito: lo que lleva más tiempo esperando decisión sale
   * primero, igual que hace la cola de verificaciones. El identificador desempata para que dos
   * casos abiertos en el mismo instante no cambien de sitio entre dos consultas.
   *
   * <p>Es una consulta administrativa: no filtra por participante porque quien la ejecuta ya pasó
   * por rol y segundo factor.
   */
  List<CasoModeracion> findByEstadoActualInOrderByFechaAperturaAscIdCasoModeracionAsc(
      Collection<EstadoCasoModeracion> estados);

  /** La misma bandeja acotada a los casos de una persona administradora. */
  List<CasoModeracion>
      findByEstadoActualInAndIdAdministradorResponsableOrderByFechaAperturaAscIdCasoModeracionAsc(
          Collection<EstadoCasoModeracion> estados, Long idAdministradorResponsable);

  /**
   * El caso con su fila bloqueada hasta el final de la transacción.
   *
   * <p>Toda mutación administrativa lo pide antes de leer el estado. Sin él, dos administradores
   * que actúen a la vez leerían el mismo estado de partida y los dos creerían que su transición era
   * válida: uno cerraría un caso que el otro acaba de cerrar, y el historial acabaría con dos
   * versiones que dicen ser la vigente. Con el bloqueo, el segundo entra cuando el primero ya
   * terminó y su comprobación de estado falla con 409.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT caso FROM CasoModeracion caso
      WHERE caso.idCasoModeracion = :idCasoModeracion
      """)
  Optional<CasoModeracion> bloquearPorId(@Param("idCasoModeracion") Long idCasoModeracion);
}
