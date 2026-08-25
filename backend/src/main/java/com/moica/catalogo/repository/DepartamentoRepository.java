package com.moica.catalogo.repository;

import com.moica.catalogo.entity.Departamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartamentoRepository extends JpaRepository<Departamento, Short> {

  List<Departamento> findByHabilitadoTrueOrderByNombre();
}
