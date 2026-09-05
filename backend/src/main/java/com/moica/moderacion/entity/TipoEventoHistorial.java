package com.moica.moderacion.entity;

/**
 * Motivo por el que se creó una versión del historial de un caso.
 *
 * <p>Los valores son los del dominio {@code TipoEventoHistorial} del diccionario de datos y la
 * restricción {@code ck_historial_caso_tipo_evento} los repite en PostgreSQL. El enumerado los
 * declara completos porque la columna admite los doce: una entidad que solo conociera algunos no
 * podría leer una fila escrita por otro incremento.
 *
 * <p>Reportar escribe {@link #CASO_ABIERTO}. La revisión administrativa escribe {@link
 * #RESPONSABLE_ASIGNADO}, {@link #ESTADO_CASO_CAMBIADO} y {@link #RESOLUCION_REGISTRADA}. Las
 * medidas y las apelaciones escriben {@link #MEDIDA_APLICADA}, {@link #MEDIDA_REVOCADA}, {@link
 * #APELACION_PRESENTADA}, {@link #APELACION_ACEPTADA}, {@link #APELACION_RECHAZADA} y {@link
 * #CASO_REABIERTO}. {@link #MEDIDA_EXPIRADA} es el único que no origina ninguna persona: lo escribe
 * el barrido que cumple el plazo de una medida temporal que sí decidió alguien.
 *
 * <p>{@link #ESTADO_CUENTA_CAMBIADO} no lo escribe nadie. El estado de la cuenta viaja como columna
 * en <em>todas</em> las versiones, así que cada evento de medida ya retrata en qué quedó el acceso.
 * Un evento aparte solo tendría sentido si el estado pudiera moverse sin que ninguna medida lo
 * moviera, y en el MVP no puede: la moderación es su única causa. El valor se conserva porque el
 * dominio del diccionario lo declara y {@code ck_historial_caso_tipo_evento} lo admite.
 */
public enum TipoEventoHistorial {
  /** Se creó el caso y su primera versión. */
  CASO_ABIERTO,
  /** Se asignó o reasignó el administrador responsable. */
  RESPONSABLE_ASIGNADO,
  /** Cambió la etapa vigente del caso. */
  ESTADO_CASO_CAMBIADO,
  /** Se registró una nueva resolución del caso. */
  RESOLUCION_REGISTRADA,
  /** Se aplicó una medida administrativa a la cuenta. */
  MEDIDA_APLICADA,
  /** Se dejó sin efecto una medida administrativa anterior. */
  MEDIDA_REVOCADA,
  /** Finalizó automáticamente la vigencia de una medida temporal. */
  MEDIDA_EXPIRADA,
  /** Cambió el estado operativo de la cuenta afectada. */
  ESTADO_CUENTA_CAMBIADO,
  /** El usuario presentó una apelación contra una decisión vigente. */
  APELACION_PRESENTADA,
  /** La apelación fue aceptada y puede originar reapertura o reversión. */
  APELACION_ACEPTADA,
  /** La apelación fue evaluada y la decisión vigente se mantuvo. */
  APELACION_RECHAZADA,
  /** El caso cerrado volvió formalmente a revisión. */
  CASO_REABIERTO
}
