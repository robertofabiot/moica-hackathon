package com.moica.moderacion.controller;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.moderacion.dto.ApelacionRecibida;
import com.moica.moderacion.dto.DatosDeExpedienteDeCaso;
import com.moica.moderacion.dto.MedidaAAplicar;
import com.moica.moderacion.dto.ReaperturaDeCaso;
import com.moica.moderacion.dto.ResolucionDeApelacion;
import com.moica.moderacion.dto.RevocacionDeMedida;
import com.moica.moderacion.service.ApelacionesDeCasoService;
import com.moica.moderacion.service.MedidasDeCasoService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las medidas y las apelaciones de un caso de moderación.
 *
 * <p>Cuelga de {@code /api/admin/casos/{id}} porque una sanción no se decide en abstracto: nace de
 * un expediente concreto y con una resolución detrás. No existe ninguna ruta para sancionar una
 * cuenta sin pasar por su caso.
 *
 * <p>Cada acción es un recurso propio, igual que en la revisión: aplicar, revocar, registrar la
 * apelación, resolverla y reabrir el caso. Así una operación que el caso no admite responde 409
 * explicando su estado real, en lugar de convertirse en un valor extraño dentro de un cuerpo.
 *
 * <p><b>No hay ninguna ruta pública de apelación</b>, ni la habrá en el MVP. La apelación llega por
 * el canal externo que la aplicación muestra junto al aviso de la medida, y lo que existe aquí es
 * el registro administrativo de lo recibido, según la decisión D-MOD-04.
 *
 * <p>Todas devuelven el expediente completo ya actualizado, para que la interfaz pinte el resultado
 * sin encadenar una segunda consulta que podría llegar tarde.
 */
@RestController
@RequestMapping("/api/admin/casos/{idCasoModeracion}")
public class MedidasDeCasoController {

  private final MedidasDeCasoService medidas;
  private final ApelacionesDeCasoService apelaciones;

  public MedidasDeCasoController(
      MedidasDeCasoService medidas, ApelacionesDeCasoService apelaciones) {
    this.medidas = medidas;
    this.apelaciones = apelaciones;
  }

  /**
   * Aplica a la cuenta reportada la medida elegida.
   *
   * <p>Si la cuenta ya sostiene otra, responde 409 {@code MEDIDA_VIGENTE_EXISTENTE} y no cambia
   * nada. Reenviar con {@code confirmaReemplazo} revoca la anterior y aplica la nueva en la misma
   * transacción.
   */
  @PostMapping("/medida")
  public DatosDeExpedienteDeCaso aplicarMedida(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idCasoModeracion,
      @Valid @RequestBody MedidaAAplicar pedido) {
    return medidas.aplicar(sujeto, idCasoModeracion, pedido);
  }

  /** Levanta la medida que este caso sostenía y devuelve la cuenta a {@code ACTIVA}. */
  @PostMapping("/medida/revocacion")
  public DatosDeExpedienteDeCaso revocarMedida(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idCasoModeracion,
      @Valid @RequestBody RevocacionDeMedida pedido) {
    return medidas.revocar(sujeto, idCasoModeracion, pedido.motivo());
  }

  /** Registra en el expediente una apelación recibida por el canal externo de soporte. */
  @PostMapping("/apelacion")
  public DatosDeExpedienteDeCaso registrarApelacion(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idCasoModeracion,
      @Valid @RequestBody ApelacionRecibida pedido) {
    return apelaciones.registrar(sujeto, idCasoModeracion, pedido.relato());
  }

  /** Acepta o rechaza la apelación registrada. Aceptarla no reabre el caso por sí sola. */
  @PostMapping("/apelacion/resolucion")
  public DatosDeExpedienteDeCaso resolverApelacion(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idCasoModeracion,
      @Valid @RequestBody ResolucionDeApelacion pedido) {
    return apelaciones.resolver(sujeto, idCasoModeracion, pedido.aceptada(), pedido.resolucion());
  }

  /** Devuelve a revisión un caso cerrado cuya apelación fue aceptada. */
  @PostMapping("/reapertura")
  public DatosDeExpedienteDeCaso reabrir(
      @AuthenticationPrincipal UsuarioAutenticado sujeto,
      @PathVariable Long idCasoModeracion,
      @Valid @RequestBody ReaperturaDeCaso pedido) {
    return apelaciones.reabrir(sujeto, idCasoModeracion, pedido.motivo());
  }
}
