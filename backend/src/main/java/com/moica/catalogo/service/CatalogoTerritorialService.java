package com.moica.catalogo.service;

import com.moica.catalogo.dto.DatosDeDepartamento;
import com.moica.catalogo.dto.DatosDeMunicipio;
import com.moica.catalogo.dto.UbicacionDeMunicipio;
import com.moica.catalogo.entity.Municipio;
import com.moica.catalogo.repository.DepartamentoRepository;
import com.moica.catalogo.repository.MunicipioRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lecturas del catálogo territorial.
 *
 * <p>El catálogo no se administra desde la API: sus filas llegan por migraciones versionadas. Este
 * servicio solo publica lo habilitado y describe municipios ya elegidos, que es todo lo que el
 * resto de la aplicación necesita.
 */
@Service
public class CatalogoTerritorialService {

  private final DepartamentoRepository departamentos;
  private final MunicipioRepository municipios;

  public CatalogoTerritorialService(
      DepartamentoRepository departamentos, MunicipioRepository municipios) {
    this.departamentos = departamentos;
    this.municipios = municipios;
  }

  /** Departamentos donde Moica opera, cada uno con sus municipios en orden alfabético. */
  @Transactional(readOnly = true)
  public List<DatosDeDepartamento> departamentosHabilitados() {
    return departamentos.findByHabilitadoTrueOrderByNombre().stream()
        .map(
            departamento ->
                DatosDeDepartamento.de(
                    departamento,
                    municipios
                        .findByIdDepartamentoOrderByNombre(departamento.getIdDepartamento())
                        .stream()
                        .map(DatosDeMunicipio::de)
                        .toList()))
        .toList();
  }

  /**
   * Describe un municipio concreto, exista o no en un departamento habilitado.
   *
   * <p>Devuelve vacío cuando el municipio no existe. Quien valida decide qué hacer con un
   * departamento deshabilitado: para el catálogo público no es una opción, pero un perfil antiguo
   * podría seguir mostrándolo si el equipo deshabilitara un departamento en el futuro.
   */
  @Transactional(readOnly = true)
  public Optional<UbicacionDeMunicipio> describirMunicipio(Integer idMunicipio) {
    return municipios.findById(idMunicipio).map(this::aUbicacion);
  }

  private UbicacionDeMunicipio aUbicacion(Municipio municipio) {
    return departamentos
        .findById(municipio.getIdDepartamento())
        .map(
            departamento ->
                new UbicacionDeMunicipio(
                    municipio.getIdMunicipio(),
                    municipio.getNombre(),
                    departamento.getNombre(),
                    departamento.isHabilitado()))
        // La clave foránea garantiza el departamento; si faltara, el esquema
        // estaría corrupto y ocultarlo sería peor que fallar.
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "El municipio " + municipio.getIdMunicipio() + " no tiene departamento"));
  }
}
