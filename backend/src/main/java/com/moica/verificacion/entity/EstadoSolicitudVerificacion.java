package com.moica.verificacion.entity;

/**
 * Estado vigente de una solicitud de verificación.
 *
 * <p>Los valores son los del dominio {@code EstadoSolicitudVerificacion} del diccionario de datos y
 * la restricción {@code ck_solicitud_verificacion_estado} los repite en PostgreSQL. No existe
 * {@code BORRADOR}: una solicitud nace {@link #PENDIENTE} con su expediente completo o no nace.
 *
 * <p>Las transiciones permitidas —quién puede llevar de cuál a cuál— no viven aquí sino en {@code
 * RevisionDeVerificacionService}, porque dependen de quién hace la petición y del nivel vigente del
 * perfil, y una fila por sí sola no puede saber eso.
 */
public enum EstadoSolicitudVerificacion {
  /** El expediente fue enviado y espera que un administrador lo tome. */
  PENDIENTE,
  /** Un administrador tomó la solicitud y analiza los documentos presentados. */
  EN_REVISION,
  /** La documentación fue aceptada y el perfil alcanzó el nivel solicitado. */
  APROBADA,
  /** La documentación no fue aceptada; exige observación y el perfil conserva su nivel anterior. */
  RECHAZADA,
  /** Una verificación previamente aprobada quedó sin efecto; exige observación. */
  REVOCADA;

  /** Si la solicitud sigue esperando una decisión. Solo una así bloquea el envío de otra igual. */
  public boolean esAbierto() {
    return this == PENDIENTE || this == EN_REVISION;
  }
}
