package com.moica.verificacion.repository;

import com.moica.verificacion.entity.DocumentoVerificacionPrestador;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoVerificacionPrestadorRepository
    extends JpaRepository<DocumentoVerificacionPrestador, Long> {

  /** El expediente de una solicitud, en el orden en que se adjuntó. */
  List<DocumentoVerificacionPrestador>
      findByIdSolicitudVerificacionOrderByIdDocumentoVerificacionAsc(Long idSolicitudVerificacion);

  /**
   * Los expedientes de varias solicitudes en una sola consulta.
   *
   * <p>El historial y la cola muestran cada solicitud con sus documentos; pedirlos uno a uno haría
   * tantas consultas como filas tenga la lista.
   */
  List<DocumentoVerificacionPrestador>
      findByIdSolicitudVerificacionInOrderByIdDocumentoVerificacionAsc(
          Collection<Long> idsSolicitudVerificacion);

  /**
   * Buscar por clave y solicitud a la vez es lo que impide abrir un documento de otro expediente.
   */
  Optional<DocumentoVerificacionPrestador> findByIdDocumentoVerificacionAndIdSolicitudVerificacion(
      Long idDocumentoVerificacion, Long idSolicitudVerificacion);
}
