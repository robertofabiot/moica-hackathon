package com.moica.moderacion.repository;

import com.moica.moderacion.entity.MedidaAdministrativa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * El catálogo de medidas administrativas.
 *
 * <p>No hay ningún método que borre. Una medida citada por un caso o por una versión del historial
 * es parte de la evidencia de una decisión, y sus claves foráneas son {@code RESTRICT}: lo que el
 * negocio llama «eliminar» es deshabilitarla. La API tampoco expone {@code DELETE}.
 */
public interface MedidaAdministrativaRepository extends JpaRepository<MedidaAdministrativa, Short> {

  /**
   * El catálogo completo, de la medida más leve a la más grave.
   *
   * <p>La severidad ordena la lista porque es como quien decide compara dos sanciones; el nombre
   * desempata para que dos medidas del mismo nivel no cambien de sitio entre dos consultas.
   *
   * <p>Devuelve también las deshabilitadas: la pantalla de gestión necesita verlas para volver a
   * habilitarlas. Quién puede aplicarse es una decisión aparte, del servicio.
   */
  List<MedidaAdministrativa> findAllByOrderByNivelSeveridadAscNombreAsc();

  boolean existsByCodigoIgnoreCase(String codigo);

  boolean existsByNombreIgnoreCase(String nombre);

  /** Si otra medida distinta de esta ya usa ese nombre. Lo consulta la edición. */
  boolean existsByNombreIgnoreCaseAndIdMedidaAdministrativaNot(
      String nombre, Short idMedidaAdministrativa);
}
