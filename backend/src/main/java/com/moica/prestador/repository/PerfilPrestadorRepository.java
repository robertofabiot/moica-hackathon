package com.moica.prestador.repository;

import com.moica.prestador.entity.PerfilPrestador;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerfilPrestadorRepository extends JpaRepository<PerfilPrestador, Long> {

  /**
   * El perfil con su fila bloqueada hasta el final de la transacción.
   *
   * <p>Es el punto por el que pasa toda escritura sobre {@code perfil_prestador}: quien va a
   * modificarlo lo lee por aquí, de modo que las transacciones que tocan el mismo perfil se ponen
   * en fila en lugar de pisarse. Sin este bloqueo, dos transacciones que trabajan sobre solicitudes
   * de verificación **distintas** no comparten ninguna fila, leen el mismo estado antiguo y la
   * última en escribir borra la decisión de la otra.
   *
   * <p>Devuelve la entidad ya bloqueada, y esa es la única forma correcta de usarlo: cargar antes
   * el perfil sin bloqueo dejaría en el contexto de persistencia una copia anterior a la espera,
   * que el bloqueo posterior no refresca.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT perfil FROM PerfilPrestador perfil WHERE perfil.idPrestador = :idPrestador")
  Optional<PerfilPrestador> bloquearPorId(@Param("idPrestador") Long idPrestador);
}
