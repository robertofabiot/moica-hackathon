package com.moica.servicio.service;

import com.moica.calificacion.dto.ReputacionPorRol;
import com.moica.calificacion.entity.RolCalificado;
import com.moica.calificacion.service.ReputacionService;
import com.moica.catalogo.dto.ClasificacionDeServicio;
import com.moica.catalogo.service.CatalogoDeServiciosService;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.portafolio.service.TrabajoPortafolioService;
import com.moica.prestador.dto.DatosPublicosDePrestador;
import com.moica.prestador.entity.EstadoDisponibilidad;
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
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura pública de servicios y prestadores.
 *
 * <p>El listado y el detalle solo entregan un servicio {@code ACTIVO} de cuenta {@code ACTIVA},
 * prestador {@code DISPONIBLE} y verificación al menos básica. Un identificador que no cumpla eso
 * responde 404, igual que uno inexistente.
 *
 * <p>El perfil de un prestador verificado con cuenta operativa sigue visible si no está disponible:
 * el portafolio permanece, los servicios no se listan y {@code admiteContratacion} queda en falso.
 *
 * <p>Cada superficie lleva además la reputación real del prestador, que es de la persona y no del
 * servicio concreto. El listado la resuelve <em>en bloque</em>, con una consulta agrupada para
 * todos los prestadores de la página: pedirla tarjeta por tarjeta añadiría una consulta por
 * tarjeta. Quien todavía no tiene calificaciones recibe el agregado vacío, con promedio nulo; no se
 * inventa un cero que rebajaría una nota inexistente.
 */
@Service
public class DescubrimientoDeServiciosService {

  private final ServicioPublicadoRepository servicios;
  private final ImagenServicioPublicadoRepository imagenes;
  private final PerfilPrestadorService perfiles;
  private final CatalogoDeServiciosService catalogo;
  private final TrabajoPortafolioService portafolio;
  private final UsuarioService usuarios;
  private final ReputacionService reputaciones;

  public DescubrimientoDeServiciosService(
      ServicioPublicadoRepository servicios,
      ImagenServicioPublicadoRepository imagenes,
      PerfilPrestadorService perfiles,
      CatalogoDeServiciosService catalogo,
      TrabajoPortafolioService portafolio,
      UsuarioService usuarios,
      ReputacionService reputaciones) {
    this.servicios = servicios;
    this.imagenes = imagenes;
    this.perfiles = perfiles;
    this.catalogo = catalogo;
    this.portafolio = portafolio;
    this.usuarios = usuarios;
    this.reputaciones = reputaciones;
  }

  /** Listado público combinando texto, categoría o subcategoría y municipio. */
  @Transactional(readOnly = true)
  public List<ResumenPublicoDeServicio> buscar(
      String texto, Short idCategoria, Integer idSubcategoria, Integer idMunicipio) {
    List<ServicioPublicado> encontrados =
        servicios.buscarPublicos(textoDeBusqueda(texto), idCategoria, idSubcategoria, idMunicipio);

    // Una sola consulta agrupada para todos los prestadores de la página, antes
    // de armar las tarjetas. Dos servicios del mismo prestador comparten
    // agregado porque la reputación es de la persona.
    Map<Long, ReputacionPorRol> porPrestador =
        reputaciones.reputacionesDePrestadores(
            encontrados.stream().map(ServicioPublicado::getIdPrestador).distinct().toList());

    return encontrados.stream()
        .map(
            servicio ->
                aResumen(
                    servicio,
                    prestadorPublico(servicio),
                    porPrestador.get(servicio.getIdPrestador())))
        .toList();
  }

  /** Detalle público de un servicio visible. */
  @Transactional(readOnly = true)
  public DetallePublicoDeServicio detallar(Long idServicio) {
    ServicioPublicado servicio =
        servicios.buscarPublicoPorId(idServicio).orElseThrow(this::servicioNoEncontrado);
    return DetallePublicoDeServicio.de(
        servicio,
        clasificacionDe(servicio),
        imagenesDe(servicio),
        prestadorPublico(servicio),
        reputaciones.reputacionDe(servicio.getIdPrestador(), RolCalificado.PRESTADOR));
  }

  /**
   * Perfil público de un prestador verificado con cuenta operativa.
   *
   * <p>Si no está disponible, el perfil y el portafolio siguen visibles, los servicios no se listan
   * y {@code admiteContratacion} queda en falso.
   */
  @Transactional(readOnly = true)
  public PerfilPublicoDePrestador perfilPublico(Long idPrestador) {
    DatosPublicosDePrestador prestador =
        perfiles
            .describirPerfilPublicable(idPrestador)
            .filter(datos -> usuarios.esCuentaOperativa(datos.idPrestador()))
            .orElseThrow(this::prestadorNoEncontrado);

    // El perfil y todas sus tarjetas son del mismo prestador, así que el
    // agregado se calcula una vez y se reparte.
    ReputacionPorRol reputacion = reputaciones.reputacionDe(idPrestador, RolCalificado.PRESTADOR);

    List<ResumenPublicoDeServicio> publicados =
        prestador.disponibilidad() == EstadoDisponibilidad.DISPONIBLE
            ? servicios
                .findByIdPrestadorAndEstadoOrderByNombreAscIdServicioPublicadoAsc(
                    idPrestador, EstadoServicio.ACTIVO)
                .stream()
                .map(servicio -> aResumen(servicio, prestador, reputacion))
                .toList()
            : List.of();

    return PerfilPublicoDePrestador.de(
        prestador, portafolio.listarPublicos(idPrestador), publicados, reputacion);
  }

  private ResumenPublicoDeServicio aResumen(
      ServicioPublicado servicio, DatosPublicosDePrestador prestador, ReputacionPorRol reputacion) {
    return ResumenPublicoDeServicio.de(
        servicio, clasificacionDe(servicio), imagenesDe(servicio), prestador, reputacion);
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
