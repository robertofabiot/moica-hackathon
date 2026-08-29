package com.moica.servicio.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.catalogo.dto.ClasificacionDeServicio;
import com.moica.catalogo.service.CatalogoDeServiciosService;
import com.moica.comun.dto.SolicitudDeOrden;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.portafolio.dto.SolicitudDeTextoAlternativo;
import com.moica.prestador.dto.CondicionDePublicacion;
import com.moica.prestador.service.PerfilPrestadorService;
import com.moica.servicio.dto.DatosDeImagenDeServicio;
import com.moica.servicio.dto.DatosDeServicioPublicado;
import com.moica.servicio.dto.SolicitudDeEstadoDeServicio;
import com.moica.servicio.dto.SolicitudDeServicio;
import com.moica.servicio.entity.EstadoServicio;
import com.moica.servicio.entity.ImagenServicioPublicado;
import com.moica.servicio.entity.ServicioPublicado;
import com.moica.servicio.repository.ImagenServicioPublicadoRepository;
import com.moica.servicio.repository.ServicioPublicadoRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas y persistencia de los servicios propios.
 *
 * <p>Toda consulta lleva la cadena de propiedad —servicio por prestador, imagen por servicio—, de
 * modo que un recurso ajeno no se encuentra y se responde como inexistente. Crear deja el servicio
 * {@code INACTIVO}. Activar bloquea el perfil para no competir con un cambio de disponibilidad o
 * una revocación.
 *
 * <p>Los objetos del almacén no se tocan aquí: los métodos que dejan filas sin imagen devuelven las
 * URL desreferenciadas y {@link ImagenDeServicioService} retira los objetos cuando la transacción
 * ya quedó consistente.
 */
@Service
public class ServicioPublicadoService {

  private final ServicioPublicadoRepository servicios;
  private final ImagenServicioPublicadoRepository imagenes;
  private final PerfilPrestadorService perfiles;
  private final CatalogoDeServiciosService catalogo;

  public ServicioPublicadoService(
      ServicioPublicadoRepository servicios,
      ImagenServicioPublicadoRepository imagenes,
      PerfilPrestadorService perfiles,
      CatalogoDeServiciosService catalogo) {
    this.servicios = servicios;
    this.imagenes = imagenes;
    this.perfiles = perfiles;
    this.catalogo = catalogo;
  }

  /** Los servicios propios, activos e inactivos, en orden determinista. */
  @Transactional(readOnly = true)
  public List<DatosDeServicioPublicado> listar(UsuarioAutenticado sujeto) {
    perfiles.exigirQueExistaElPerfil(sujeto.idUsuario());
    return serviciosDe(sujeto).stream().map(this::aDatos).toList();
  }

  /** Un servicio propio. */
  @Transactional(readOnly = true)
  public DatosDeServicioPublicado consultar(UsuarioAutenticado sujeto, Long idServicio) {
    perfiles.exigirQueExistaElPerfil(sujeto.idUsuario());
    return aDatos(servicioPropio(sujeto, idServicio));
  }

  /**
   * Prepara un servicio propio. Nace inactivo, aunque el perfil ya esté verificado: activar es una
   * decisión posterior.
   */
  @Transactional
  public DatosDeServicioPublicado crear(UsuarioAutenticado sujeto, SolicitudDeServicio solicitud) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);
    ClasificacionDeServicio clasificacion =
        catalogo.exigirSubcategoria(solicitud.idSubcategoriaServicio());

    ServicioPublicado servicio =
        new ServicioPublicado(
            sujeto.idUsuario(),
            clasificacion.idSubcategoriaServicio(),
            solicitud.nombre(),
            solicitud.descripcion(),
            solicitud.precioReferencia());

    return aDatos(servicios.save(servicio));
  }

  /** Sustituye nombre, descripción, subcategoría y precio de un servicio propio. */
  @Transactional
  public DatosDeServicioPublicado actualizar(
      UsuarioAutenticado sujeto, Long idServicio, SolicitudDeServicio solicitud) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    ServicioPublicado servicio = servicioPropio(sujeto, idServicio);
    ClasificacionDeServicio clasificacion =
        catalogo.exigirSubcategoria(solicitud.idSubcategoriaServicio());

    servicio.actualizar(
        clasificacion.idSubcategoriaServicio(),
        solicitud.nombre(),
        solicitud.descripcion(),
        solicitud.precioReferencia());

    return aDatos(servicio);
  }

  /**
   * Activa o desactiva un servicio propio.
   *
   * <p>Desactivar no exige verificación ni disponibilidad. Activar sí: cuenta activa (ya comprobada
   * al exigir modificar), prestador disponible y al menos verificación básica, leídos con el perfil
   * bloqueado.
   */
  @Transactional
  public DatosDeServicioPublicado cambiarEstado(
      UsuarioAutenticado sujeto, Long idServicio, SolicitudDeEstadoDeServicio solicitud) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    ServicioPublicado servicio = servicioPropio(sujeto, idServicio);

    if (solicitud.estado() == EstadoServicio.ACTIVO) {
      CondicionDePublicacion condicion = perfiles.bloquearParaPublicarServicio(sujeto.idUsuario());
      if (!condicion.tieneVerificacionBasica()) {
        throw new ErrorDeAplicacion(
            HttpStatus.CONFLICT,
            "VERIFICACION_BASICA_REQUERIDA",
            "Necesitas la verificación básica para activar un servicio.");
      }
      if (!condicion.estaDisponible()) {
        throw new ErrorDeAplicacion(
            HttpStatus.CONFLICT,
            "PRESTADOR_NO_DISPONIBLE",
            "No puedes activar un servicio mientras tu perfil está no disponible.");
      }
    }

    servicio.cambiarEstado(solicitud.estado());
    return aDatos(servicio);
  }

  /** Registra la fila de una imagen ya subida al almacén, al final de las del servicio. */
  @Transactional
  public DatosDeImagenDeServicio registrarImagen(
      UsuarioAutenticado sujeto, Long idServicio, String urlImagen, String textoAlternativo) {
    ServicioPublicado servicio = servicioPropio(sujeto, idServicio);

    List<ImagenServicioPublicado> existentes = imagenesDe(servicio.getIdServicioPublicado());
    short siguienteOrden =
        existentes.isEmpty()
            ? 0
            : (short) (existentes.get(existentes.size() - 1).getOrdenVisualizacion() + 1);

    ImagenServicioPublicado imagen =
        new ImagenServicioPublicado(
            servicio.getIdServicioPublicado(), urlImagen, textoAlternativo, siguienteOrden);

    return DatosDeImagenDeServicio.de(imagenes.save(imagen));
  }

  /** Sustituye el texto alternativo de una imagen propia. */
  @Transactional
  public DatosDeImagenDeServicio actualizarTextoAlternativo(
      UsuarioAutenticado sujeto,
      Long idServicio,
      Long idImagen,
      SolicitudDeTextoAlternativo solicitud) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    ImagenServicioPublicado imagen = imagenPropia(sujeto, idServicio, idImagen);
    imagen.cambiarTextoAlternativo(solicitud.textoAlternativo());

    return DatosDeImagenDeServicio.de(imagen);
  }

  /** Deja las imágenes de un servicio propio en el orden pedido. */
  @Transactional
  public List<DatosDeImagenDeServicio> reordenarImagenes(
      UsuarioAutenticado sujeto, Long idServicio, SolicitudDeOrden orden) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    ServicioPublicado servicio = servicioPropio(sujeto, idServicio);
    Map<Long, ImagenServicioPublicado> porId =
        imagenesDe(servicio.getIdServicioPublicado()).stream()
            .collect(
                Collectors.toMap(
                    ImagenServicioPublicado::getIdImagenServicioPublicado, Function.identity()));

    orden.exigirExactamente(porId.keySet());

    short posicion = 0;
    for (Long id : orden.idsEnOrden()) {
      porId.get(id).cambiarOrdenVisualizacion(posicion);
      posicion++;
    }

    return orden.idsEnOrden().stream().map(porId::get).map(DatosDeImagenDeServicio::de).toList();
  }

  /** Elimina la fila de una imagen propia y devuelve su URL desreferenciada. */
  @Transactional
  public String eliminarFilaDeImagen(UsuarioAutenticado sujeto, Long idServicio, Long idImagen) {
    ImagenServicioPublicado imagen = imagenPropia(sujeto, idServicio, idImagen);
    String url = imagen.getUrlImagen();
    imagenes.delete(imagen);
    return url;
  }

  /**
   * Exige que el servicio exista, sea del sujeto y su cuenta pueda modificarlo.
   *
   * <p>Lo usa la orquestación de imágenes antes de subir un objeto.
   */
  @Transactional(readOnly = true)
  public void exigirQuePuedaModificarElServicio(UsuarioAutenticado sujeto, Long idServicio) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);
    servicioPropio(sujeto, idServicio);
  }

  private List<ServicioPublicado> serviciosDe(UsuarioAutenticado sujeto) {
    return servicios.findByIdPrestadorOrderByNombreAscIdServicioPublicadoAsc(sujeto.idUsuario());
  }

  private List<ImagenServicioPublicado> imagenesDe(Long idServicio) {
    return imagenes
        .findByIdServicioPublicadoOrderByOrdenVisualizacionAscIdImagenServicioPublicadoAsc(
            idServicio);
  }

  private ServicioPublicado servicioPropio(UsuarioAutenticado sujeto, Long idServicio) {
    return servicios
        .findByIdServicioPublicadoAndIdPrestador(idServicio, sujeto.idUsuario())
        .orElseThrow(ServicioPublicadoService::servicioNoEncontrado);
  }

  private ImagenServicioPublicado imagenPropia(
      UsuarioAutenticado sujeto, Long idServicio, Long idImagen) {
    servicioPropio(sujeto, idServicio);
    return imagenes
        .findByIdImagenServicioPublicadoAndIdServicioPublicado(idImagen, idServicio)
        .orElseThrow(
            () ->
                new ErrorDeAplicacion(
                    HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "Esa imagen no existe."));
  }

  private DatosDeServicioPublicado aDatos(ServicioPublicado servicio) {
    ClasificacionDeServicio clasificacion =
        catalogo
            .describirSubcategoria(servicio.getIdSubcategoriaServicio())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "El servicio "
                            + servicio.getIdServicioPublicado()
                            + " referencia una subcategoría que no existe"));
    return DatosDeServicioPublicado.de(
        servicio,
        clasificacion,
        imagenesDe(servicio.getIdServicioPublicado()).stream()
            .map(DatosDeImagenDeServicio::de)
            .toList());
  }

  private static ErrorDeAplicacion servicioNoEncontrado() {
    return new ErrorDeAplicacion(
        HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "Ese servicio no existe.");
  }
}
