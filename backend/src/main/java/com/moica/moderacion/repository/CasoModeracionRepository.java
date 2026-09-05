package com.moica.moderacion.repository;

import com.moica.moderacion.entity.CasoModeracion;
import com.moica.moderacion.entity.EstadoCasoModeracion;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
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

  /**
   * El expediente que sostiene la medida vigente de una cuenta, si alguno la sostiene.
   *
   * <p>Devuelve como máximo uno: {@code uq_caso_moderacion_medida_vigente_por_cuenta} es un índice
   * único parcial sobre los casos con medida, así que dos sanciones vigentes sobre la misma persona
   * no pueden existir. Es la consulta que decide si aplicar una medida es una aplicación limpia o
   * un reemplazo que exige confirmación.
   *
   * <p>Quien la llama debe haber bloqueado antes la fila de la cuenta: sin ese bloqueo, dos
   * transacciones simultáneas leerían las dos que no hay ninguna vigente.
   */
  Optional<CasoModeracion> findByIdReportadoAndIdMedidaAdministrativaActualNotNull(
      Long idReportado);

  /**
   * A quién sanciona un expediente, sin cargar el expediente.
   *
   * <p>Devuelve solo el identificador a propósito. Quien va a bloquear necesita saber <em>qué
   * cuenta bloquear</em> antes de tocar el caso, y cargar la entidad para averiguarlo la dejaría en
   * el contexto de persistencia: el {@code SELECT … FOR UPDATE} posterior bloquearía la fila, pero
   * Hibernate devolvería la copia que ya tenía en memoria en lugar de releerla. La transacción
   * seguiría trabajando con el estado anterior al bloqueo, que es justo lo que el bloqueo venía a
   * evitar.
   *
   * <p>{@code idReportado} es inmutable desde la apertura del caso, así que leerlo sin bloqueo es
   * seguro.
   */
  @Query(
      """
      SELECT caso.idReportado FROM CasoModeracion caso
      WHERE caso.idCasoModeracion = :idCasoModeracion
      """)
  Optional<Long> idReportadoDe(@Param("idCasoModeracion") Long idCasoModeracion);

  /**
   * Los expedientes cuya medida temporal ya cumplió su plazo.
   *
   * <p>La consulta el barrido de expiración. Filtra por fecha en la base y no en memoria porque el
   * barrido corre periódicamente sobre toda la tabla y no debe traerse los casos que no vencen.
   *
   * <p>Devuelve identificadores y no entidades por el mismo motivo que {@link #idReportadoDe}: el
   * barrido bloquea cada caso antes de tocarlo, y una entidad ya cargada haría que ese bloqueo no
   * refrescara nada.
   *
   * <p>Una medida sin fecha de fin nunca entra: {@code fechaFinMedidaActual} nulo significa que
   * solo se levanta revocándola, y la comparación descarta los nulos por sí sola.
   */
  @Query(
      """
      SELECT caso.idCasoModeracion FROM CasoModeracion caso
      WHERE caso.idMedidaAdministrativaActual IS NOT NULL
        AND caso.fechaFinMedidaActual <= :instante
      ORDER BY caso.idCasoModeracion
      """)
  List<Long> idsConMedidaVencida(@Param("instante") OffsetDateTime instante);
}
