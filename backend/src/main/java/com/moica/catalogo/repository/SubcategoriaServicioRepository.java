package com.moica.catalogo.repository;

import com.moica.catalogo.entity.SubcategoriaServicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubcategoriaServicioRepository
    extends JpaRepository<SubcategoriaServicio, Integer> {

  List<SubcategoriaServicio> findByIdCategoriaServicioOrderByNombreAscIdSubcategoriaServicioAsc(
      Short idCategoriaServicio);
}
