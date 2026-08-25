package com.moica.prestador.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.dto.SolicitudDeOrden;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.prestador.dto.DatosDeMedioContacto;
import com.moica.prestador.dto.SolicitudDeMedioContacto;
import com.moica.prestador.entity.MedioContactoPrestador;
import com.moica.prestador.repository.MedioContactoPrestadorRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Medios de contacto del prestador de la sesión.
 *
 * <p>Toda consulta al repositorio lleva el identificador del propietario: un contacto ajeno no se
 * encuentra, así que se responde como inexistente y nadie puede confirmar contactos de otros
 * probando identificadores.
 */
@Service
public class MedioContactoService {

  private final MedioContactoPrestadorRepository repositorio;
  private final PerfilPrestadorService perfiles;

  public MedioContactoService(
      MedioContactoPrestadorRepository repositorio, PerfilPrestadorService perfiles) {
    this.repositorio = repositorio;
    this.perfiles = perfiles;
  }

  /** Los contactos propios en su orden de visualización. */
  @Transactional(readOnly = true)
  public List<DatosDeMedioContacto> listar(UsuarioAutenticado sujeto) {
    perfiles.exigirQueExistaElPerfil(sujeto.idUsuario());
    return contactosDe(sujeto).stream().map(DatosDeMedioContacto::de).toList();
  }

  /** Agrega un contacto al final de la lista. */
  @Transactional
  public DatosDeMedioContacto crear(UsuarioAutenticado sujeto, SolicitudDeMedioContacto solicitud) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    List<MedioContactoPrestador> existentes = contactosDe(sujeto);
    short siguienteOrden =
        existentes.isEmpty()
            ? 0
            : (short) (existentes.get(existentes.size() - 1).getOrdenVisualizacion() + 1);

    MedioContactoPrestador contacto =
        new MedioContactoPrestador(sujeto.idUsuario(), solicitud.contenido(), siguienteOrden);

    return DatosDeMedioContacto.de(repositorio.save(contacto));
  }

  /** Sustituye el contenido de un contacto propio. */
  @Transactional
  public DatosDeMedioContacto actualizar(
      UsuarioAutenticado sujeto, Long idMedioContacto, SolicitudDeMedioContacto solicitud) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    MedioContactoPrestador contacto = contactoPropio(sujeto, idMedioContacto);
    contacto.cambiarContenido(solicitud.contenido());

    return DatosDeMedioContacto.de(contacto);
  }

  /** Elimina un contacto propio. */
  @Transactional
  public void eliminar(UsuarioAutenticado sujeto, Long idMedioContacto) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);
    repositorio.delete(contactoPropio(sujeto, idMedioContacto));
  }

  /**
   * Deja los contactos en el orden pedido.
   *
   * <p>La solicitud debe traer exactamente los contactos existentes: con la lista completa la
   * operación es idempotente y no hay posiciones huérfanas ni duplicadas.
   */
  @Transactional
  public List<DatosDeMedioContacto> reordenar(UsuarioAutenticado sujeto, SolicitudDeOrden orden) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    List<MedioContactoPrestador> existentes = contactosDe(sujeto);
    Map<Long, MedioContactoPrestador> porId =
        existentes.stream()
            .collect(
                Collectors.toMap(
                    MedioContactoPrestador::getIdMedioContactoPrestador, Function.identity()));

    orden.exigirExactamente(porId.keySet());

    short posicion = 0;
    for (Long id : orden.idsEnOrden()) {
      porId.get(id).cambiarOrdenVisualizacion(posicion);
      posicion++;
    }

    return orden.idsEnOrden().stream().map(porId::get).map(DatosDeMedioContacto::de).toList();
  }

  private List<MedioContactoPrestador> contactosDe(UsuarioAutenticado sujeto) {
    return repositorio.findByIdPrestadorOrderByOrdenVisualizacionAscIdMedioContactoPrestadorAsc(
        sujeto.idUsuario());
  }

  private MedioContactoPrestador contactoPropio(UsuarioAutenticado sujeto, Long idMedioContacto) {
    return repositorio
        .findByIdMedioContactoPrestadorAndIdPrestador(idMedioContacto, sujeto.idUsuario())
        .orElseThrow(
            () ->
                new ErrorDeAplicacion(
                    HttpStatus.NOT_FOUND,
                    "RECURSO_NO_ENCONTRADO",
                    "Ese medio de contacto no existe."));
  }
}
