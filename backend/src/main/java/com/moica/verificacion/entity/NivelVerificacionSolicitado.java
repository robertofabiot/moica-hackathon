package com.moica.verificacion.entity;

import com.moica.prestador.entity.NivelVerificacionPrestador;

/**
 * Nivel que pretende obtener una solicitud de verificación.
 *
 * <p>Los valores son los del dominio {@code NivelVerificacionSolicitado} del diccionario de datos y
 * la restricción {@code ck_solicitud_verificacion_nivel} los repite en PostgreSQL.
 *
 * <p>Es un dominio distinto de {@link NivelVerificacionPrestador} y no se confunde con él: aquel
 * describe el nivel **vigente** de un perfil e incluye {@code SIN_VERIFICAR}, que no es nada que se
 * pueda solicitar. La correspondencia entre los dos la resuelve {@link #nivelQueProyecta()}.
 */
public enum NivelVerificacionSolicitado {
  /** Verificación básica de identidad; se solicita cuando el perfil está sin verificar. */
  BASICA(NivelVerificacionPrestador.VERIFICADO_BASICO),
  /** Verificación profesional; exige una básica vigente y no la sustituye. */
  PROFESIONAL(NivelVerificacionPrestador.PROFESIONAL_VERIFICADO);

  private final NivelVerificacionPrestador nivelProyectado;

  NivelVerificacionSolicitado(NivelVerificacionPrestador nivelProyectado) {
    this.nivelProyectado = nivelProyectado;
  }

  /** El nivel que queda vigente en el perfil cuando un administrador aprueba esta solicitud. */
  public NivelVerificacionPrestador nivelQueProyecta() {
    return nivelProyectado;
  }
}
