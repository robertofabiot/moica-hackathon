package com.moica.prestador.dto;

import com.moica.prestador.entity.NivelVerificacionPrestador;
import com.moica.prestador.entity.PerfilPrestador;
import com.moica.prestador.entity.TipoPrestador;

/**
 * Lo mínimo que otra capacidad necesita saber de un perfil ajeno.
 *
 * <p>Existe para que la capacidad {@code verificacion} pueda decidir y mostrar sin llegar al
 * repositorio de {@code prestador} ni recibir la entidad: la comunicación entre capacidades pasa
 * por el {@code service} y lo que cruza esa frontera es un DTO.
 *
 * <p>Es deliberadamente corto. La vista completa del perfil pertenece a su propietario y viaja en
 * {@link DatosDePerfilPrestador}; aquí solo va lo que hace falta para revisar un expediente:
 * identificar el perfil, saber cómo se presenta y cuál es su nivel vigente.
 */
public record ResumenDePerfilPrestador(
    Long idPrestador,
    String nombrePublico,
    TipoPrestador tipoPrestador,
    NivelVerificacionPrestador nivelVerificacion) {

  public static ResumenDePerfilPrestador de(PerfilPrestador perfil) {
    return new ResumenDePerfilPrestador(
        perfil.getIdPrestador(),
        perfil.getNombrePublico(),
        perfil.getTipoPrestador(),
        perfil.getNivelVerificacion());
  }
}
