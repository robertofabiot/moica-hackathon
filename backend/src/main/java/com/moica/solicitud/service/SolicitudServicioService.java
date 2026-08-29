package com.moica.solicitud.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.catalogo.dto.UbicacionDeMunicipio;
import com.moica.catalogo.service.CatalogoTerritorialService;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.prestador.dto.CondicionDePublicacion;
import com.moica.prestador.dto.ResumenDePerfilPrestador;
import com.moica.prestador.service.PerfilPrestadorService;
import com.moica.servicio.dto.ReferenciaDeServicio;
import com.moica.servicio.entity.EstadoServicio;
import com.moica.servicio.service.ServicioPublicadoService;
import com.moica.solicitud.dto.DatosDeCambioEstadoSolicitud;
import com.moica.solicitud.dto.DatosDeSolicitudServicio;
import com.moica.solicitud.dto.ResumenDeSolicitudServicio;
import com.moica.solicitud.dto.SolicitudDeCancelacion;
import com.moica.solicitud.dto.SolicitudDeContratacion;
import com.moica.solicitud.entity.CambioEstadoSolicitud;
import com.moica.solicitud.entity.EstadoSolicitud;
import com.moica.solicitud.entity.SolicitudServicio;
import com.moica.solicitud.repository.CambioEstadoSolicitudRepository;
import com.moica.solicitud.repository.SolicitudServicioRepository;
import com.moica.usuario.entity.EstadoCuenta;
import com.moica.usuario.service.UsuarioService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de una solicitud de servicio y de su historial.
 *
 * <p>Crear y cada transición ocurren en una sola transacción: actualizan {@code estadoActual} y
 * dejan el {@link CambioEstadoSolicitud} correspondiente con el mismo instante. Las transiciones
 * bloquean la fila para que dos acciones simultáneas no dejen estado e historial divergentes.
 *
 * <p>Solo los dos participantes leen la solicitud. Un tercero recibe 404, igual que en el resto de
 * recursos propios. Aceptar no revela contactos: eso corresponde a un incremento posterior.
 */
@Service
public class SolicitudServicioService {

  private final SolicitudServicioRepository solicitudes;
  private final CambioEstadoSolicitudRepository cambios;
  private final ServicioPublicadoService servicios;
  private final PerfilPrestadorService perfiles;
  private final CatalogoTerritorialService catalogo;
  private final UsuarioService usuarios;

  public SolicitudServicioService(
      SolicitudServicioRepository solicitudes,
      CambioEstadoSolicitudRepository cambios,
      ServicioPublicadoService servicios,
      PerfilPrestadorService perfiles,
      CatalogoTerritorialService catalogo,
      UsuarioService usuarios) {
    this.solicitudes = solicitudes;
    this.cambios = cambios;
    this.servicios = servicios;
    this.perfiles = perfiles;
    this.catalogo = catalogo;
    this.usuarios = usuarios;
  }

  /**
   * Envía una solicitud a un servicio ajeno.
   *
   * @throws ErrorDeAplicacion si la cuenta no está activa, el servicio no admite contratación, el
   *     municipio no está disponible o el servicio es propio
   */
  @Transactional
  public DatosDeSolicitudServicio crear(UsuarioAutenticado sujeto, SolicitudDeContratacion pedido) {
    exigirCuentaActiva(
        sujeto, "Tu cuenta está restringida y por ahora no puede enviar solicitudes.");

    ReferenciaDeServicio servicio =
        servicios
            .referenciar(pedido.idServicioPublicado())
            .orElseThrow(
                () ->
                    new ErrorDeAplicacion(
                        HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "Ese servicio no existe."));

    if (servicio.idPrestador().equals(sujeto.idUsuario())) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "SERVICIO_PROPIO",
          "No puedes solicitar un servicio publicado por tu propia cuenta.");
    }
    if (servicio.estado() != EstadoServicio.ACTIVO) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT, "SERVICIO_INACTIVO", "Ese servicio no admite solicitudes ahora.");
    }

    CondicionDePublicacion condicion =
        perfiles.condicionDe(servicio.idPrestador()).orElseThrow(this::noEncontrada);
    if (!condicion.tieneVerificacionBasica()) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "VERIFICACION_BASICA_REQUERIDA",
          "Ese prestador todavía no tiene la verificación básica.");
    }
    if (!condicion.estaDisponible() || !usuarios.esCuentaOperativa(servicio.idPrestador())) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "PRESTADOR_NO_DISPONIBLE",
          "Ese prestador no está disponible para nuevas solicitudes.");
    }

    UbicacionDeMunicipio municipio = municipioDisponible(pedido.idMunicipio());
    OffsetDateTime instante = OffsetDateTime.now();

    SolicitudServicio solicitud =
        solicitudes.save(
            new SolicitudServicio(
                sujeto.idUsuario(),
                servicio.idServicioPublicado(),
                municipio.idMunicipio(),
                pedido.descripcionNecesidad(),
                pedido.indicacionUbicacion(),
                pedido.fechaPreferida(),
                instante));
    cambios.save(
        CambioEstadoSolicitud.inicial(
            solicitud.getIdSolicitudServicio(), sujeto.idUsuario(), instante));

    return detalleDe(solicitud);
  }

  /** Las solicitudes que la cuenta envió como cliente, de la más reciente a la más antigua. */
  @Transactional(readOnly = true)
  public List<ResumenDeSolicitudServicio> enviadas(UsuarioAutenticado sujeto) {
    return solicitudes
        .findByIdClienteOrderByFechaCreacionDescIdSolicitudServicioDesc(sujeto.idUsuario())
        .stream()
        .map(this::resumenDe)
        .toList();
  }

  /**
   * Las solicitudes dirigidas a los servicios de la cuenta, de la más reciente a la más antigua.
   */
  @Transactional(readOnly = true)
  public List<ResumenDeSolicitudServicio> recibidas(UsuarioAutenticado sujeto) {
    List<Long> ids = servicios.idsDeServiciosDe(sujeto.idUsuario());
    if (ids.isEmpty()) {
      return List.of();
    }
    return solicitudes
        .findByIdServicioPublicadoInOrderByFechaCreacionDescIdSolicitudServicioDesc(ids)
        .stream()
        .map(this::resumenDe)
        .toList();
  }

  /**
   * Detalle e historial de una solicitud de la que el sujeto es participante.
   *
   * @throws ErrorDeAplicacion 404 si no existe o no le pertenece
   */
  @Transactional(readOnly = true)
  public DatosDeSolicitudServicio consultar(UsuarioAutenticado sujeto, Long idSolicitudServicio) {
    return detalleDe(solicitudVisible(sujeto, idSolicitudServicio));
  }

  /** El prestador destinatario acepta una solicitud {@code PENDIENTE}. */
  @Transactional
  public DatosDeSolicitudServicio aceptar(UsuarioAutenticado sujeto, Long idSolicitudServicio) {
    exigirCuentaActiva(
        sujeto, "Tu cuenta está restringida y por ahora no puede aceptar solicitudes.");
    SolicitudServicio solicitud = solicitudBloqueada(sujeto, idSolicitudServicio);
    exigirPrestador(sujeto, solicitud);
    return transicionar(
        solicitud, sujeto, EstadoSolicitud.PENDIENTE, EstadoSolicitud.ACEPTADA, null);
  }

  /**
   * El prestador destinatario rechaza una solicitud {@code PENDIENTE}.
   *
   * <p>Exige cuenta {@code ACTIVA}.
   */
  @Transactional
  public DatosDeSolicitudServicio rechazar(UsuarioAutenticado sujeto, Long idSolicitudServicio) {
    exigirCuentaActiva(
        sujeto, "Tu cuenta está restringida y por ahora no puede rechazar solicitudes.");
    SolicitudServicio solicitud = solicitudBloqueada(sujeto, idSolicitudServicio);
    exigirPrestador(sujeto, solicitud);
    return transicionar(
        solicitud, sujeto, EstadoSolicitud.PENDIENTE, EstadoSolicitud.RECHAZADA, null);
  }

  /**
   * Cancela según el estado y el actor.
   *
   * <p>En {@code PENDIENTE} solo el cliente, sin motivo. En {@code ACEPTADA} cualquiera de los dos,
   * con motivo obligatorio. Una cuenta restringida puede cancelar un compromiso existente.
   */
  @Transactional
  public DatosDeSolicitudServicio cancelar(
      UsuarioAutenticado sujeto, Long idSolicitudServicio, SolicitudDeCancelacion pedido) {
    SolicitudServicio solicitud = solicitudBloqueada(sujeto, idSolicitudServicio);
    String motivo = pedido == null ? null : pedido.motivo();

    if (solicitud.getEstadoActual() == EstadoSolicitud.PENDIENTE) {
      exigirCliente(sujeto, solicitud);
      return transicionar(
          solicitud, sujeto, EstadoSolicitud.PENDIENTE, EstadoSolicitud.CANCELADA, null);
    }
    if (solicitud.getEstadoActual() == EstadoSolicitud.ACEPTADA) {
      if (motivo == null || motivo.isBlank()) {
        throw new ErrorDeAplicacion(
            HttpStatus.BAD_REQUEST, "MOTIVO_OBLIGATORIO", "Indica el motivo de la cancelación.");
      }
      return transicionar(
          solicitud, sujeto, EstadoSolicitud.ACEPTADA, EstadoSolicitud.CANCELADA, motivo);
    }
    throw transicionNoPermitida();
  }

  /** El prestador destinatario marca una solicitud {@code ACEPTADA} como completada. */
  @Transactional
  public DatosDeSolicitudServicio completar(UsuarioAutenticado sujeto, Long idSolicitudServicio) {
    exigirCuentaActiva(
        sujeto, "Tu cuenta está restringida y por ahora no puede completar solicitudes.");
    SolicitudServicio solicitud = solicitudBloqueada(sujeto, idSolicitudServicio);
    exigirPrestador(sujeto, solicitud);
    return transicionar(
        solicitud, sujeto, EstadoSolicitud.ACEPTADA, EstadoSolicitud.COMPLETADA, null);
  }

  private DatosDeSolicitudServicio transicionar(
      SolicitudServicio solicitud,
      UsuarioAutenticado sujeto,
      EstadoSolicitud origen,
      EstadoSolicitud destino,
      String motivo) {
    if (solicitud.getEstadoActual() != origen) {
      throw transicionNoPermitida();
    }

    OffsetDateTime instante = OffsetDateTime.now();
    EstadoSolicitud anterior = solicitud.getEstadoActual();
    solicitud.cambiarEstado(destino, instante);
    cambios.save(
        new CambioEstadoSolicitud(
            solicitud.getIdSolicitudServicio(),
            anterior,
            destino,
            sujeto.idUsuario(),
            motivo,
            instante));
    return detalleDe(solicitud);
  }

  private SolicitudServicio solicitudVisible(UsuarioAutenticado sujeto, Long idSolicitudServicio) {
    SolicitudServicio solicitud =
        solicitudes.findById(idSolicitudServicio).orElseThrow(this::noEncontrada);
    if (!esParticipante(sujeto, solicitud)) {
      throw noEncontrada();
    }
    return solicitud;
  }

  private SolicitudServicio solicitudBloqueada(
      UsuarioAutenticado sujeto, Long idSolicitudServicio) {
    SolicitudServicio solicitud =
        solicitudes.bloquearPorId(idSolicitudServicio).orElseThrow(this::noEncontrada);
    if (!esParticipante(sujeto, solicitud)) {
      throw noEncontrada();
    }
    return solicitud;
  }

  private boolean esParticipante(UsuarioAutenticado sujeto, SolicitudServicio solicitud) {
    if (solicitud.getIdCliente().equals(sujeto.idUsuario())) {
      return true;
    }
    return servicios
        .referenciar(solicitud.getIdServicioPublicado())
        .map(referencia -> referencia.idPrestador().equals(sujeto.idUsuario()))
        .orElse(false);
  }

  private void exigirCliente(UsuarioAutenticado sujeto, SolicitudServicio solicitud) {
    if (!solicitud.getIdCliente().equals(sujeto.idUsuario())) {
      throw transicionNoPermitida();
    }
  }

  private void exigirPrestador(UsuarioAutenticado sujeto, SolicitudServicio solicitud) {
    boolean esDestinatario =
        servicios
            .referenciar(solicitud.getIdServicioPublicado())
            .map(referencia -> referencia.idPrestador().equals(sujeto.idUsuario()))
            .orElse(false);
    if (!esDestinatario) {
      throw transicionNoPermitida();
    }
  }

  private ResumenDeSolicitudServicio resumenDe(SolicitudServicio solicitud) {
    ReferenciaDeServicio servicio = servicioDe(solicitud);
    ResumenDePerfilPrestador prestador = prestadorDe(servicio.idPrestador());
    return ResumenDeSolicitudServicio.de(
        solicitud,
        servicio.nombre(),
        usuarios.obtener(solicitud.getIdCliente()).nombreCompleto(),
        prestador.idPrestador(),
        prestador.nombrePublico(),
        municipioDe(solicitud).nombreMunicipio());
  }

  private DatosDeSolicitudServicio detalleDe(SolicitudServicio solicitud) {
    ReferenciaDeServicio servicio = servicioDe(solicitud);
    ResumenDePerfilPrestador prestador = prestadorDe(servicio.idPrestador());
    UbicacionDeMunicipio municipio = municipioDe(solicitud);
    List<DatosDeCambioEstadoSolicitud> historial =
        cambios
            .findByIdSolicitudServicioOrderByFechaCambioAscIdCambioEstadoSolicitudAsc(
                solicitud.getIdSolicitudServicio())
            .stream()
            .map(
                cambio ->
                    DatosDeCambioEstadoSolicitud.de(
                        cambio, usuarios.obtener(cambio.getIdActor()).nombreCompleto()))
            .toList();
    return DatosDeSolicitudServicio.de(
        solicitud,
        servicio.nombre(),
        usuarios.obtener(solicitud.getIdCliente()).nombreCompleto(),
        prestador.idPrestador(),
        prestador.nombrePublico(),
        municipio.nombreMunicipio(),
        municipio.nombreDepartamento(),
        historial);
  }

  private ReferenciaDeServicio servicioDe(SolicitudServicio solicitud) {
    return servicios
        .referenciar(solicitud.getIdServicioPublicado())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "La solicitud "
                        + solicitud.getIdSolicitudServicio()
                        + " referencia un servicio que no existe"));
  }

  private ResumenDePerfilPrestador prestadorDe(Long idPrestador) {
    return perfiles
        .resumirPerfil(idPrestador)
        .orElseThrow(
            () -> new IllegalStateException("El prestador " + idPrestador + " no tiene perfil"));
  }

  private UbicacionDeMunicipio municipioDe(SolicitudServicio solicitud) {
    return catalogo
        .describirMunicipio(solicitud.getIdMunicipio())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "La solicitud "
                        + solicitud.getIdSolicitudServicio()
                        + " referencia un municipio que no existe"));
  }

  private UbicacionDeMunicipio municipioDisponible(Integer idMunicipio) {
    return catalogo
        .describirMunicipio(idMunicipio)
        .filter(UbicacionDeMunicipio::departamentoHabilitado)
        .orElseThrow(
            () ->
                new ErrorDeAplicacion(
                    HttpStatus.BAD_REQUEST,
                    "MUNICIPIO_NO_DISPONIBLE",
                    "El municipio elegido no está disponible en Moica."));
  }

  private static void exigirCuentaActiva(UsuarioAutenticado sujeto, String mensaje) {
    if (sujeto.estadoCuenta() != EstadoCuenta.ACTIVA) {
      throw new ErrorDeAplicacion(HttpStatus.FORBIDDEN, "CUENTA_RESTRINGIDA", mensaje);
    }
  }

  private ErrorDeAplicacion noEncontrada() {
    return new ErrorDeAplicacion(
        HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "Esa solicitud no existe.");
  }

  private static ErrorDeAplicacion transicionNoPermitida() {
    return new ErrorDeAplicacion(
        HttpStatus.CONFLICT, "TRANSICION_NO_PERMITIDA", "Esa solicitud ya no admite esta acción.");
  }
}
