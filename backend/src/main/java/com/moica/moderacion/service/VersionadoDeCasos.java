package com.moica.moderacion.service;

import com.moica.moderacion.entity.CasoModeracion;
import com.moica.moderacion.entity.HistorialCaso;
import com.moica.moderacion.entity.TipoEventoHistorial;
import com.moica.moderacion.repository.HistorialCasoRepository;
import com.moica.usuario.entity.EstadoCuenta;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/**
 * El encadenado SCD2 del historial de un caso, en un solo sitio.
 *
 * <p>P10A lo estrenó dentro de {@code RevisionDeCasosService} porque era el único que versionaba.
 * P10B añade un segundo escritor —las medidas, las apelaciones y la reapertura— y una regla como
 * esta, que depende de un índice único parcial y de una restricción de exclusión temporal, no puede
 * vivir copiada en dos clases: la segunda copia es la que se olvida de vaciar en el orden correcto.
 *
 * <p>Cada llamada cierra la versión vigente y crea la siguiente dentro de la transacción que ya
 * abrió quien llama. <b>No abre transacción propia a propósito:</b> versionar sin el cambio que
 * describe, o el cambio sin su versión, dejaría el expediente mintiendo.
 */
@Component
class VersionadoDeCasos {

  private final HistorialCasoRepository historial;

  VersionadoDeCasos(HistorialCasoRepository historial) {
    this.historial = historial;
  }

  /**
   * Fotografía un cambio que originó una persona administradora.
   *
   * @param caso el caso <b>ya mutado</b>: la versión retrata el estado resultante, no el anterior
   * @param estadoCuentaAfectada el estado de la cuenta reportada <b>después</b> del cambio. Se
   *     recibe en lugar de leerlo aquí porque en P10B la misma transacción puede haberlo movido, y
   *     una consulta intermedia podría devolver el valor anterior
   */
  HistorialCaso versionar(
      CasoModeracion caso,
      Long idAdministradorActor,
      TipoEventoHistorial evento,
      EstadoCuenta estadoCuentaAfectada,
      String detalle,
      OffsetDateTime instante) {

    HistorialCaso anterior = cerrarLaVigente(caso, instante);

    return historial.saveAndFlush(
        HistorialCaso.siguienteDe(
            caso,
            anterior.getNumeroVersion() + 1,
            idAdministradorActor,
            evento,
            estadoCuentaAfectada,
            detalle,
            instante));
  }

  /**
   * Fotografía un cambio que no originó nadie, como el vencimiento de un plazo ya decidido.
   *
   * <p>Es idéntico al anterior salvo por el actor. Se separa en dos métodos y no en un parámetro
   * porque quien versiona sabe siempre cuál de los dos está haciendo, y un enumerado suelto en la
   * llamada invitaría a marcar como del sistema una decisión que sí tomó una persona.
   */
  HistorialCaso versionarPorElSistema(
      CasoModeracion caso,
      TipoEventoHistorial evento,
      EstadoCuenta estadoCuentaAfectada,
      String detalle,
      OffsetDateTime instante) {

    HistorialCaso anterior = cerrarLaVigente(caso, instante);

    return historial.saveAndFlush(
        HistorialCaso.delSistemaDe(
            caso,
            anterior.getNumeroVersion() + 1,
            evento,
            estadoCuentaAfectada,
            detalle,
            instante));
  }

  /**
   * Cierra la versión vigente y la escribe antes de que nadie inserte la siguiente.
   *
   * <p>El {@code saveAndFlush} no es opcional: Hibernate agrupa sus inserciones antes que sus
   * actualizaciones dentro de un mismo vaciado, así que la versión nueva llegaría a la base
   * mientras la anterior sigue marcada como vigente y {@code uq_historial_caso_version_actual} la
   * rechazaría con una clave duplicada que no dice nada del orden. Escribir el cierre primero es lo
   * que deja el índice libre para la que entra.
   *
   * <p>El mismo instante sirve de fin de una y de inicio de la otra. El intervalo es semiabierto,
   * de modo que los dos periodos se tocan sin superponerse y {@code ex_historial_caso_vigencia} los
   * admite.
   */
  private HistorialCaso cerrarLaVigente(CasoModeracion caso, OffsetDateTime instante) {
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
    return historial.saveAndFlush(vigente);
  }
}
