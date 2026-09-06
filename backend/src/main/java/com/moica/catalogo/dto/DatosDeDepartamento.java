package com.moica.catalogo.dto;

import com.moica.catalogo.entity.Departamento;
import java.util.List;

/**
 * Departamento habilitado con sus municipios, tal como lo consume un formulario.
 *
 * <p>Solo se publican departamentos habilitados: los demás existen en el catálogo para conservar la
 * estructura ampliable, pero no son una opción válida mientras Moica no opere en ellos.
 */
public record DatosDeDepartamento(
    Short idDepartamento, String nombre, List<DatosDeMunicipio> municipios) {

  public DatosDeDepartamento {
    municipios = List.copyOf(municipios);
  }

  public static DatosDeDepartamento de(
      Departamento departamento, List<DatosDeMunicipio> municipios) {
    return new DatosDeDepartamento(
        departamento.getIdDepartamento(), departamento.getNombre(), municipios);
  }
}
