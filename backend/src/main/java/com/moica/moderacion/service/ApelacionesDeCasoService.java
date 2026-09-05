package com.moica.moderacion.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.moderacion.dto.DatosDeExpedienteDeCaso;
import com.moica.moderacion.dto.EstadoDeApelacion;
import com.moica.moderacion.entity.CasoModeracion;
import com.moica.moderacion.entity.EstadoCasoModeracion;
import com.moica.moderacion.entity.TipoEventoHistorial;
import com.moica.moderacion.repository.CasoModeracionRepository;
import com.moica.moderacion.repository.HistorialCasoRepository;
import com.moica.usuario.service.UsuarioService;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las apelaciones de un caso y la reapertura que pueden justificar.
 *
 * <p><b>La apelación no se presenta dentro de Moica.</b> No hay formulario, ni endpoint público, ni
 * adjuntos, ni buzón, ni correo automatizado: la decisión D-MOD-04 y la definición 11.5 lo excluyen
 * del MVP. La aplicación se limita a <em>mostrar</em> el canal externo junto al aviso de la medida,
 * y lo que existe aquí es el registro administrativo de lo que llegó por ese canal.
 *
 * <p>Ninguna de estas operaciones toca una cuenta ni una sesión. Aceptar una apelación no levanta
 * la sanción: si quien revisa decide levantarla, la revoca desde {@link MedidasDeCasoService}, y
 * esa es otra decisión con su propio motivo y su propio evento. Por eso este servicio solo necesita
 * bloquear el expediente.
 *
 * <p><b>La apelación no es una tabla.</b> El diccionario de datos no la modela como entidad: la
 * representa con los eventos del historial, y su estado se lee de ahí con {@link
 * EstadoDeApelacion#deLasVersiones}. No hay ningún dato que guardar aparte ni que mantener
 * sincronizado.
 *
 * <p>Las tres operaciones son deliberadamente distintas, una decisión cada una:
 *
 * <pre>
 *   registrar   CERRADO, sin apelación pendiente  --&gt; APELACION_PRESENTADA
 *   resolver    con apelación pendiente           --&gt; APELACION_ACEPTADA | APELACION_RECHAZADA
 *   reabrir     CERRADO, apelación aceptada       --&gt; CASO_REABIERTO   (CERRADO -&gt; REABIERTO)
 * </pre>
 *
 * <p>Aceptar y reabrir van separadas porque la definición 11.5 las separa: el administrador la
 * aceptará o la rechazará «y, <b>cuando proceda</b>, reabrirá el mismo expediente». A veces basta
 * con aceptarla y revocar la medida sin volver a investigar. Separarlas deja además un evento por
 * decisión, que es como versiona el resto del expediente, en lugar de dos fotografías en el mismo
 * instante que la exclusión temporal de {@code V51} no admitiría.
 */
@Service
public class ApelacionesDeCasoService {

  private final CasoModeracionRepository casos;
  private final HistorialCasoRepository historial;
  private final RevisionDeCasosService expedientes;
  private final VersionadoDeCasos versionado;
  private final UsuarioService usuarios;
  private final Clock reloj;

  public ApelacionesDeCasoService(
      CasoModeracionRepository casos,
      HistorialCasoRepository historial,
      RevisionDeCasosService expedientes,
      VersionadoDeCasos versionado,
      UsuarioService usuarios,
      Clock reloj) {
    this.casos = casos;
    this.historial = historial;
    this.expedientes = expedientes;
    this.versionado = versionado;
    this.usuarios = usuarios;
    this.reloj = reloj;
  }

  /**
   * Registra en el expediente una apelación recibida por el canal externo.
   *
   * <p>El actor de la versión es la persona administradora que registra, no quien apeló. Es lo
   * honesto: dentro de Moica el acto verificable es el registro, y la persona sancionada no ejecutó
   * nada aquí —ni podría: una suspensión le revocó las sesiones—. El detalle deja constancia de que
   * lo apelado vino de fuera.
   *
   * <p>Solo sobre un caso {@code CERRADO}: apelar es discutir una decisión, y un caso sin decisión
   * vigente no tiene nada que discutir. Tampoco se admite una segunda mientras la anterior siga sin
   * resolverse, porque entonces no se sabría cuál se está resolviendo.
   *
   * @throws ErrorDeAplicacion 404 si el caso no existe; 403 {@code CASO_DE_OTRO_ADMINISTRADOR}; 409
   *     {@code CASO_SIN_RESPONSABLE}, {@code TRANSICION_NO_PERMITIDA} o {@code APELACION_PENDIENTE}
   */
  @Transactional
  public DatosDeExpedienteDeCaso registrar(UsuarioAutenticado sujeto, Long idCaso, String relato) {

    GuardiasDeCaso.exigirPermisoAdministrativo(sujeto);

    CasoModeracion caso = bloquear(idCaso);
    GuardiasDeCaso.exigirQueSeaElResponsable(caso, sujeto);

    if (caso.getEstadoActual() != EstadoCasoModeracion.CERRADO) {
      throw GuardiasDeCaso.transicionNoPermitida(
          caso, "Solo se registra una apelación sobre un caso cerrado con su decisión vigente.");
    }
    if (apelacionDe(idCaso) == EstadoDeApelacion.PENDIENTE) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "APELACION_PENDIENTE",
          "Este caso ya tiene una apelación registrada sin resolver. Resuélvela antes de registrar"
              + " otra.");
    }

    versionar(
        caso,
        sujeto,
        TipoEventoHistorial.APELACION_PRESENTADA,
        "La persona afectada apeló por el canal externo de soporte y se registró aquí. " + relato);

    return expedientes.consultarExpediente(sujeto, idCaso);
  }

  /**
   * Acepta o rechaza la apelación registrada.
   *
   * <p>Aceptar no reabre el caso ni levanta la medida por sí solo: habilita la reapertura y deja
   * constancia. Rechazarla mantiene la decisión vigente tal como estaba.
   *
   * @throws ErrorDeAplicacion 404 si el caso no existe; 403 {@code CASO_DE_OTRO_ADMINISTRADOR}; 409
   *     {@code CASO_SIN_RESPONSABLE} o {@code SIN_APELACION_PENDIENTE}
   */
  @Transactional
  public DatosDeExpedienteDeCaso resolver(
      UsuarioAutenticado sujeto, Long idCaso, boolean aceptada, String resolucion) {

    GuardiasDeCaso.exigirPermisoAdministrativo(sujeto);

    CasoModeracion caso = bloquear(idCaso);
    GuardiasDeCaso.exigirQueSeaElResponsable(caso, sujeto);

    if (apelacionDe(idCaso) != EstadoDeApelacion.PENDIENTE) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "SIN_APELACION_PENDIENTE",
          "Este caso no tiene ninguna apelación esperando decisión.");
    }

    versionar(
        caso,
        sujeto,
        aceptada ? TipoEventoHistorial.APELACION_ACEPTADA : TipoEventoHistorial.APELACION_RECHAZADA,
        (aceptada ? "Se aceptó la apelación. " : "Se rechazó la apelación. ") + resolucion);

    return expedientes.consultarExpediente(sujeto, idCaso);
  }

  /**
   * Devuelve a revisión un caso cerrado cuya apelación prosperó.
   *
   * <p>Completa la única transición que P10A dejó pendiente, {@code CERRADO → REABIERTO}, y exige
   * exactamente lo que la definición 11.1 pone como condición: una apelación aceptada. Después el
   * caso sigue el camino que ya existía, {@code REABIERTO → EN_REVISION}.
   *
   * <p><b>La resolución anterior no se pierde.</b> La fila del caso la suelta porque {@code
   * ck_caso_moderacion_cierre} solo admite resultado, resolución y fecha de cierre en {@code
   * CERRADO}: una decisión que dejó de ser definitiva no puede seguir figurando como vigente. La
   * versión del historial que la registró la conserva íntegra, y por eso reabrir crea una versión
   * nueva en lugar de reescribir la anterior.
   *
   * <p>La medida sí sobrevive: volver a mirar el expediente no absuelve a nadie.
   *
   * <p>Reabrir <b>consume</b> la apelación aceptada —así lo lee {@link
   * EstadoDeApelacion#deLasVersiones}—, de modo que reabrir dos veces exige que la persona vuelva a
   * apelar por el canal externo.
   *
   * @throws ErrorDeAplicacion 404 si el caso no existe; 403 {@code CASO_DE_OTRO_ADMINISTRADOR}; 409
   *     {@code CASO_SIN_RESPONSABLE}, {@code TRANSICION_NO_PERMITIDA} o {@code
   *     APELACION_NO_ACEPTADA}
   */
  @Transactional
  public DatosDeExpedienteDeCaso reabrir(UsuarioAutenticado sujeto, Long idCaso, String motivo) {
    GuardiasDeCaso.exigirPermisoAdministrativo(sujeto);

    CasoModeracion caso = bloquear(idCaso);
    GuardiasDeCaso.exigirQueSeaElResponsable(caso, sujeto);

    if (caso.getEstadoActual() != EstadoCasoModeracion.CERRADO) {
      throw GuardiasDeCaso.transicionNoPermitida(caso, "Solo se reabre un caso cerrado.");
    }
    if (apelacionDe(idCaso) != EstadoDeApelacion.ACEPTADA) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "APELACION_NO_ACEPTADA",
          "Un caso solo se reabre cuando su apelación fue aceptada.");
    }

    OffsetDateTime instante = OffsetDateTime.now(reloj);
    caso.reabrir(instante);

    versionado.versionar(
        caso,
        sujeto.idUsuario(),
        TipoEventoHistorial.CASO_REABIERTO,
        usuarios.obtener(caso.getIdReportado()).estadoCuenta(),
        "El caso volvió a revisión tras aceptarse la apelación. " + motivo,
        instante);

    return expedientes.consultarExpediente(sujeto, idCaso);
  }

  /**
   * Versiona un evento de apelación.
   *
   * <p>El estado de la cuenta se <b>lee</b>: registrar, aceptar o rechazar una apelación no cambia
   * el acceso de nadie. La fotografía retrata la situación tal como está, que es justamente el dato
   * que una revisión posterior necesita para entender en qué condiciones se decidió.
   */
  private void versionar(
      CasoModeracion caso, UsuarioAutenticado sujeto, TipoEventoHistorial evento, String detalle) {

    versionado.versionar(
        caso,
        sujeto.idUsuario(),
        evento,
        usuarios.obtener(caso.getIdReportado()).estadoCuenta(),
        detalle,
        OffsetDateTime.now(reloj));
  }

  private EstadoDeApelacion apelacionDe(Long idCaso) {
    return EstadoDeApelacion.deLasVersiones(
        historial.findByIdCasoModeracionOrderByNumeroVersionAsc(idCaso));
  }

  private CasoModeracion bloquear(Long idCaso) {
    return casos.bloquearPorId(idCaso).orElseThrow(GuardiasDeCaso::casoNoEncontrado);
  }
}
