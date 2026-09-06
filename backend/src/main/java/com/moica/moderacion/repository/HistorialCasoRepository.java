package com.moica.moderacion.repository;

import com.moica.moderacion.entity.HistorialCaso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Las versiones SCD2 de los casos de moderación.
 *
 * <p>P9 solo escribía: la apertura de un caso crea su primera versión en la misma transacción. P10A
 * añade lo que la revisión administrativa necesita: leer el expediente completo y encontrar la
 * versión vigente para cerrarla antes de crear la siguiente.
 *
 * <p>No hay ningún método que borre ni que actualice en bloque. El historial es la evidencia de una
 * investigación: solo crece.
 */
public interface HistorialCasoRepository extends JpaRepository<HistorialCaso, Long> {

  /**
   * Todas las versiones de un caso, de la más antigua a la más reciente.
   *
   * <p>El orden es el del número de versión y no el de la fecha: dos eventos del mismo instante son
   * posibles y la secuencia de versiones nunca es ambigua.
   */
  List<HistorialCaso> findByIdCasoModeracionOrderByNumeroVersionAsc(Long idCasoModeracion);

  /**
   * La versión vigente de un caso.
   *
   * <p>Devuelve como máximo una: {@code uq_historial_caso_version_actual} es un índice único
   * parcial sobre las filas con {@code es_version_actual}, así que dos vigentes no pueden existir.
   * Vacío significaría un caso sin fotografía actual, que el esquema tampoco permite crear.
   */
  Optional<HistorialCaso> findByIdCasoModeracionAndEsVersionActualTrue(Long idCasoModeracion);
}
