package com.moica.auth.seguridad;

/**
 * Quién hace la petición y con qué sesión.
 *
 * <p>Es el sujeto autenticado que {@link FiltroDeSesion} deja en el contexto de seguridad. Lleva la
 * sesión además de la cuenta porque cerrar sesión revoca exactamente la que se está usando, no
 * todas las de la persona.
 *
 * <p>No guarda nombre ni correo a propósito: esos datos se piden a la capacidad {@code usuario}
 * cuando hacen falta, y así no quedan copiados en el contexto de seguridad.
 */
public record UsuarioAutenticado(Long idUsuario, Long idSesion) {}
