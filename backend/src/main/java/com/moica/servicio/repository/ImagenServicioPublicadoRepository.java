package com.moica.servicio.repository;

import com.moica.servicio.entity.ImagenServicioPublicado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagenServicioPublicadoRepository
    extends JpaRepository<ImagenServicioPublicado, Long> {

  List<ImagenServicioPublicado>
      findByIdServicioPublicadoOrderByOrdenVisualizacionAscIdImagenServicioPublicadoAsc(
          Long idServicioPublicado);

  Optional<ImagenServicioPublicado> findByIdImagenServicioPublicadoAndIdServicioPublicado(
      Long idImagenServicioPublicado, Long idServicioPublicado);
}
