package com.moica.moderacion.service;

import com.moica.moderacion.entity.CasoModeracion;
import com.moica.moderacion.entity.MedidaAdministrativa;
import com.moica.moderacion.entity.TipoEventoHistorial;
import com.moica.moderacion.repository.CasoModeracionRepository;
import com.moica.usuario.entity.EstadoCuenta;
import com.moica.usuario.service.UsuarioService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El vencimiento de las medidas temporales que una persona ya había decidido.
 *
 * <p>Esto <b>no</b> es una sanción automática. No elige medida, no escala severidad, no mira
 * reincidencia y no sanciona a nadie: se limita a ejecutar el plazo que una persona administradora
 * fijó al aplicar la medida, que es exactamente lo que la definición 11.3 sí admite en el MVP
 * («únicamente ejecuta el plazo de una decisión humana previa»). Por eso vive aparte de {@link
 * MedidasDeCasoService}, donde están las decisiones, y por eso sus versiones del historial quedan a
 * nombre del sistema y no de nadie.
 *
 * <p>Al vencer, el caso suelta la medida y la cuenta vuelve a {@link EstadoCuenta#ACTIVA}. Vuelve
 * sin más comprobaciones porque D-MOD-03 garantiza que no había ninguna otra medida esperando
 * debajo: una cuenta sostiene como máximo una.
 *
 * <p><b>Idempotente y segura ante carreras.</b> El barrido vuelve a comprobar cada caso después de
 * bloquearlo, así que una medida revocada a mano o sustituida entre la consulta y el bloqueo
 * simplemente se salta. Ejecutarlo dos veces seguidas no cambia nada la segunda vez: sin medida
 * vigente no hay nada que expirar. El orden de bloqueo es el mismo que usan las decisiones
 * administrativas —primero la cuenta, después el expediente—, de modo que un barrido y una
 * revocación simultáneas se serializan en lugar de abrazarse.
 *
 * <p>No revoca sesiones: volver a {@code ACTIVA} devuelve acceso, no lo quita.
 */
@Service
public class ExpiracionDeMedidas {

  private final CasoModeracionRepository casos;
  private final CatalogoDeMedidasService catalogo;
  private final VersionadoDeCasos versionado;
  private final UsuarioService usuarios;
  private final Clock reloj;

  public ExpiracionDeMedidas(
      CasoModeracionRepository casos,
      CatalogoDeMedidasService catalogo,
      VersionadoDeCasos versionado,
      UsuarioService usuarios,
      Clock reloj) {
    this.casos = casos;
    this.catalogo = catalogo;
    this.versionado = versionado;
    this.usuarios = usuarios;
    this.reloj = reloj;
  }

  /**
   * Levanta todas las medidas temporales cuyo plazo ya se cumplió.
   *
   * <p>Corre periódicamente y también se invoca directamente desde las pruebas, que es lo que
   * permite comprobar el vencimiento sin esperar tiempo real: la fecha de fin se envejece en la
   * base y el barrido se llama a mano.
   *
   * <p>Todo el barrido va en una sola transacción. Es lo más simple que cumple lo que hace falta —o
   * se aplican todos los vencimientos o ninguno, y un fallo deja la base como estaba para que la
   * pasada siguiente lo reintente— y a la escala del MVP no hay motivo para trocearlo.
   *
   * @return cuántas medidas se levantaron, para que quien lo invoque pueda comprobarlo
   */
  @Scheduled(
      fixedDelayString = "${moica.moderacion.periodo-de-expiracion:PT1M}",
      initialDelayString = "${moica.moderacion.periodo-de-expiracion:PT1M}")
  @Transactional
  public int expirarLasVencidas() {
    OffsetDateTime instante = OffsetDateTime.now(reloj);

    // La consulta devuelve identificadores y no entidades: cargar los casos aquí
    // los dejaría en el contexto de persistencia, y entonces el bloqueo que toma
    // `expirar` devolvería esa copia en memoria en lugar de releer la fila. El
    // barrido trabajaría con el estado anterior al bloqueo y podría expirar una
    // medida que otra transacción acababa de revocar o sustituir.
    List<Long> vencidos = casos.idsConMedidaVencida(instante);

    int levantadas = 0;
    for (Long idCaso : vencidos) {
      if (expirar(idCaso, instante)) {
        levantadas++;
      }
    }
    return levantadas;
  }

  /**
   * Levanta la medida de un caso concreto, si al bloquearlo sigue habiendo algo que levantar.
   *
   * <p>La segunda comprobación no sobra: entre la consulta que lo encontró vencido y el bloqueo,
   * otra transacción pudo revocar la medida o sustituirla por una con otro plazo. Sin ella el
   * barrido crearía una versión {@code MEDIDA_EXPIRADA} de una medida que ya no existía, o
   * devolvería a {@code ACTIVA} una cuenta que acababa de recibir una sanción nueva.
   *
   * @return si de verdad se levantó una medida
   */
  private boolean expirar(Long idCaso, OffsetDateTime instante) {
    Long idReportado = casos.idReportadoDe(idCaso).orElse(null);
    if (idReportado == null) {
      return false;
    }

    // La cuenta primero y el expediente después, el mismo orden que usan las
    // decisiones administrativas. El caso se lee ya bloqueado, nunca antes.
    usuarios.bloquearCuenta(idReportado);
    CasoModeracion caso = casos.bloquearPorId(idCaso).orElse(null);

    if (caso == null || !yaVencio(caso, instante)) {
      return false;
    }

    MedidaAdministrativa expirada = catalogo.obtener(caso.getIdMedidaAdministrativaActual());

    caso.retirarMedida(instante);
    usuarios.proyectarEstadoDeCuenta(caso.getIdReportado(), EstadoCuenta.ACTIVA, null);

    versionado.versionarPorElSistema(
        caso,
        TipoEventoHistorial.MEDIDA_EXPIRADA,
        EstadoCuenta.ACTIVA,
        "Terminó el plazo de la medida «"
            + expirada.getNombre()
            + "» y la cuenta volvió a estar activa.",
        instante);

    return true;
  }

  private static boolean yaVencio(CasoModeracion caso, OffsetDateTime instante) {
    return caso.getIdMedidaAdministrativaActual() != null
        && caso.getFechaFinMedidaActual() != null
        && !caso.getFechaFinMedidaActual().isAfter(instante);
  }
}
