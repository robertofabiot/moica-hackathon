package com.moica.servicio.entity;

/**
 * Estado de publicación de un servicio.
 *
 * <p>Los valores son los del dominio {@code EstadoServicio} del diccionario de datos y la
 * restricción {@code ck_servicio_publicado_estado} los repite en PostgreSQL.
 *
 * <p>Un servicio no se elimina: se desactiva. Solo {@link #ACTIVO} puede aparecer en el
 * descubrimiento, y únicamente si el prestador está disponible, verificado y su cuenta es
 * operativa.
 */
public enum EstadoServicio {
  /** Puede mostrarse y, si el prestador está disponible, recibir solicitudes. */
  ACTIVO,
  /** No aparece como opción habilitada y no admite nuevas solicitudes. */
  INACTIVO
}
