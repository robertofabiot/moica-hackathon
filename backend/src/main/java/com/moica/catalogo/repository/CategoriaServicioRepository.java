package com.moica.catalogo.repository;

import com.moica.catalogo.entity.CategoriaServicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaServicioRepository extends JpaRepository<CategoriaServicio, Short> {

  /** Orden alfabético con desempate por identificador, para un catálogo determinista. */
  List<CategoriaServicio> findAllByOrderByNombreAscIdCategoriaServicioAsc();
}
