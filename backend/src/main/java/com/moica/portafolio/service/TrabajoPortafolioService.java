package com.moica.portafolio.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.dto.SolicitudDeOrden;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.portafolio.dto.DatosDeImagenDeTrabajo;
import com.moica.portafolio.dto.DatosDeTrabajo;
import com.moica.portafolio.dto.SolicitudDeTextoAlternativo;
import com.moica.portafolio.dto.SolicitudDeTrabajo;
import com.moica.portafolio.entity.ImagenTrabajoPortafolio;
import com.moica.portafolio.entity.TrabajoPortafolio;
import com.moica.portafolio.repository.ImagenTrabajoPortafolioRepository;
import com.moica.portafolio.repository.TrabajoPortafolioRepository;
import com.moica.prestador.service.PerfilPrestadorService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas y persistencia de los trabajos del portafolio propio.
 *
 * <p>El portafolio lo administra manualmente el prestador; nada lo alimenta con servicios
 * completados. Toda consulta lleva la cadena de propiedad completa —trabajo por prestador, imagen
 * por trabajo—, de modo que un recurso ajeno no se encuentra y se responde como inexistente.
 *
 * <p>Los objetos del almacén no se tocan aquí: los métodos que dejan filas sin imagen devuelven las
 * URL desreferenciadas y {@link ImagenDeTrabajoService} retira los objetos cuando la transacción ya
 * quedó consistente.
 */
@Service
public class TrabajoPortafolioService {

  private final TrabajoPortafolioRepository trabajos;
  private final ImagenTrabajoPortafolioRepository imagenes;
  private final PerfilPrestadorService perfiles;

  public TrabajoPortafolioService(
      TrabajoPortafolioRepository trabajos,
      ImagenTrabajoPortafolioRepository imagenes,
      PerfilPrestadorService perfiles) {
    this.trabajos = trabajos;
    this.imagenes = imagenes;
    this.perfiles = perfiles;
  }

  /**
   * Los trabajos públicos de un prestador, en su orden.
   *
   * <p>No comprueba visibilidad: eso lo decide el descubrimiento, que es quien sabe si el perfil
   * puede mostrarse. Aquí solo se lee lo que cuelga del identificador.
   */
  @Transactional(readOnly = true)
  public List<DatosDeTrabajo> listarPublicos(Long idPrestador) {
    return trabajos.findByIdPrestadorOrderByOrdenVisualizacionAscIdTrabajoAsc(idPrestador).stream()
        .map(this::aDatos)
        .toList();
  }

  /** Los trabajos propios en su orden, cada uno con sus imágenes en el suyo. */
  @Transactional(readOnly = true)
  public List<DatosDeTrabajo> listar(UsuarioAutenticado sujeto) {
    perfiles.exigirQueExistaElPerfil(sujeto.idUsuario());
    return trabajosDe(sujeto).stream().map(this::aDatos).toList();
  }

  /** Agrega un trabajo al final del portafolio. */
  @Transactional
  public DatosDeTrabajo crear(UsuarioAutenticado sujeto, SolicitudDeTrabajo solicitud) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    List<TrabajoPortafolio> existentes = trabajosDe(sujeto);
    short siguienteOrden =
        existentes.isEmpty()
            ? 0
            : (short) (existentes.get(existentes.size() - 1).getOrdenVisualizacion() + 1);

    TrabajoPortafolio trabajo =
        new TrabajoPortafolio(
            sujeto.idUsuario(),
            solicitud.titulo(),
            solicitud.descripcion(),
            solicitud.fechaRealizacion(),
            siguienteOrden);

    return aDatos(trabajos.save(trabajo));
  }

  /** Sustituye título, descripción y fecha de un trabajo propio. */
  @Transactional
  public DatosDeTrabajo actualizar(
      UsuarioAutenticado sujeto, Long idTrabajo, SolicitudDeTrabajo solicitud) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    TrabajoPortafolio trabajo = trabajoPropio(sujeto, idTrabajo);
    trabajo.actualizar(solicitud.titulo(), solicitud.descripcion(), solicitud.fechaRealizacion());

    return aDatos(trabajo);
  }

  /** Deja los trabajos en el orden pedido, con la lista completa de identificadores. */
  @Transactional
  public List<DatosDeTrabajo> reordenar(UsuarioAutenticado sujeto, SolicitudDeOrden orden) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    Map<Long, TrabajoPortafolio> porId =
        trabajosDe(sujeto).stream()
            .collect(Collectors.toMap(TrabajoPortafolio::getIdTrabajo, Function.identity()));

    orden.exigirExactamente(porId.keySet());

    short posicion = 0;
    for (Long id : orden.idsEnOrden()) {
      porId.get(id).cambiarOrdenVisualizacion(posicion);
      posicion++;
    }

    return orden.idsEnOrden().stream().map(porId::get).map(this::aDatos).toList();
  }

  /**
   * Elimina un trabajo propio con sus filas de imagen y devuelve las URL desreferenciadas.
   *
   * <p>Las filas se borran explícitamente en la misma transacción; la clave foránea en cascada
   * queda como red de seguridad, no como mecanismo.
   */
  @Transactional
  public List<String> eliminar(UsuarioAutenticado sujeto, Long idTrabajo) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    TrabajoPortafolio trabajo = trabajoPropio(sujeto, idTrabajo);
    List<ImagenTrabajoPortafolio> deLaFila = imagenesDe(trabajo.getIdTrabajo());
    List<String> urls = deLaFila.stream().map(ImagenTrabajoPortafolio::getUrlImagen).toList();

    imagenes.deleteAll(deLaFila);
    trabajos.delete(trabajo);

    return urls;
  }

  /** Registra la fila de una imagen ya subida al almacén, al final de las del trabajo. */
  @Transactional
  public DatosDeImagenDeTrabajo registrarImagen(
      UsuarioAutenticado sujeto, Long idTrabajo, String urlImagen, String textoAlternativo) {
    TrabajoPortafolio trabajo = trabajoPropio(sujeto, idTrabajo);

    List<ImagenTrabajoPortafolio> existentes = imagenesDe(trabajo.getIdTrabajo());
    short siguienteOrden =
        existentes.isEmpty()
            ? 0
            : (short) (existentes.get(existentes.size() - 1).getOrdenVisualizacion() + 1);

    ImagenTrabajoPortafolio imagen =
        new ImagenTrabajoPortafolio(
            trabajo.getIdTrabajo(), urlImagen, textoAlternativo, siguienteOrden);

    return DatosDeImagenDeTrabajo.de(imagenes.save(imagen));
  }

  /** Sustituye el texto alternativo de una imagen propia. */
  @Transactional
  public DatosDeImagenDeTrabajo actualizarTextoAlternativo(
      UsuarioAutenticado sujeto,
      Long idTrabajo,
      Long idImagen,
      SolicitudDeTextoAlternativo solicitud) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    ImagenTrabajoPortafolio imagen = imagenPropia(sujeto, idTrabajo, idImagen);
    imagen.cambiarTextoAlternativo(solicitud.textoAlternativo());

    return DatosDeImagenDeTrabajo.de(imagen);
  }

  /** Deja las imágenes de un trabajo propio en el orden pedido. */
  @Transactional
  public List<DatosDeImagenDeTrabajo> reordenarImagenes(
      UsuarioAutenticado sujeto, Long idTrabajo, SolicitudDeOrden orden) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    TrabajoPortafolio trabajo = trabajoPropio(sujeto, idTrabajo);
    Map<Long, ImagenTrabajoPortafolio> porId =
        imagenesDe(trabajo.getIdTrabajo()).stream()
            .collect(
                Collectors.toMap(
                    ImagenTrabajoPortafolio::getIdImagenTrabajoPortafolio, Function.identity()));

    orden.exigirExactamente(porId.keySet());

    short posicion = 0;
    for (Long id : orden.idsEnOrden()) {
      porId.get(id).cambiarOrdenVisualizacion(posicion);
      posicion++;
    }

    return orden.idsEnOrden().stream().map(porId::get).map(DatosDeImagenDeTrabajo::de).toList();
  }

  /** Elimina la fila de una imagen propia y devuelve su URL desreferenciada. */
  @Transactional
  public String eliminarFilaDeImagen(UsuarioAutenticado sujeto, Long idTrabajo, Long idImagen) {
    ImagenTrabajoPortafolio imagen = imagenPropia(sujeto, idTrabajo, idImagen);
    String url = imagen.getUrlImagen();
    imagenes.delete(imagen);
    return url;
  }

  /**
   * Exige que el trabajo exista, sea del sujeto y su cuenta pueda modificarlo.
   *
   * <p>Lo usa la orquestación de imágenes antes de subir un objeto: comprobarlo primero evita subir
   * algo que la transacción posterior rechazaría siempre.
   */
  @Transactional(readOnly = true)
  public void exigirQuePuedaModificarElTrabajo(UsuarioAutenticado sujeto, Long idTrabajo) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);
    trabajoPropio(sujeto, idTrabajo);
  }

  private List<TrabajoPortafolio> trabajosDe(UsuarioAutenticado sujeto) {
    return trabajos.findByIdPrestadorOrderByOrdenVisualizacionAscIdTrabajoAsc(sujeto.idUsuario());
  }

  private List<ImagenTrabajoPortafolio> imagenesDe(Long idTrabajo) {
    return imagenes.findByIdTrabajoOrderByOrdenVisualizacionAscIdImagenTrabajoPortafolioAsc(
        idTrabajo);
  }

  private TrabajoPortafolio trabajoPropio(UsuarioAutenticado sujeto, Long idTrabajo) {
    return trabajos
        .findByIdTrabajoAndIdPrestador(idTrabajo, sujeto.idUsuario())
        .orElseThrow(TrabajoPortafolioService::trabajoNoEncontrado);
  }

  private ImagenTrabajoPortafolio imagenPropia(
      UsuarioAutenticado sujeto, Long idTrabajo, Long idImagen) {
    // Primero la propiedad del trabajo; después la pertenencia de la imagen.
    trabajoPropio(sujeto, idTrabajo);
    return imagenes
        .findByIdImagenTrabajoPortafolioAndIdTrabajo(idImagen, idTrabajo)
        .orElseThrow(
            () ->
                new ErrorDeAplicacion(
                    HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "Esa imagen no existe."));
  }

  private DatosDeTrabajo aDatos(TrabajoPortafolio trabajo) {
    return DatosDeTrabajo.de(
        trabajo,
        imagenesDe(trabajo.getIdTrabajo()).stream().map(DatosDeImagenDeTrabajo::de).toList());
  }

  private static ErrorDeAplicacion trabajoNoEncontrado() {
    return new ErrorDeAplicacion(
        HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "Ese trabajo del portafolio no existe.");
  }
}
