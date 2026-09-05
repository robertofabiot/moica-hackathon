package com.moica.moderacion.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.chat.dto.DatosDeMensajeSolicitud;
import com.moica.chat.service.HiloDeSolicitudService;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.moderacion.dto.DatosDeExpedienteDeCaso;
import com.moica.moderacion.dto.DatosDeVersionDeCaso;
import com.moica.moderacion.dto.ResumenDeCasoAdministrativo;
import com.moica.moderacion.entity.CasoModeracion;
import com.moica.moderacion.entity.EstadoCasoModeracion;
import com.moica.moderacion.entity.HistorialCaso;
import com.moica.moderacion.entity.ResultadoCasoModeracion;
import com.moica.moderacion.entity.TipoEventoHistorial;
import com.moica.moderacion.repository.CasoModeracionRepository;
import com.moica.moderacion.repository.HistorialCasoRepository;
import com.moica.servicio.service.ServicioPublicadoService;
import com.moica.solicitud.dto.DatosDeSolicitudServicio;
import com.moica.solicitud.service.SolicitudServicioService;
import com.moica.usuario.entity.EstadoCuenta;
import com.moica.usuario.service.AdministradorService;
import com.moica.usuario.service.UsuarioService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La revisión y la resolución de casos, tal como las hace una persona administradora.
 *
 * <p>Toda operación de esta clase llega por {@code /api/admin/**}, que la cadena de seguridad solo
 * deja pasar con rol administrativo y segundo factor verificado <b>en esa misma sesión</b>. La
 * comprobación se repite aquí como última red, igual que en la revisión de verificaciones de P4V:
 * si alguien alcanzara el servicio por otra vía, tampoco obtendría nada. Eso vale también para las
 * lecturas: una bandeja o un expediente son datos sobre personas reportadas, no información
 * pública.
 *
 * <p>Las transiciones que P10A admite son exactamente estas:
 *
 * <pre>
 *   ABIERTO     --iniciar revisión--&gt; EN_REVISION
 *   REABIERTO   --iniciar revisión--&gt; EN_REVISION
 *   EN_REVISION --cerrar--&gt;           CERRADO
 * </pre>
 *
 * <p>{@code CERRADO} a {@code REABIERTO} no está: nace de aceptar una apelación y es P10B.
 * Cualquier otra combinación responde 409 y no deja nada a medias.
 *
 * <p><b>Quién puede qué.</b> Asignar y reasignar las puede hacer cualquier administrador: repartir
 * trabajo es coordinación, y quien reasigna queda registrado en el historial. Iniciar la revisión y
 * cerrar exigen ser el responsable asignado, por el mismo motivo que aprobar una verificación exige
 * haberla tomado: una decisión la firma quien la estudió.
 *
 * <p><b>Resolver no sanciona.</b> Cerrar un caso como {@link ResultadoCasoModeracion#PROCEDENTE}
 * dice que amerita una decisión administrativa; no elige medida, no cambia ninguna cuenta y no
 * revoca ninguna sesión. Eso es P10B y siempre lo decide una persona, según la definición 11.3.
 *
 * <p><b>Cada mutación versiona.</b> Cambiar el caso y fotografiarlo son una sola transacción: se
 * bloquea la fila, se comprueba la transición, se aplica, se cierra la versión vigente con el mismo
 * instante en que empieza la nueva y se crea la siguiente. Si algo falla, no queda un caso mutado
 * sin historial ni dos versiones diciendo ser la actual.
 */
@Service
public class RevisionDeCasosService {

  /** Lo que la bandeja muestra sin filtros: lo que todavía espera una decisión. */
  private static final Collection<EstadoCasoModeracion> BANDEJA_POR_OMISION =
      List.of(
          EstadoCasoModeracion.ABIERTO,
          EstadoCasoModeracion.EN_REVISION,
          EstadoCasoModeracion.REABIERTO);

  private final CasoModeracionRepository casos;
  private final HistorialCasoRepository historial;
  private final SolicitudServicioService solicitudes;
  private final HiloDeSolicitudService hilos;
  private final ServicioPublicadoService servicios;
  private final AdministradorService administradores;
  private final UsuarioService usuarios;
  private final Clock reloj;

  public RevisionDeCasosService(
      CasoModeracionRepository casos,
      HistorialCasoRepository historial,
      SolicitudServicioService solicitudes,
      HiloDeSolicitudService hilos,
      ServicioPublicadoService servicios,
      AdministradorService administradores,
      UsuarioService usuarios,
      Clock reloj) {
    this.casos = casos;
    this.historial = historial;
    this.solicitudes = solicitudes;
    this.hilos = hilos;
    this.servicios = servicios;
    this.administradores = administradores;
    this.usuarios = usuarios;
    this.reloj = reloj;
  }

  /**
   * La bandeja de casos, del más antiguo al más reciente.
   *
   * <p>Sin filtros muestra lo que espera decisión: {@code ABIERTO}, {@code EN_REVISION} y {@code
   * REABIERTO}, que es el trabajo pendiente. Los cerrados se piden explícitamente, porque sirven
   * para consultar una decisión anterior y no para trabajarla.
   *
   * @param soloMios acota a los casos de los que la sesión es responsable
   */
  @Transactional(readOnly = true)
  public List<ResumenDeCasoAdministrativo> consultarBandeja(
      UsuarioAutenticado sujeto, Collection<EstadoCasoModeracion> estados, boolean soloMios) {

    exigirPermisosAdministrativos(sujeto);

    Collection<EstadoCasoModeracion> filtro =
        (estados == null || estados.isEmpty()) ? BANDEJA_POR_OMISION : estados;

    List<CasoModeracion> encontrados =
        soloMios
            ? casos
                .findByEstadoActualInAndIdAdministradorResponsableOrderByFechaAperturaAscIdCasoModeracionAsc(
                    filtro, sujeto.idUsuario())
            : casos.findByEstadoActualInOrderByFechaAperturaAscIdCasoModeracionAsc(filtro);

    return encontrados.stream().map(this::resumir).toList();
  }

  /**
   * El expediente completo de un caso.
   *
   * <p>Reúne el caso, la solicitud reportada con su historial de transiciones, las imágenes del
   * servicio contratado y las versiones del propio expediente. Los mensajes no: tienen su propia
   * ruta, {@link #consultarMensajes}.
   */
  @Transactional(readOnly = true)
  public DatosDeExpedienteDeCaso consultarExpediente(UsuarioAutenticado sujeto, Long idCaso) {
    exigirPermisosAdministrativos(sujeto);
    return describir(buscar(idCaso), sujeto);
  }

  /**
   * Los mensajes que los participantes se cruzaron en la solicitud reportada.
   *
   * <p>Es la única vía por la que el área administrativa alcanza una conversación privada, y está
   * atada al caso a propósito: <b>la ruta pide un caso, no una solicitud</b>. Sin un caso abierto
   * sobre esa solicitud no hay forma de leer su hilo desde aquí, ni siquiera conociendo el
   * identificador de la solicitud. Así el acceso queda limitado al contexto que lo justifica, como
   * exige la matriz de permisos del plan.
   *
   * <p>Solo lee. El área administrativa no participa en la conversación y no existe ninguna ruta
   * para enviar un mensaje desde aquí.
   */
  @Transactional(readOnly = true)
  public List<DatosDeMensajeSolicitud> consultarMensajes(UsuarioAutenticado sujeto, Long idCaso) {
    exigirPermisosAdministrativos(sujeto);
    return hilos.mensajesParaModeracion(buscar(idCaso).getIdSolicitudServicio());
  }

  /**
   * Deja el caso a cargo de una persona administradora, o se lo pasa a otra.
   *
   * <p>La misma operación cubre asignar y reasignar; lo que cambia es el detalle que queda en el
   * historial, para que una lectura posterior distinga la primera asignación de un traspaso.
   *
   * <p>No cambia el estado: un caso puede tener responsable y seguir {@code ABIERTO}. Asignar es
   * decir quién responde, no empezar a revisar.
   *
   * @throws ErrorDeAplicacion 404 si el caso no existe; 400 {@code ADMINISTRADOR_NO_VALIDO} si el
   *     destinatario no tiene el rol; 409 {@code TRANSICION_NO_PERMITIDA} si el caso está cerrado
   */
  @Transactional
  public DatosDeExpedienteDeCaso asignar(
      UsuarioAutenticado sujeto, Long idCaso, Long idAdministrador) {

    exigirPermisosAdministrativos(sujeto);

    if (!administradores.esAdministrador(idAdministrador)) {
      throw new ErrorDeAplicacion(
          HttpStatus.BAD_REQUEST,
          "ADMINISTRADOR_NO_VALIDO",
          "Esa cuenta no tiene permisos administrativos y no puede recibir un caso.");
    }

    CasoModeracion caso = bloquear(idCaso);
    // Un caso cerrado ya tiene decisión vigente: cambiarle el responsable haría
    // que la resolución dejara de decir quién la firmó. Se reabre en P10B.
    if (caso.getEstadoActual() == EstadoCasoModeracion.CERRADO) {
      throw transicionNoPermitida(caso);
    }

    if (idAdministrador.equals(caso.getIdAdministradorResponsable())) {
      // Reasignar a quien ya lo tiene no es un error, pero tampoco un evento:
      // una versión idéntica a la vigente solo ensuciaría el historial.
      return describir(caso, sujeto);
    }

    boolean esReasignacion = caso.getIdAdministradorResponsable() != null;
    OffsetDateTime instante = OffsetDateTime.now(reloj);
    caso.asignarResponsable(idAdministrador, instante);
    versionar(
        caso,
        sujeto,
        TipoEventoHistorial.RESPONSABLE_ASIGNADO,
        esReasignacion
            ? "El caso se reasignó a otra persona administradora."
            : "El caso se asignó a una persona administradora responsable.",
        instante);

    return describir(caso, sujeto);
  }

  /**
   * Pone el caso en revisión.
   *
   * <p>Se entra desde {@code ABIERTO} y desde {@code REABIERTO}: en los dos el expediente vuelve al
   * análisis. Exige responsable asignado y que sea quien lo pide, porque revisar sin que conste
   * quién lo hace dejaría una decisión sin autor.
   *
   * @throws ErrorDeAplicacion 404 si el caso no existe; 409 {@code CASO_SIN_RESPONSABLE} si nadie
   *     lo tiene asignado o {@code TRANSICION_NO_PERMITIDA} si el estado no lo admite; 403 {@code
   *     CASO_DE_OTRO_ADMINISTRADOR} si lo lleva otra persona
   */
  @Transactional
  public DatosDeExpedienteDeCaso iniciarRevision(UsuarioAutenticado sujeto, Long idCaso) {
    exigirPermisosAdministrativos(sujeto);

    CasoModeracion caso = bloquear(idCaso);
    if (caso.getEstadoActual() != EstadoCasoModeracion.ABIERTO
        && caso.getEstadoActual() != EstadoCasoModeracion.REABIERTO) {
      throw transicionNoPermitida(caso);
    }
    exigirQueSeaElResponsable(caso, sujeto);

    OffsetDateTime instante = OffsetDateTime.now(reloj);
    caso.iniciarRevision(instante);
    versionar(
        caso,
        sujeto,
        TipoEventoHistorial.ESTADO_CASO_CAMBIADO,
        "La persona responsable inició la revisión del caso.",
        instante);

    return describir(caso, sujeto);
  }

  /**
   * Cierra el caso con su resultado y su resolución.
   *
   * <p>Solo desde {@code EN_REVISION}: cerrar sin haber revisado saltaría la etapa que da sentido a
   * la decisión. Exige además ser el responsable.
   *
   * <p>No aplica ninguna medida ni cambia el estado de ninguna cuenta, tampoco con {@code
   * PROCEDENTE}. La versión que queda fotografía el estado de cuenta que la persona reportada tenía
   * en ese instante, que es el mismo que tenía antes.
   *
   * @throws ErrorDeAplicacion 404 si el caso no existe; 409 {@code TRANSICION_NO_PERMITIDA} si no
   *     está en revisión o {@code CASO_SIN_RESPONSABLE} si nadie lo tiene; 403 {@code
   *     CASO_DE_OTRO_ADMINISTRADOR} si lo lleva otra persona
   */
  @Transactional
  public DatosDeExpedienteDeCaso cerrar(
      UsuarioAutenticado sujeto,
      Long idCaso,
      ResultadoCasoModeracion resultado,
      String resolucion) {

    exigirPermisosAdministrativos(sujeto);

    CasoModeracion caso = bloquear(idCaso);
    if (caso.getEstadoActual() != EstadoCasoModeracion.EN_REVISION) {
      throw transicionNoPermitida(caso);
    }
    exigirQueSeaElResponsable(caso, sujeto);

    OffsetDateTime instante = OffsetDateTime.now(reloj);
    caso.cerrar(resultado, resolucion, instante);
    versionar(
        caso,
        sujeto,
        TipoEventoHistorial.RESOLUCION_REGISTRADA,
        "El caso se cerró con resultado " + resultado + " y su resolución.",
        instante);

    return describir(caso, sujeto);
  }

  /**
   * Cierra la versión vigente y crea la siguiente, dentro de la transacción ya abierta.
   *
   * <p>El mismo instante sirve de fin de una y de inicio de la otra. El intervalo es semiabierto,
   * de modo que los dos periodos se tocan sin superponerse y {@code ex_historial_caso_vigencia} los
   * admite; el índice parcial {@code uq_historial_caso_version_actual} garantiza que solo quede una
   * vigente.
   *
   * <p>Los dos {@code saveAndFlush} son deliberados. El primero ordena las escrituras: sin él,
   * Hibernate insertaría la versión nueva antes de actualizar la anterior y el índice parcial de
   * versión vigente rechazaría la operación. El segundo obliga a PostgreSQL a comprobar la
   * exclusión temporal y la unicidad <b>aquí</b> y no al confirmar, de modo que un conflicto real
   * salga como el fallo de esta operación y no como un error tardío sin contexto.
   */
  private void versionar(
      CasoModeracion caso,
      UsuarioAutenticado sujeto,
      TipoEventoHistorial evento,
      String detalle,
      OffsetDateTime instante) {

    HistorialCaso vigente =
        historial
            .findByIdCasoModeracionAndEsVersionActualTrue(caso.getIdCasoModeracion())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "El caso "
                            + caso.getIdCasoModeracion()
                            + " no tiene versión vigente; su apertura debió crearla"));

    vigente.cerrarVigencia(instante);
    // Este vaciado no es opcional: Hibernate ordena sus inserciones antes que
    // sus actualizaciones dentro de un mismo vaciado, así que la versión nueva
    // llegaría a la base mientras la anterior sigue marcada como vigente y
    // `uq_historial_caso_version_actual` la rechazaría. Escribir el cierre
    // primero es lo que deja el índice libre para la que entra.
    historial.saveAndFlush(vigente);

    // El estado de la cuenta reportada se lee, no se cambia: P10A no sanciona.
    EstadoCuenta estadoCuentaAfectada = usuarios.obtener(caso.getIdReportado()).estadoCuenta();

    historial.saveAndFlush(
        HistorialCaso.siguienteDe(
            caso,
            vigente.getNumeroVersion() + 1,
            sujeto.idUsuario(),
            evento,
            estadoCuentaAfectada,
            detalle,
            instante));
  }

  /** El expediente de un caso ya cargado y con los permisos ya comprobados. */
  private DatosDeExpedienteDeCaso describir(CasoModeracion caso, UsuarioAutenticado sujeto) {
    DatosDeSolicitudServicio solicitud =
        solicitudes.detalleParaModeracion(caso.getIdSolicitudServicio());

    List<DatosDeVersionDeCaso> versiones =
        historial.findByIdCasoModeracionOrderByNumeroVersionAsc(caso.getIdCasoModeracion()).stream()
            .map(
                version ->
                    DatosDeVersionDeCaso.de(
                        version,
                        nombreDeCuenta(version.getIdActor()),
                        nombreDeCuenta(version.getIdAdministradorResponsable())))
            .toList();

    return new DatosDeExpedienteDeCaso(
        resumir(caso),
        caso.getDescripcion(),
        caso.getResolucionActual(),
        solicitud,
        servicios.describirImagenesDe(solicitud.idServicioPublicado()),
        versiones,
        esResponsable(caso, sujeto));
  }

  private ResumenDeCasoAdministrativo resumir(CasoModeracion caso) {
    Long responsable = caso.getIdAdministradorResponsable();
    return ResumenDeCasoAdministrativo.de(
        caso,
        usuarios.obtener(caso.getIdReportante()).nombreCompleto(),
        usuarios.obtener(caso.getIdReportado()).nombreCompleto(),
        responsable == null ? null : usuarios.obtener(responsable).nombreCompleto());
  }

  /**
   * El nombre de una cuenta, o nulo si no hay ninguna.
   *
   * <p>Lo usan por igual el actor de una versión —nulo cuando la originó el sistema— y su
   * responsable —nulo mientras nadie tuviera el caso asignado—. Son campos distintos con la misma
   * ausencia posible, así que se resuelven igual.
   */
  private String nombreDeCuenta(Long idUsuario) {
    return idUsuario == null ? null : usuarios.obtener(idUsuario).nombreCompleto();
  }

  private static boolean esResponsable(CasoModeracion caso, UsuarioAutenticado sujeto) {
    return sujeto.idUsuario().equals(caso.getIdAdministradorResponsable());
  }

  private CasoModeracion buscar(Long idCaso) {
    return casos.findById(idCaso).orElseThrow(RevisionDeCasosService::casoNoEncontrado);
  }

  private CasoModeracion bloquear(Long idCaso) {
    return casos.bloquearPorId(idCaso).orElseThrow(RevisionDeCasosService::casoNoEncontrado);
  }

  private static void exigirQueSeaElResponsable(CasoModeracion caso, UsuarioAutenticado sujeto) {
    if (caso.getIdAdministradorResponsable() == null) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "CASO_SIN_RESPONSABLE",
          "Asigna primero una persona responsable del caso.");
    }
    if (!esResponsable(caso, sujeto)) {
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "CASO_DE_OTRO_ADMINISTRADOR",
          "Este caso lo lleva otra persona administradora. Solo quien lo tiene asignado puede"
              + " revisarlo y resolverlo.");
    }
  }

  private static ErrorDeAplicacion transicionNoPermitida(CasoModeracion caso) {
    // El mensaje nombra el estado real porque es información administrativa que
    // quien revisa necesita para entender qué pasó mientras tenía la pantalla
    // abierta.
    return new ErrorDeAplicacion(
        HttpStatus.CONFLICT,
        "TRANSICION_NO_PERMITIDA",
        "Esa acción no está disponible: el caso está en estado " + caso.getEstadoActual() + ".");
  }

  private static void exigirPermisosAdministrativos(UsuarioAutenticado sujeto) {
    if (sujeto == null || !sujeto.puedeAdministrar()) {
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "ACCESO_DENEGADO",
          "Esta cuenta no tiene permisos administrativos.");
    }
  }

  private static ErrorDeAplicacion casoNoEncontrado() {
    return new ErrorDeAplicacion(
        HttpStatus.NOT_FOUND, "CASO_NO_ENCONTRADO", "Ese caso de moderación no existe.");
  }
}
