package com.moica.chat.repository;

import com.moica.chat.entity.MensajeSolicitud;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeSolicitudRepository extends JpaRepository<MensajeSolicitud, Long> {

  /**
   * El hilo completo de una solicitud, en orden cronológico.
   *
   * <p>El desempate por identificador da un orden estable cuando dos mensajes comparten instante;
   * el índice {@code ix_mensaje_solicitud_id_solicitud} está declarado con esas mismas columnas.
   */
  List<MensajeSolicitud> findByIdSolicitudServicioOrderByFechaEnvioAscIdMensajeSolicitudAsc(
      Long idSolicitudServicio);
}
