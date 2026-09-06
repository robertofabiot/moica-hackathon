package com.moica.prestador.repository;

import com.moica.prestador.entity.MedioContactoPrestador;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedioContactoPrestadorRepository
    extends JpaRepository<MedioContactoPrestador, Long> {

  /** El desempate por identificador da un orden estable cuando dos filas comparten posición. */
  List<MedioContactoPrestador>
      findByIdPrestadorOrderByOrdenVisualizacionAscIdMedioContactoPrestadorAsc(Long idPrestador);

  /** Buscar por clave y propietario a la vez es lo que impide operar sobre contactos ajenos. */
  Optional<MedioContactoPrestador> findByIdMedioContactoPrestadorAndIdPrestador(
      Long idMedioContactoPrestador, Long idPrestador);
}
