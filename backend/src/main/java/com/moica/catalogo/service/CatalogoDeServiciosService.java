package com.moica.catalogo.service;

import com.moica.catalogo.dto.ClasificacionDeServicio;
import com.moica.catalogo.dto.DatosDeCategoriaServicio;
import com.moica.catalogo.dto.DatosDeSubcategoriaServicio;
import com.moica.catalogo.entity.CategoriaServicio;
import com.moica.catalogo.repository.CategoriaServicioRepository;
import com.moica.catalogo.repository.SubcategoriaServicioRepository;
import com.moica.comun.error.ErrorDeAplicacion;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lecturas del catálogo de categorías de servicio.
 *
 * <p>El catálogo no se administra desde la API: sus filas llegan por migraciones versionadas. Este
 * servicio solo publica la taxonomía y describe una subcategoría ya elegida.
 */
@Service
public class CatalogoDeServiciosService {

  private final CategoriaServicioRepository categorias;
  private final SubcategoriaServicioRepository subcategorias;

  public CatalogoDeServiciosService(
      CategoriaServicioRepository categorias, SubcategoriaServicioRepository subcategorias) {
    this.categorias = categorias;
    this.subcategorias = subcategorias;
  }

  /** Categorías con sus subcategorías, en orden alfabético determinista. */
  @Transactional(readOnly = true)
  public List<DatosDeCategoriaServicio> categorias() {
    return categorias.findAllByOrderByNombreAscIdCategoriaServicioAsc().stream()
        .map(this::conSubcategorias)
        .toList();
  }

  /**
   * Describe una subcategoría concreta con su categoría.
   *
   * <p>Devuelve vacío cuando no existe. Quien publica un servicio decide cómo responder.
   */
  @Transactional(readOnly = true)
  public Optional<ClasificacionDeServicio> describirSubcategoria(Integer idSubcategoriaServicio) {
    return subcategorias
        .findById(idSubcategoriaServicio)
        .flatMap(
            subcategoria ->
                categorias
                    .findById(subcategoria.getIdCategoriaServicio())
                    .map(categoria -> ClasificacionDeServicio.de(categoria, subcategoria)));
  }

  /**
   * La subcategoría elegida al publicar o editar un servicio.
   *
   * @throws ErrorDeAplicacion si no existe
   */
  @Transactional(readOnly = true)
  public ClasificacionDeServicio exigirSubcategoria(Integer idSubcategoriaServicio) {
    return describirSubcategoria(idSubcategoriaServicio)
        .orElseThrow(
            () ->
                new ErrorDeAplicacion(
                    HttpStatus.BAD_REQUEST,
                    "SUBCATEGORIA_NO_DISPONIBLE",
                    "La subcategoría elegida no está disponible en Moica."));
  }

  private DatosDeCategoriaServicio conSubcategorias(CategoriaServicio categoria) {
    return DatosDeCategoriaServicio.de(
        categoria,
        subcategorias
            .findByIdCategoriaServicioOrderByNombreAscIdSubcategoriaServicioAsc(
                categoria.getIdCategoriaServicio())
            .stream()
            .map(DatosDeSubcategoriaServicio::de)
            .toList());
  }
}
