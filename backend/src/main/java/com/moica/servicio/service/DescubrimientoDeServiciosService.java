package com.moica.servicio.service;

import com.moica.catalogo.dto.ClasificacionDeServicio;
import com.moica.catalogo.service.CatalogoDeServiciosService;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.portafolio.service.TrabajoPortafolioService;
import com.moica.prestador.dto.DatosPublicosDePrestador;
import com.moica.prestador.service.PerfilPrestadorService;
import com.moica.servicio.dto.DatosDeImagenDeServicio;
import com.moica.servicio.dto.DetallePublicoDeServicio;
import com.moica.servicio.dto.PerfilPublicoDePrestador;
import com.moica.servicio.dto.ResumenPublicoDeServicio;
import com.moica.servicio.entity.EstadoServicio;
import com.moica.servicio.entity.ServicioPublicado;
import com.moica.servicio.repository.ImagenServicioPublicadoRepository;
import com.moica.servicio.repository.ServicioPublicadoRepository;
import com.moica.usuario.service.UsuarioService;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura pública de servicios y prestadores.
 *
 * <p>Solo entrega lo que un visitante puede ver: servicio {@code ACTIVO}, cuenta {@code ACTIVA},
 * prestador {@code DISPONIBLE} y verificación al menos básica. Un identificador que no cumpla eso
 * responde 404, igual que uno inexistente.
 */
@Service
public class DescubrimientoDeServiciosService {

  private final ServicioPublicadoRepository servicios;
  private final ImagenServicioPublicadoRepository imagenes;
  private final PerfilPrestadorService perfiles;
  private final CatalogoDeServiciosService catalogo;
  private final TrabajoPortafolioService portafolio;
  private final UsuarioService usuarios;

  public DescubrimientoDeServiciosService(
      ServicioPublicadoRepository servicios,
      ImagenServicioPublicadoRepository imagenes,
      PerfilPrestadorService perfiles,
      CatalogoDeServiciosService catalogo,
      TrabajoPortafolioService portafolio,
      UsuarioService usuarios) {
    this.servicios = servicios;
    this.imagenes = imagenes;
    this.perfiles = perfiles;
    this.catalogo = catalogo;
    this.portafolio = portafolio;
    this.usuarios = usuarios;
  }

  /** Listado público combinando texto, categoría o subcategoría y municipio. */
  @Transactional(readOnly = true)
  public List<ResumenPublicoDeServicio> buscar(
      String texto, Short idCategoria, Integer idSubcategoria, Integer idMunicipio) {
    return servicios
        .buscarPublicos(textoDeBusqueda(texto), idCategoria, idSubcategoria, idMunicipio)
        .stream()
        .map(this::aResumen)
        .toList();
  }

  /** Detalle público de un servicio visible. */
  @Transactional(readOnly = true)
  public DetallePublicoDeServicio detallar(Long idServicio) {
    ServicioPublicado servicio =
        servicios.buscarPublicoPorId(idServicio).orElseThrow(this::servicioNoEncontrado);
    return DetallePublicoDeServicio.de(
        servicio, clasificacionDe(servicio), imagenesDe(servicio), prestadorPublico(servicio));
  }

  /**
   * Perfil público de un prestador verificado con cuenta operativa.
   *
   * <p>Si no está disponible, el perfil y el portafolio siguen visibles y los servicios activos se
   * listan, pero {@code admiteContratacion} queda en falso.
   */
  @Transactional(readOnly = true)
  public PerfilPublicoDePrestador perfilPublico(Long idPrestador) {
    DatosPublicosDePrestador prestador =
        perfiles
            .describirPerfilPublicable(idPrestador)
            .filter(datos -> usuarios.esCuentaOperativa(datos.idPrestador()))
            .orElseThrow(this::prestadorNoEncontrado);

    List<ResumenPublicoDeServicio> publicados =
        servicios
            .findByIdPrestadorAndEstadoOrderByNombreAscIdServicioPublicadoAsc(
                idPrestador, EstadoServicio.ACTIVO)
            .stream()
            .map(servicio -> aResumen(servicio, prestador))
            .toList();

    return PerfilPublicoDePrestador.de(
        prestador, portafolio.listarPublicos(idPrestador), publicados);
  }

  private ResumenPublicoDeServicio aResumen(ServicioPublicado servicio) {
    return aResumen(servicio, prestadorPublico(servicio));
  }

  private ResumenPublicoDeServicio aResumen(
      ServicioPublicado servicio, DatosPublicosDePrestador prestador) {
    return ResumenPublicoDeServicio.de(
        servicio, clasificacionDe(servicio), imagenesDe(servicio), prestador);
  }

  private DatosPublicosDePrestador prestadorPublico(ServicioPublicado servicio) {
    return perfiles
        .describirPerfilPublicable(servicio.getIdPrestador())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "El servicio "
                        + servicio.getIdServicioPublicado()
                        + " referencia un perfil que no es publicable"));
  }

  private ClasificacionDeServicio clasificacionDe(ServicioPublicado servicio) {
    return catalogo
        .describirSubcategoria(servicio.getIdSubcategoriaServicio())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "El servicio "
                        + servicio.getIdServicioPublicado()
                        + " referencia una subcategoría que no existe"));
  }

  private List<DatosDeImagenDeServicio> imagenesDe(ServicioPublicado servicio) {
    return imagenes
        .findByIdServicioPublicadoOrderByOrdenVisualizacionAscIdImagenServicioPublicadoAsc(
            servicio.getIdServicioPublicado())
        .stream()
        .map(DatosDeImagenDeServicio::de)
        .toList();
  }

  private static String textoDeBusqueda(String texto) {
    if (texto == null) {
      return null;
    }
    String normalizado = texto.strip().toLowerCase(Locale.ROOT);
    if (normalizado.isEmpty()) {
      return null;
    }
    return "%" + normalizado + "%";
  }

  private ErrorDeAplicacion servicioNoEncontrado() {
    return new ErrorDeAplicacion(
        HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "Ese servicio no existe.");
  }

  private ErrorDeAplicacion prestadorNoEncontrado() {
    return new ErrorDeAplicacion(
        HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "Ese prestador no existe.");
  }
}
