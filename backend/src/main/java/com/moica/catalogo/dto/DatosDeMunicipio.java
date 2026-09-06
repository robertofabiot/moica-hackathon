package com.moica.catalogo.dto;

import com.moica.catalogo.entity.Municipio;

/** Vista pública de un municipio dentro de su departamento. */
public record DatosDeMunicipio(Integer idMunicipio, String nombre) {

  public static DatosDeMunicipio de(Municipio municipio) {
    return new DatosDeMunicipio(municipio.getIdMunicipio(), municipio.getNombre());
  }
}
