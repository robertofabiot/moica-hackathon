package com.moica.verificacion.dto;

import com.moica.prestador.entity.NivelVerificacionPrestador;
import com.moica.prestador.entity.TipoPrestador;
import com.moica.verificacion.entity.EstadoSolicitudVerificacion;
import com.moica.verificacion.entity.NivelVerificacionSolicitado;
import com.moica.verificacion.entity.SolicitudVerificacionPrestador;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Una solicitud tal como la ve la persona que revisa.
 *
 * <p>Añade sobre la vista del propietario lo que hace falta para decidir: a qué cuenta pertenece el
 * perfil, qué nivel tiene hoy y qué administrador tiene asignada la revisión. Nada de esto sale
 * nunca de {@code /api/admin}: la cadena de seguridad exige rol administrativo y segundo factor
 * verificado en esa misma sesión.
 *
 * <p>Sigue sin llevar claves de almacenamiento ni URL de archivos. El binario se abre con el
 * endpoint de acceso temporal, uno por documento y por petición.
 *
 * @param idAdministradorRevisor quién tiene asignada la revisión, o {@code null} mientras está
 *     pendiente. Solo el asignado puede aprobar o rechazar
 */
public record DatosDeExpediente(
    Long idSolicitudVerificacion,
    PrestadorDelExpediente prestador,
    NivelVerificacionSolicitado nivelSolicitado,
    EstadoSolicitudVerificacion estadoSolicitud,
    String observacionResolucion,
    Long idAdministradorRevisor,
    OffsetDateTime fechaSolicitud,
    OffsetDateTime fechaInicioRevision,
    OffsetDateTime fechaResolucion,
    List<DatosDeDocumentoVerificacion> documentos) {

  /**
   * Quién presenta el expediente.
   *
   * <p>Lleva el nombre y el correo de la cuenta porque revisar un documento de identidad consiste
   * justamente en contrastarlo con la persona que dice ser. Es información administrativa y no se
   * publica en ninguna superficie pública.
   */
  public record PrestadorDelExpediente(
      Long idPrestador,
      String nombrePublico,
      TipoPrestador tipoPrestador,
      NivelVerificacionPrestador nivelVerificacion,
      String nombreCompleto,
      String correoElectronico) {}

  public DatosDeExpediente {
    documentos = List.copyOf(documentos);
  }

  public static DatosDeExpediente de(
      SolicitudVerificacionPrestador solicitud,
      PrestadorDelExpediente prestador,
      List<DatosDeDocumentoVerificacion> documentos) {
    return new DatosDeExpediente(
        solicitud.getIdSolicitudVerificacion(),
        prestador,
        solicitud.getNivelSolicitado(),
        solicitud.getEstadoSolicitud(),
        solicitud.getObservacionResolucion(),
        solicitud.getIdAdministradorRevisor(),
        solicitud.getFechaSolicitud(),
        solicitud.getFechaInicioRevision(),
        solicitud.getFechaResolucion(),
        documentos);
  }
}
