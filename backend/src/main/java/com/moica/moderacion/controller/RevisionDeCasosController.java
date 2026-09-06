package com.moica.moderacion.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.chat.dto.DatosDeMensajeSolicitud;
import com.moica.moderacion.dto.AsignacionDeCaso;
import com.moica.moderacion.dto.DatosDeExpedienteDeCaso;
import com.moica.moderacion.dto.ResolucionDeCaso;
import com.moica.moderacion.dto.ResumenDeCasoAdministrativo;
import com.moica.moderacion.entity.EstadoCasoModeracion;
import com.moica.moderacion.service.RevisionDeCasosService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La revisión administrativa de casos de moderación.
 *
 * <p>Cuelga de {@code /api/admin}, así que hereda sus dos condiciones simultáneas: rol
 * administrativo y segundo factor verificado en <b>esa</b> sesión. Las impone la cadena de
 * seguridad, no cada método, y el servicio las repite como última red.
 *
 * <p>Cada acción es un recurso propio —{@code /asignacion}, {@code /revision}, {@code /cierre}— en
 * lugar de un campo de estado que se sobrescribe, igual que en la revisión de verificaciones: así
 * una transición inválida no es un valor raro en un cuerpo sino una operación que el caso no
 * admite, y el 409 explica cuál es su estado real.
 *
 * <p>No hay {@code PUT}, {@code PATCH} ni {@code DELETE}. Un caso no se edita ni se borra: es la
 * evidencia de una investigación y solo avanza por sus transiciones.
 *
 * <p>Los mensajes cuelgan del caso y no de la solicitud, a propósito: leer una conversación privada
 * exige un expediente que lo justifique.
 */
@RestController
@RequestMapping("/api/admin/casos")
public class RevisionDeCasosController {

  private final RevisionDeCasosService servicio;

  public RevisionDeCasosController(RevisionDeCasosService servicio) {
    this.servicio = servicio;
  }

  /**
   * La bandeja de casos.
   *
   * <p>Sin parámetros devuelve lo que espera decisión: {@code ABIERTO}, {@code EN_REVISION} y
   * {@code REABIERTO}. Con {@code estado} se piden otros —{@code CERRADO}, por ejemplo, para
   * consultar una decisión anterior— y con {@code mios} se acota a los propios.
   */
  @GetMapping
  public List<ResumenDeCasoAdministrativo> consultarBandeja(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @RequestParam(name = "estado", required = false) List<EstadoCasoModeracion> estados,
      @RequestParam(name = "mios", defaultValue = "false") boolean soloMios) {
    return servicio.consultarBandeja(sujeto, estados, soloMios);
  }

  /** El expediente de un caso: su decisión vigente, la solicitud, las evidencias y el historial. */
  @GetMapping("/{idCasoModeracion}")
  public DatosDeExpedienteDeCaso consultarExpediente(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idCasoModeracion) {
    return servicio.consultarExpediente(sujeto, idCasoModeracion);
  }

  /** El hilo de mensajes de la solicitud reportada, dentro del contexto de este caso. */
  @GetMapping("/{idCasoModeracion}/mensajes")
  public List<DatosDeMensajeSolicitud> consultarMensajes(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idCasoModeracion) {
    return servicio.consultarMensajes(sujeto, idCasoModeracion);
  }

  /** Asigna o reasigna la persona responsable del caso. */
  @PostMapping("/{idCasoModeracion}/asignacion")
  public DatosDeExpedienteDeCaso asignar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idCasoModeracion,
      @Valid @RequestBody AsignacionDeCaso asignacion) {
    return servicio.asignar(sujeto, idCasoModeracion, asignacion.idAdministrador());
  }

  /** Pone el caso en revisión. Solo quien lo tiene asignado. */
  @PostMapping("/{idCasoModeracion}/revision")
  public DatosDeExpedienteDeCaso iniciarRevision(
      @AuthenticationPrincipal UsuarioAutenticado sujeto, @PathVariable Long idCasoModeracion) {
    return servicio.iniciarRevision(sujeto, idCasoModeracion);
  }

  /** Cierra el caso con su resultado y su resolución. Solo quien lo tiene asignado. */
  @PostMapping("/{idCasoModeracion}/cierre")
  public DatosDeExpedienteDeCaso cerrar(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idCasoModeracion,
      @Valid @RequestBody ResolucionDeCaso resolucion) {
    return servicio.cerrar(
        sujeto, idCasoModeracion, resolucion.resultado(), resolucion.resolucion());
  }
}
