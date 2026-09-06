package com.moica.verificacion.entity;

/**
 * Clase de respaldo que el prestador adjunta al expediente.
 *
 * <p>Los valores son los del dominio {@code TipoDocumentoVerificacion} del diccionario de datos y
 * la restricción {@code ck_documento_verificacion_tipo} los repite en PostgreSQL.
 *
 * <p>El tipo lo declara quien sube el archivo y es lo único que Moica interpreta de su contenido:
 * no hay OCR, ni extracción automática de datos, ni comprobación biométrica. Quien decide si un
 * documento respalda de verdad lo que dice es la persona que revisa.
 */
public enum TipoDocumentoVerificacion {
  /** Documento oficial de identidad de la persona responsable del perfil. */
  IDENTIDAD,
  /** Certificado técnico o profesional que respalda la actividad declarada. */
  CERTIFICACION,
  /** Constancia de experiencia, laboral o de trabajos realizados. */
  CONSTANCIA,
  /** Documento comercial o registro del emprendimiento o la PYME. */
  REGISTRO_NEGOCIO,
  /** Cualquier otro respaldo aportado por el prestador y valorado por la persona revisora. */
  OTRO_RESPALDO;

  /**
   * Si este tipo sirve como respaldo profesional, técnico o comercial.
   *
   * <p>Es lo que exige la verificación profesional además de una básica vigente. La identidad queda
   * fuera a propósito: ya la respalda la básica, y volver a presentarla no aporta trayectoria.
   */
  public boolean esRespaldoProfesional() {
    return this != IDENTIDAD;
  }
}
