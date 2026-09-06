package com.moica.catalogo.repository;

import com.moica.catalogo.entity.Municipio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MunicipioRepository extends JpaRepository<Municipio, Integer> {

  List<Municipio> findByIdDepartamentoOrderByNombre(Short idDepartamento);
}
