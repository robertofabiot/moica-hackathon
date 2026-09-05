package com.moica.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.moderacion.service.ExpiracionDeMedidas;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * El vencimiento automático de una medida temporal, de extremo a extremo.
 *
 * <p>Cubre lo que fija el criterio de salida de P10B para la expiración: solo finaliza una medida
 * temporal que <b>una persona ya había elegido</b>, devuelve la cuenta a {@code ACTIVA}, deja el
 * evento {@code MEDIDA_EXPIRADA} a nombre del sistema y es idempotente y segura ante carreras con
 * una revocación o un reemplazo manual.
 *
 * <p>El barrido se invoca directamente en lugar de esperar a su temporizador: el plazo se envejece
 * en la base con SQL, que es como el resto del proyecto viaja en el tiempo, y así ninguna prueba
 * depende de esperar un minuto real.
 */
class ExpiracionDeMedidasIT extends EscenarioDeMedidas {

  @Autowired private ExpiracionDeMedidas barrido;

  private NavegadorDePrueba admin;

  @BeforeEach
  void prepararAdministradora() {
    admin = administradora(CORREO_ADMIN);
  }

  /** Un caso con una restricción temporal vigente sobre la cuenta reportada. */
  private long casoConRestriccionVigente() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(
            aplicarMedida(admin, idCaso, medidaDeRestriccion(), OffsetDateTime.now().plusDays(7))
                .statusCode())
        .isEqualTo(HttpStatus.OK.value());
    return idCaso;
  }

  @Test
  @DisplayName("Antes de la fecha no expira nada")
  void antesDeLaFechaNoExpiraNada() {
    long idCaso = casoConRestriccionVigente();
    int versionesAntes = versionesEnBase(idCaso);

    assertThat(barrido.expirarLasVencidas()).isZero();

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("RESTRINGIDA_TEMPORAL");
    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
    assertThat(versionesEnBase(idCaso)).isEqualTo(versionesAntes);
  }

  @Test
  @DisplayName("Alcanzada la fecha, la medida se levanta y la cuenta vuelve a ACTIVA")
  void alVencerElPlazoLaCuentaVuelveAEstarActiva() {
    long idCaso = casoConRestriccionVigente();
    vencerLaMedidaDe(idCaso);

    assertThat(barrido.expirarLasVencidas()).isEqualTo(1);

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
    assertThat(fechaFinDeCuentaEnBase(CORREO)).isNull();
    assertThat(medidasVigentesDe(CORREO)).isZero();
    assertThat(casoEnBase(idCaso).get("id_medida_administrativa_actual")).isNull();
    assertThat(casoEnBase(idCaso).get("fecha_fin_medida_actual")).isNull();
  }

  @Test
  @DisplayName("La expiración queda registrada a nombre del sistema, sin actor")
  void laExpiracionEsUnEventoDelSistema() {
    long idCaso = casoConRestriccionVigente();
    vencerLaMedidaDe(idCaso);

    assertThat(barrido.expirarLasVencidas()).isEqualTo(1);

    Map<String, Object> version = versionActual(idCaso);
    assertThat(version.get("tipo_evento")).isEqualTo("MEDIDA_EXPIRADA");
    assertThat(version.get("tipo_actor")).isEqualTo("SISTEMA");
    assertThat(version.get("id_actor")).isNull();
    assertThat(version.get("estado_cuenta")).isEqualTo("ACTIVA");
    assertThat(version.get("id_medida_administrativa")).isNull();
    // El responsable histórico se conserva: el evento no lo originó, pero sigue
    // respondiendo por el caso.
    assertThat(version.get("id_administrador_responsable")).isEqualTo(idDe(CORREO_ADMIN));

    assertThat(eventosEnOrden(idCaso)).endsWith("MEDIDA_EXPIRADA");
    comprobarCadenaScd2(idCaso);
  }

  @Test
  @DisplayName("Una suspensión expirada deja volver a iniciar sesión")
  void unaSuspensionExpiradaDevuelveElAcceso() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(
            aplicarMedida(admin, idCaso, medidaDeSuspension(), OffsetDateTime.now().plusDays(7))
                .statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(iniciarSesion(abrirNavegador(), CORREO, CLAVE).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());

    vencerLaMedidaDe(idCaso);
    assertThat(barrido.expirarLasVencidas()).isEqualTo(1);

    assertThat(iniciarSesion(abrirNavegador(), CORREO, CLAVE).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
  }

  @Test
  @DisplayName("Repetir el barrido no vuelve a expirar ni añade otra versión")
  void elBarridoEsIdempotente() {
    long idCaso = casoConRestriccionVigente();
    vencerLaMedidaDe(idCaso);

    assertThat(barrido.expirarLasVencidas()).isEqualTo(1);
    int versionesTrasExpirar = versionesEnBase(idCaso);

    assertThat(barrido.expirarLasVencidas()).isZero();
    assertThat(barrido.expirarLasVencidas()).isZero();

    assertThat(versionesEnBase(idCaso)).isEqualTo(versionesTrasExpirar);
    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
    comprobarCadenaScd2(idCaso);
  }

  @Test
  @DisplayName("Una medida ya revocada a mano no se expira después")
  void unaMedidaRevocadaNoSeExpiraDespues() {
    long idCaso = casoConRestriccionVigente();
    vencerLaMedidaDe(idCaso);

    assertThat(revocarMedida(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    int versionesTrasRevocar = versionesEnBase(idCaso);

    assertThat(barrido.expirarLasVencidas()).isZero();

    assertThat(versionesEnBase(idCaso)).isEqualTo(versionesTrasRevocar);
    assertThat(eventosEnOrden(idCaso))
        .endsWith("MEDIDA_REVOCADA")
        .doesNotContain("MEDIDA_EXPIRADA");
    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
  }

  @Test
  @DisplayName("Una medida sustituida por otra con plazo nuevo no arrastra el vencimiento anterior")
  void unReemplazoDejaSinEfectoElPlazoAnterior() {
    long primero = casoProcedenteDe(admin, CORREO_ADMIN);
    long segundo = otroCasoProcedenteDe(admin, CORREO_ADMIN);

    assertThat(
            aplicarMedida(admin, primero, medidaDeRestriccion(), OffsetDateTime.now().plusDays(7))
                .statusCode())
        .isEqualTo(HttpStatus.OK.value());
    vencerLaMedidaDe(primero);

    // Se sustituye por una suspensión con plazo futuro antes de que pase el barrido.
    assertThat(
            aplicarMedida(
                    admin, segundo, medidaDeSuspension(), OffsetDateTime.now().plusDays(30), true)
                .statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(barrido.expirarLasVencidas()).isZero();

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("SUSPENDIDA_TEMPORAL");
    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
    assertThat(eventosEnOrden(primero)).doesNotContain("MEDIDA_EXPIRADA");
    assertThat(eventosEnOrden(segundo)).endsWith("MEDIDA_APLICADA");
  }

  @Test
  @DisplayName("Una medida permanente no vence sola por mucho que pase el barrido")
  void unaMedidaPermanenteNoVenceSola() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(aplicarMedida(admin, idCaso, medidaPermanente(), null).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(barrido.expirarLasVencidas()).isZero();

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("SUSPENDIDA_PERMANENTE");
    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
  }

  @Test
  @DisplayName("El barrido levanta a la vez las medidas vencidas de personas distintas")
  void levantaVariasMedidasVencidasDeUnaPasada() {
    // Los dos expedientes se montan antes de sancionar a nadie: una cuenta ya
    // restringida no aceptaría la solicitud que hace falta para el segundo.
    long delPrestador = casoProcedenteDe(admin, CORREO_ADMIN);

    // El segundo va al revés: el prestador reporta a un cliente distinto, así
    // que el sancionado es otra cuenta y no la misma. Es lo que hace que el
    // barrido tenga de verdad dos medidas que levantar y no una.
    NavegadorDePrueba otroCliente = cuentaAutenticada(CORREO_TERCERO);
    long idSolicitud = idDeSolicitud(enviarSolicitud(otroCliente, idServicio));
    assertThat(aceptar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());
    long delCliente = idDeCaso(reportar(navegador, idSolicitud));
    assertThat(asignar(admin, delCliente, idDe(CORREO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(iniciarRevision(admin, delCliente).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(cerrar(admin, delCliente, "PROCEDENTE").statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(
            aplicarMedida(
                    admin, delPrestador, medidaDeRestriccion(), OffsetDateTime.now().plusDays(7))
                .statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(
            aplicarMedida(admin, delCliente, medidaDeSuspension(), OffsetDateTime.now().plusDays(7))
                .statusCode())
        .isEqualTo(HttpStatus.OK.value());

    vencerLaMedidaDe(delPrestador);
    vencerLaMedidaDe(delCliente);

    assertThat(barrido.expirarLasVencidas()).isEqualTo(2);

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
    assertThat(estadoDeCuentaEnBase(CORREO_TERCERO)).isEqualTo("ACTIVA");
    assertThat(eventosEnOrden(delPrestador)).endsWith("MEDIDA_EXPIRADA");
    assertThat(eventosEnOrden(delCliente)).endsWith("MEDIDA_EXPIRADA");
  }
}
