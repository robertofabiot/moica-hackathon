package com.moica.verificacion.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.prestador.dto.ResumenDePerfilPrestador;
import com.moica.prestador.entity.NivelVerificacionPrestador;
import com.moica.prestador.service.PerfilPrestadorService;
import com.moica.verificacion.dto.DatosDeDocumentoVerificacion;
import com.moica.verificacion.dto.DatosDeEstadoDeVerificacion;
import com.moica.verificacion.dto.DatosDeSolicitudVerificacion;
import com.moica.verificacion.entity.DocumentoVerificacionPrestador;
import com.moica.verificacion.entity.EstadoSolicitudVerificacion;
import com.moica.verificacion.entity.NivelVerificacionSolicitado;
import com.moica.verificacion.entity.SolicitudVerificacionPrestador;
import com.moica.verificacion.entity.TipoDocumentoVerificacion;
import com.moica.verificacion.repository.DocumentoVerificacionPrestadorRepository;
import com.moica.verificacion.repository.SolicitudVerificacionPrestadorRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El expediente de verificación visto desde su propietario.
 *
 * <p>Toda operación parte del sujeto de la sesión: ningún método acepta decidir sobre qué perfil
 * actuar, así que consultar o presentar el expediente de otra persona no es un permiso que se
 * comprueba sino un camino que no existe.
 *
 * <p>El propietario nunca cambia el nivel de su perfil ni el estado de una solicitud: solo envía y
 * consulta. Resolver es competencia de {@link RevisionDeVerificacionService}, y su puerta de
 * entrada exige rol administrativo con segundo factor verificado.
 *
 * <p>La creación es atómica. No existe un estado {@code BORRADOR}: o se registra la solicitud con
 * su expediente completo, o no se registra nada. Los archivos ya están en el almacenamiento privado
 * cuando esta clase entra en juego; de retirarlos si esta transacción falla se encarga {@link
 * EnvioDeExpedienteService}.
 */
@Service
public class VerificacionDelPrestadorService {

  /** Los dos estados que ocupan el índice parcial {@code uq_solicitud_verificacion_abierta}. */
  static final Set<EstadoSolicitudVerificacion> ESTADOS_ABIERTOS =
      Set.of(EstadoSolicitudVerificacion.PENDIENTE, EstadoSolicitudVerificacion.EN_REVISION);

  private final SolicitudVerificacionPrestadorRepository solicitudes;
  private final DocumentoVerificacionPrestadorRepository documentos;
  private final PerfilPrestadorService perfiles;
  private final Clock reloj;

  public VerificacionDelPrestadorService(
      SolicitudVerificacionPrestadorRepository solicitudes,
      DocumentoVerificacionPrestadorRepository documentos,
      PerfilPrestadorService perfiles,
      Clock reloj) {
    this.solicitudes = solicitudes;
    this.documentos = documentos;
    this.perfiles = perfiles;
    this.reloj = reloj;
  }

  /**
   * Dónde está el perfil propio: nivel vigente, qué significa, qué puede pedir y qué espera
   * decisión.
   *
   * @throws ErrorDeAplicacion si la cuenta todavía no creó su perfil
   */
  @Transactional(readOnly = true)
  public DatosDeEstadoDeVerificacion consultarEstado(UsuarioAutenticado sujeto) {
    NivelVerificacionPrestador nivel = nivelDelPerfil(sujeto.idUsuario());
    List<SolicitudVerificacionPrestador> abiertas =
        solicitudes.findByIdPrestadorAndEstadoSolicitudIn(sujeto.idUsuario(), ESTADOS_ABIERTOS);

    boolean basicaAbierta = hayAbiertaDe(abiertas, NivelVerificacionSolicitado.BASICA);
    boolean profesionalAbierta = hayAbiertaDe(abiertas, NivelVerificacionSolicitado.PROFESIONAL);

    // Solo puede haber una solicitud abierta por nivel, así que como mucho hay
    // dos; se muestra la más reciente, que es la que la persona acaba de enviar.
    DatosDeSolicitudVerificacion abierta =
        abiertas.stream()
            .max(Comparator.comparing(SolicitudVerificacionPrestador::getIdSolicitudVerificacion))
            .map(this::conSuExpediente)
            .orElse(null);

    return new DatosDeEstadoDeVerificacion(
        nivel,
        DatosDeEstadoDeVerificacion.significadoDe(nivel),
        nivel == NivelVerificacionPrestador.SIN_VERIFICAR && !basicaAbierta,
        nivel == NivelVerificacionPrestador.VERIFICADO_BASICO && !profesionalAbierta,
        abierta);
  }

  /**
   * Todas las solicitudes propias, de la más reciente a la más antigua, con sus documentos.
   *
   * <p>Incluye las resueltas: una solicitud rechazada o revocada se conserva como evidencia y es lo
   * que permite leer el motivo y volver a intentarlo con los documentos corregidos.
   *
   * @throws ErrorDeAplicacion si la cuenta todavía no creó su perfil
   */
  @Transactional(readOnly = true)
  public List<DatosDeSolicitudVerificacion> consultarHistorial(UsuarioAutenticado sujeto) {
    perfiles.exigirQueExistaElPerfil(sujeto.idUsuario());

    List<SolicitudVerificacionPrestador> propias =
        solicitudes.findByIdPrestadorOrderByFechaSolicitudDescIdSolicitudVerificacionDesc(
            sujeto.idUsuario());

    Map<Long, List<DatosDeDocumentoVerificacion>> expedientes = expedientesDe(propias);
    return propias.stream()
        .map(
            solicitud ->
                DatosDeSolicitudVerificacion.de(
                    solicitud,
                    expedientes.getOrDefault(solicitud.getIdSolicitudVerificacion(), List.of())))
        .toList();
  }

  /**
   * Una solicitud propia con los metadatos de su expediente.
   *
   * <p>Pedir la de otro prestador responde igual que pedir una inexistente: distinguirlas
   * permitiría enumerar identificadores para averiguar quién presentó expediente.
   *
   * @throws ErrorDeAplicacion 404 si no existe o no es de esta cuenta
   */
  @Transactional(readOnly = true)
  public DatosDeSolicitudVerificacion consultarSolicitudPropia(
      UsuarioAutenticado sujeto, Long idSolicitudVerificacion) {
    perfiles.exigirQueExistaElPerfil(sujeto.idUsuario());

    return solicitudes
        .findByIdSolicitudVerificacionAndIdPrestador(idSolicitudVerificacion, sujeto.idUsuario())
        .map(this::conSuExpediente)
        .orElseThrow(VerificacionDelPrestadorService::solicitudNoEncontrada);
  }

  /**
   * Comprueba, antes de tocar el almacenamiento, que este envío tiene sentido.
   *
   * <p>Se hace primero para no subir archivos que después habría que retirar. No sustituye a la
   * restricción de la base de datos: dos envíos simultáneos pasarían los dos por aquí y es el
   * índice parcial el que deja pasar solo a uno.
   *
   * @throws ErrorDeAplicacion si la cuenta no está activa, no tiene perfil, ya tiene abierta una
   *     solicitud de ese nivel o ese nivel no le corresponde ahora
   */
  @Transactional(readOnly = true)
  public void exigirQuePuedaSolicitar(
      UsuarioAutenticado sujeto, NivelVerificacionSolicitado nivelSolicitado) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    List<SolicitudVerificacionPrestador> abiertas =
        solicitudes.findByIdPrestadorAndEstadoSolicitudIn(sujeto.idUsuario(), ESTADOS_ABIERTOS);
    if (hayAbiertaDe(abiertas, nivelSolicitado)) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "SOLICITUD_ABIERTA_DUPLICADA",
          "Ya tienes una solicitud de ese nivel esperando revisión. Espera la respuesta antes de"
              + " enviar otra.");
    }
    exigirNivelCoherente(nivelDelPerfil(sujeto.idUsuario()), nivelSolicitado);
  }

  /**
   * Exige que el expediente respalde el nivel que se pide.
   *
   * <p>Se comprueba sobre los tipos declarados, antes de subir nada: la básica necesita al menos un
   * documento de identidad y la profesional al menos un respaldo profesional, técnico o comercial.
   * Qué documento demuestra qué lo decide después una persona; esto solo evita que llegue a
   * revisión un expediente que no puede sostener lo que pide.
   *
   * @throws ErrorDeAplicacion 400 si el expediente llega vacío o sin el respaldo exigido
   */
  public void exigirExpedienteCompleto(
      NivelVerificacionSolicitado nivelSolicitado, List<TipoDocumentoVerificacion> tipos) {
    if (tipos.isEmpty()) {
      throw expedienteIncompleto("Adjunta al menos un documento para enviar tu solicitud.");
    }
    if (nivelSolicitado == NivelVerificacionSolicitado.BASICA
        && tipos.stream().noneMatch(tipo -> tipo == TipoDocumentoVerificacion.IDENTIDAD)) {
      throw expedienteIncompleto(
          "La verificación básica necesita al menos un documento oficial de identidad.");
    }
    if (nivelSolicitado == NivelVerificacionSolicitado.PROFESIONAL
        && tipos.stream().noneMatch(TipoDocumentoVerificacion::esRespaldoProfesional)) {
      throw expedienteIncompleto(
          "La verificación profesional necesita al menos un respaldo profesional, técnico o"
              + " comercial: una certificación, una constancia, un registro de negocio u otro"
              + " respaldo.");
    }
  }

  /**
   * Registra la solicitud y su expediente completo en una sola transacción.
   *
   * <p>Los archivos ya están en el bucket privado. Si esta transacción falla no queda nada a medias
   * en la base de datos, y quien la invocó retira los objetos que había subido.
   *
   * @throws ErrorDeAplicacion 409 si otra petición simultánea se adelantó con el mismo nivel
   */
  @Transactional
  public DatosDeSolicitudVerificacion registrar(
      Long idPrestador,
      NivelVerificacionSolicitado nivelSolicitado,
      List<DocumentoCargado> cargados) {

    OffsetDateTime ahora = OffsetDateTime.now(reloj);
    SolicitudVerificacionPrestador solicitud =
        new SolicitudVerificacionPrestador(idPrestador, nivelSolicitado, ahora);

    SolicitudVerificacionPrestador guardada;
    try {
      // Se fuerza la escritura aquí para que la violación del índice parcial
      // llegue como excepción de esta llamada y no al confirmar, cuando ya no
      // podría traducirse en una respuesta clara.
      guardada = solicitudes.saveAndFlush(solicitud);
    } catch (DataIntegrityViolationException colision) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "SOLICITUD_ABIERTA_DUPLICADA",
          "Ya tienes una solicitud de ese nivel esperando revisión. Espera la respuesta antes de"
              + " enviar otra.");
    }

    List<DocumentoVerificacionPrestador> nuevos = new ArrayList<>(cargados.size());
    for (DocumentoCargado cargado : cargados) {
      nuevos.add(
          new DocumentoVerificacionPrestador(
              guardada.getIdSolicitudVerificacion(),
              cargado.tipoDocumento(),
              cargado.claveAlmacenamiento(),
              cargado.nombreOriginal(),
              cargado.tipoMime(),
              cargado.tamanoBytes(),
              ahora));
    }

    return DatosDeSolicitudVerificacion.de(
        guardada,
        documentos.saveAll(nuevos).stream().map(DatosDeDocumentoVerificacion::de).toList());
  }

  private NivelVerificacionPrestador nivelDelPerfil(Long idPrestador) {
    return perfiles
        .resumirPerfil(idPrestador)
        .map(ResumenDePerfilPrestador::nivelVerificacion)
        .orElseThrow(
            () ->
                new ErrorDeAplicacion(
                    HttpStatus.NOT_FOUND,
                    "PERFIL_NO_ENCONTRADO",
                    "Esta cuenta todavía no tiene un perfil de prestador."));
  }

  private static void exigirNivelCoherente(
      NivelVerificacionPrestador nivelVigente, NivelVerificacionSolicitado nivelSolicitado) {

    if (nivelSolicitado == NivelVerificacionSolicitado.BASICA
        && nivelVigente != NivelVerificacionPrestador.SIN_VERIFICAR) {
      throw nivelYaVigente(
          "Tu perfil ya tiene la verificación básica vigente. No hace falta volver a solicitarla.");
    }
    if (nivelSolicitado == NivelVerificacionSolicitado.PROFESIONAL) {
      if (nivelVigente == NivelVerificacionPrestador.PROFESIONAL_VERIFICADO) {
        throw nivelYaVigente("Tu perfil ya tiene la verificación profesional vigente.");
      }
      if (nivelVigente != NivelVerificacionPrestador.VERIFICADO_BASICO) {
        throw new ErrorDeAplicacion(
            HttpStatus.CONFLICT,
            "VERIFICACION_BASICA_REQUERIDA",
            "La verificación profesional necesita una verificación básica vigente. Consíguela"
                + " primero.");
      }
    }
  }

  private static boolean hayAbiertaDe(
      List<SolicitudVerificacionPrestador> abiertas, NivelVerificacionSolicitado nivel) {
    return abiertas.stream().anyMatch(solicitud -> solicitud.getNivelSolicitado() == nivel);
  }

  private DatosDeSolicitudVerificacion conSuExpediente(SolicitudVerificacionPrestador solicitud) {
    return DatosDeSolicitudVerificacion.de(
        solicitud,
        documentos
            .findByIdSolicitudVerificacionOrderByIdDocumentoVerificacionAsc(
                solicitud.getIdSolicitudVerificacion())
            .stream()
            .map(DatosDeDocumentoVerificacion::de)
            .toList());
  }

  /** Los expedientes de varias solicitudes en una consulta, agrupados por solicitud. */
  private Map<Long, List<DatosDeDocumentoVerificacion>> expedientesDe(
      List<SolicitudVerificacionPrestador> lista) {
    if (lista.isEmpty()) {
      return Map.of();
    }
    List<Long> ids =
        lista.stream().map(SolicitudVerificacionPrestador::getIdSolicitudVerificacion).toList();

    return documentos.findByIdSolicitudVerificacionInOrderByIdDocumentoVerificacionAsc(ids).stream()
        .collect(
            Collectors.groupingBy(
                DocumentoVerificacionPrestador::getIdSolicitudVerificacion,
                Collectors.mapping(DatosDeDocumentoVerificacion::de, Collectors.toList())));
  }

  private static ErrorDeAplicacion expedienteIncompleto(String mensaje) {
    return new ErrorDeAplicacion(HttpStatus.BAD_REQUEST, "EXPEDIENTE_INCOMPLETO", mensaje);
  }

  private static ErrorDeAplicacion nivelYaVigente(String mensaje) {
    return new ErrorDeAplicacion(HttpStatus.CONFLICT, "NIVEL_YA_VIGENTE", mensaje);
  }

  private static ErrorDeAplicacion solicitudNoEncontrada() {
    return new ErrorDeAplicacion(
        HttpStatus.NOT_FOUND,
        "SOLICITUD_NO_ENCONTRADA",
        "Esa solicitud de verificación no existe.");
  }
}
