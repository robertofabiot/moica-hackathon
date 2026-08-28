package com.moica.verificacion.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.almacenamiento.AlmacenamientoDeDocumentosPrivados;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.prestador.dto.ResumenDePerfilPrestador;
import com.moica.prestador.entity.NivelVerificacionPrestador;
import com.moica.prestador.service.PerfilPrestadorService;
import com.moica.usuario.dto.DatosDeUsuario;
import com.moica.usuario.service.UsuarioService;
import com.moica.verificacion.dto.DatosDeDocumentoVerificacion;
import com.moica.verificacion.dto.DatosDeExpediente;
import com.moica.verificacion.entity.DocumentoVerificacionPrestador;
import com.moica.verificacion.entity.EstadoSolicitudVerificacion;
import com.moica.verificacion.entity.NivelVerificacionSolicitado;
import com.moica.verificacion.entity.SolicitudVerificacionPrestador;
import com.moica.verificacion.repository.DocumentoVerificacionPrestadorRepository;
import com.moica.verificacion.repository.SolicitudVerificacionPrestadorRepository;
import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La revisión manual de expedientes, tal como la hace una persona administradora.
 *
 * <p>Moica no aprueba, rechaza ni revoca nada por su cuenta: toda decisión de esta clase la origina
 * una petición de {@code /api/admin/**}, que la cadena de seguridad solo deja pasar con rol
 * administrativo y segundo factor verificado **en esa misma sesión**. La comprobación se repite
 * aquí como última red, igual que en el resumen administrativo de P3.
 *
 * <p>Las transiciones válidas son exactamente estas:
 *
 * <pre>
 *   PENDIENTE   --tomar-->     EN_REVISION
 *   EN_REVISION --aprobar-->   APROBADA
 *   EN_REVISION --rechazar-->  RECHAZADA
 *   APROBADA    --revocar-->   REVOCADA
 * </pre>
 *
 * <p>Cualquier otra responde 409 y no deja nada a medias. Aprobar y rechazar exigen además ser el
 * administrador que tomó la revisión: quien no la tomó no la resuelve.
 *
 * <p>Las tres resoluciones —aprobar, rechazar y revocar— toman sus bloqueos en un único orden:
 * <b>primero el perfil, después la solicitud</b>. Bloquear solo la solicitud no bastaba: aprobar
 * una profesional y revocar la básica trabajan sobre filas distintas, así que ambas transacciones
 * avanzaban a la vez, leían el mismo nivel antiguo del perfil y la última en escribir dejaba un
 * nivel que contradecía las solicitudes ya resueltas. El perfil es la fila que sí comparten, y
 * tomarlo antes de nada es lo que las pone en fila. Como todas piden lo mismo en el mismo orden,
 * ninguna pareja puede esperarse mutuamente.
 *
 * <p>{@link #tomar(UsuarioAutenticado, Long)} queda fuera de ese orden a propósito: solo bloquea la
 * solicitud y no pide nada después, así que no puede formar parte de un ciclo de espera.
 */
@Service
public class RevisionDeVerificacionService {

  private static final Collection<EstadoSolicitudVerificacion> COLA_POR_OMISION =
      List.of(EstadoSolicitudVerificacion.PENDIENTE, EstadoSolicitudVerificacion.EN_REVISION);

  private final SolicitudVerificacionPrestadorRepository solicitudes;
  private final DocumentoVerificacionPrestadorRepository documentos;
  private final PerfilPrestadorService perfiles;
  private final UsuarioService usuarios;
  private final AlmacenamientoDeDocumentosPrivados almacenamiento;
  private final Clock reloj;

  public RevisionDeVerificacionService(
      SolicitudVerificacionPrestadorRepository solicitudes,
      DocumentoVerificacionPrestadorRepository documentos,
      PerfilPrestadorService perfiles,
      UsuarioService usuarios,
      AlmacenamientoDeDocumentosPrivados almacenamiento,
      Clock reloj) {
    this.solicitudes = solicitudes;
    this.documentos = documentos;
    this.perfiles = perfiles;
    this.usuarios = usuarios;
    this.almacenamiento = almacenamiento;
    this.reloj = reloj;
  }

  /**
   * La cola de revisión, de la más antigua a la más reciente.
   *
   * <p>Sin filtros muestra lo que espera decisión —{@code PENDIENTE} y {@code EN_REVISION}—, que es
   * el trabajo pendiente. Los estados resueltos se piden explícitamente: sirven para revocar una
   * verificación concedida y para seguir la traza de un caso.
   */
  @Transactional(readOnly = true)
  public List<DatosDeExpediente> consultarCola(
      UsuarioAutenticado sujeto,
      Collection<EstadoSolicitudVerificacion> estados,
      NivelVerificacionSolicitado nivel) {

    exigirPermisosAdministrativos(sujeto);

    Collection<EstadoSolicitudVerificacion> filtro =
        (estados == null || estados.isEmpty()) ? COLA_POR_OMISION : estados;

    List<SolicitudVerificacionPrestador> encontradas =
        (nivel == null)
            ? solicitudes.findByEstadoSolicitudInOrderByFechaSolicitudAscIdSolicitudVerificacionAsc(
                filtro)
            : solicitudes
                .findByEstadoSolicitudInAndNivelSolicitadoOrderByFechaSolicitudAscIdSolicitudVerificacionAsc(
                    filtro, nivel);

    return encontradas.stream().map(this::describir).toList();
  }

  /** El detalle de un expediente con los metadatos de todos sus documentos. */
  @Transactional(readOnly = true)
  public DatosDeExpediente consultarExpediente(
      UsuarioAutenticado sujeto, Long idSolicitudVerificacion) {
    exigirPermisosAdministrativos(sujeto);
    return describir(buscar(idSolicitudVerificacion));
  }

  /**
   * Toma una solicitud pendiente para revisarla.
   *
   * <p>La fila se bloquea antes de leer su estado: dos administradores que pulsen a la vez no
   * pueden quedarse los dos con la revisión, porque el segundo entra cuando el primero ya la dejó
   * en {@code EN_REVISION} y su comprobación falla con 409.
   */
  @Transactional
  public DatosDeExpediente tomar(UsuarioAutenticado sujeto, Long idSolicitudVerificacion) {
    exigirPermisosAdministrativos(sujeto);

    SolicitudVerificacionPrestador solicitud = bloquear(idSolicitudVerificacion);
    if (solicitud.getEstadoSolicitud() == EstadoSolicitudVerificacion.EN_REVISION) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "SOLICITUD_YA_TOMADA",
          "Otra persona administradora ya tomó esta solicitud. Actualiza la cola para ver quién la"
              + " tiene.");
    }
    exigirEstado(solicitud, EstadoSolicitudVerificacion.PENDIENTE);

    solicitud.tomar(sujeto.idUsuario(), OffsetDateTime.now(reloj));
    return describir(solicitud);
  }

  /**
   * Aprueba la solicitud y deja vigente en el perfil el nivel que pedía.
   *
   * <p>El nivel se vuelve a comprobar aquí y no solo al enviar: entre el envío y la revisión pudo
   * revocarse la básica, y aprobar entonces una profesional dejaría un perfil profesional sin
   * identidad respaldada.
   */
  @Transactional
  public DatosDeExpediente aprobar(UsuarioAutenticado sujeto, Long idSolicitudVerificacion) {
    SolicitudVerificacionPrestador solicitud = tomarParaResolver(sujeto, idSolicitudVerificacion);

    if (solicitud.getNivelSolicitado() == NivelVerificacionSolicitado.PROFESIONAL
        && nivelDe(solicitud.getIdPrestador()) != NivelVerificacionPrestador.VERIFICADO_BASICO) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "VERIFICACION_BASICA_REQUERIDA",
          "Este perfil ya no tiene una verificación básica vigente, así que no puede recibir la"
              + " profesional. Rechaza la solicitud indicando el motivo.");
    }

    solicitud.aprobar(sujeto.idUsuario(), OffsetDateTime.now(reloj));
    perfiles.proyectarNivelDeVerificacion(
        solicitud.getIdPrestador(), solicitud.getNivelSolicitado().nivelQueProyecta());

    return describir(solicitud);
  }

  /**
   * Rechaza la solicitud con un motivo obligatorio.
   *
   * <p>El perfil conserva el nivel que tuviera: rechazar una profesional no toca la básica vigente.
   */
  @Transactional
  public DatosDeExpediente rechazar(
      UsuarioAutenticado sujeto, Long idSolicitudVerificacion, String observacion) {

    SolicitudVerificacionPrestador solicitud = tomarParaResolver(sujeto, idSolicitudVerificacion);
    solicitud.rechazar(sujeto.idUsuario(), observacion, OffsetDateTime.now(reloj));
    return describir(solicitud);
  }

  /**
   * Revoca una verificación ya concedida, con un motivo obligatorio.
   *
   * <p>Revocar la profesional degrada el perfil a {@code VERIFICADO_BASICO}. Revocar la básica lo
   * devuelve a {@code SIN_VERIFICAR} y, en la **misma transacción**, deja sin efecto cualquier
   * profesional aprobada, con el mismo motivo, el mismo administrador y el mismo instante: una
   * profesional que sobreviviera a la caída de su base diría que Moica respalda una trayectoria de
   * alguien cuya identidad ya no respalda.
   *
   * <p>Una profesional revocada así **no se reactiva sola** si el perfil vuelve a obtener la
   * básica: su fila queda {@code REVOCADA} para siempre y recuperar la insignia exige una solicitud
   * nueva.
   *
   * <p>A diferencia de aprobar y rechazar, revocar no exige haber tomado la solicitud: la aprobó
   * otra persona en otro momento y la revisión ya está cerrada.
   */
  @Transactional
  public DatosDeExpediente revocar(
      UsuarioAutenticado sujeto, Long idSolicitudVerificacion, String observacion) {

    exigirPermisosAdministrativos(sujeto);

    Long idPrestador = bloquearElPerfilDe(idSolicitudVerificacion);
    SolicitudVerificacionPrestador solicitud = bloquear(idSolicitudVerificacion);
    exigirEstado(solicitud, EstadoSolicitudVerificacion.APROBADA);

    OffsetDateTime ahora = OffsetDateTime.now(reloj);
    solicitud.revocar(sujeto.idUsuario(), observacion, ahora);

    if (solicitud.getNivelSolicitado() == NivelVerificacionSolicitado.BASICA) {
      revocarProfesionalesVigentes(idPrestador, sujeto.idUsuario(), observacion, ahora);
      perfiles.proyectarNivelDeVerificacion(idPrestador, NivelVerificacionPrestador.SIN_VERIFICAR);
    } else if (nivelDe(idPrestador) == NivelVerificacionPrestador.PROFESIONAL_VERIFICADO) {
      // Solo degrada si la profesional era la vigente. Si la básica ya se había
      // revocado, el perfil está SIN_VERIFICAR y subirlo a básico sería
      // devolverle una verificación que nadie le concedió.
      perfiles.proyectarNivelDeVerificacion(
          idPrestador, NivelVerificacionPrestador.VERIFICADO_BASICO);
    }

    return describir(solicitud);
  }

  /**
   * Un acceso de lectura de vida corta a un documento del expediente.
   *
   * <p>Se autoriza en cada petición: el rol y el segundo factor los exige la cadena de seguridad, y
   * aquí se comprueba además que el documento pertenece de verdad a esa solicitud. La URL firmada
   * no se guarda en ninguna parte y caduca sola.
   *
   * @throws ErrorDeAplicacion 404 si la solicitud o el documento no existen, o si el documento
   *     pertenece a otro expediente
   */
  @Transactional(readOnly = true)
  public URI abrirDocumento(
      UsuarioAutenticado sujeto, Long idSolicitudVerificacion, Long idDocumentoVerificacion) {

    exigirPermisosAdministrativos(sujeto);
    SolicitudVerificacionPrestador solicitud = buscar(idSolicitudVerificacion);

    DocumentoVerificacionPrestador documento =
        documentos
            .findByIdDocumentoVerificacionAndIdSolicitudVerificacion(
                idDocumentoVerificacion, solicitud.getIdSolicitudVerificacion())
            .orElseThrow(
                () ->
                    new ErrorDeAplicacion(
                        HttpStatus.NOT_FOUND,
                        "DOCUMENTO_NO_ENCONTRADO",
                        "Ese documento no forma parte de este expediente."));

    return almacenamiento.accesoTemporalDeLectura(documento.getClaveAlmacenamiento());
  }

  /** Bloquea, comprueba el estado y comprueba que quien resuelve sea quien tomó la revisión. */
  private SolicitudVerificacionPrestador tomarParaResolver(
      UsuarioAutenticado sujeto, Long idSolicitudVerificacion) {

    exigirPermisosAdministrativos(sujeto);

    bloquearElPerfilDe(idSolicitudVerificacion);
    SolicitudVerificacionPrestador solicitud = bloquear(idSolicitudVerificacion);
    exigirEstado(solicitud, EstadoSolicitudVerificacion.EN_REVISION);

    if (!solicitud.laRevisa(sujeto.idUsuario())) {
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "REVISION_DE_OTRO_ADMINISTRADOR",
          "Esta solicitud la está revisando otra persona administradora. Solo quien la tomó puede"
              + " resolverla.");
    }
    return solicitud;
  }

  private void revocarProfesionalesVigentes(
      Long idPrestador, Long idAdministrador, String observacion, OffsetDateTime instante) {

    solicitudes
        .findByIdPrestadorAndNivelSolicitadoAndEstadoSolicitud(
            idPrestador,
            NivelVerificacionSolicitado.PROFESIONAL,
            EstadoSolicitudVerificacion.APROBADA)
        .forEach(profesional -> profesional.revocar(idAdministrador, observacion, instante));
  }

  private SolicitudVerificacionPrestador buscar(Long idSolicitudVerificacion) {
    return solicitudes
        .findById(idSolicitudVerificacion)
        .orElseThrow(RevisionDeVerificacionService::solicitudNoEncontrada);
  }

  private SolicitudVerificacionPrestador bloquear(Long idSolicitudVerificacion) {
    return solicitudes
        .bloquearPorId(idSolicitudVerificacion)
        .orElseThrow(RevisionDeVerificacionService::solicitudNoEncontrada);
  }

  /**
   * Primer paso de toda resolución: toma el perfil al que pertenece el expediente.
   *
   * <p>Se pide el dueño de la solicitud sin cargarla, se bloquea su perfil y solo después se
   * bloquea la solicitud. Ese orden —perfil, luego solicitud— es el que respetan las tres
   * resoluciones, y por eso ninguna pareja puede quedarse esperándose: quien llega segundo no tiene
   * todavía ningún bloqueo que el primero necesite.
   *
   * @return el identificador del perfil sobre el que va a decidirse, ya bloqueado
   */
  private Long bloquearElPerfilDe(Long idSolicitudVerificacion) {
    Long idPrestador =
        solicitudes
            .idPrestadorDe(idSolicitudVerificacion)
            .orElseThrow(RevisionDeVerificacionService::solicitudNoEncontrada);

    perfiles.bloquearParaResolverVerificacion(idPrestador);
    return idPrestador;
  }

  private NivelVerificacionPrestador nivelDe(Long idPrestador) {
    return perfiles
        .resumirPerfil(idPrestador)
        .map(ResumenDePerfilPrestador::nivelVerificacion)
        // La clave foránea garantiza el perfil; si faltara, el esquema estaría
        // corrupto y ocultarlo sería peor que fallar.
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "La solicitud de verificación referencia un perfil que no existe"));
  }

  private DatosDeExpediente describir(SolicitudVerificacionPrestador solicitud) {
    ResumenDePerfilPrestador perfil =
        perfiles
            .resumirPerfil(solicitud.getIdPrestador())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "La solicitud de verificación referencia un perfil que no existe"));
    DatosDeUsuario cuenta = usuarios.obtener(solicitud.getIdPrestador());

    List<DatosDeDocumentoVerificacion> expediente =
        documentos
            .findByIdSolicitudVerificacionOrderByIdDocumentoVerificacionAsc(
                solicitud.getIdSolicitudVerificacion())
            .stream()
            .map(DatosDeDocumentoVerificacion::de)
            .toList();

    return DatosDeExpediente.de(
        solicitud,
        new DatosDeExpediente.PrestadorDelExpediente(
            perfil.idPrestador(),
            perfil.nombrePublico(),
            perfil.tipoPrestador(),
            perfil.nivelVerificacion(),
            cuenta.nombreCompleto(),
            cuenta.correoElectronico()),
        expediente);
  }

  private static void exigirEstado(
      SolicitudVerificacionPrestador solicitud, EstadoSolicitudVerificacion esperado) {

    if (solicitud.getEstadoSolicitud() != esperado) {
      // El mensaje nombra el estado real porque es información administrativa
      // que quien revisa necesita para entender qué pasó.
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "TRANSICION_NO_PERMITIDA",
          "Esa acción no está disponible: la solicitud está en estado "
              + solicitud.getEstadoSolicitud()
              + ".");
    }
  }

  private static void exigirPermisosAdministrativos(UsuarioAutenticado sujeto) {
    if (sujeto == null || !sujeto.puedeAdministrar()) {
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "ACCESO_DENEGADO",
          "Esta cuenta no tiene permisos administrativos.");
    }
  }

  private static ErrorDeAplicacion solicitudNoEncontrada() {
    return new ErrorDeAplicacion(
        HttpStatus.NOT_FOUND,
        "SOLICITUD_NO_ENCONTRADA",
        "Esa solicitud de verificación no existe.");
  }
}
