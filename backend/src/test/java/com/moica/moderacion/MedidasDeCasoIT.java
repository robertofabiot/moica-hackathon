package com.moica.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * La aplicación, la revocación y la sustitución de medidas, de extremo a extremo.
 *
 * <p>Cubre el núcleo del criterio de salida de P10B: la medida la elige una persona, cada cuenta
 * sostiene como máximo una, sustituirla exige confirmación explícita y ocurre dentro de una sola
 * operación, el estado de cuenta refleja la medida, las sesiones se revocan cuando el acceso se
 * cierra y revocar la única medida devuelve la cuenta a {@code ACTIVA}.
 *
 * <p>Quien acaba sancionado es el prestador del escenario ({@link #CORREO}), porque el caso lo abre
 * el cliente contra él.
 */
class MedidasDeCasoIT extends EscenarioDeMedidas {

  private NavegadorDePrueba admin;

  @BeforeEach
  void prepararAdministradora() {
    admin = administradora(CORREO_ADMIN);
  }

  private OffsetDateTime dentroDeUnaSemana() {
    return OffsetDateTime.now().plusDays(7);
  }

  // --- Autorización -------------------------------------------------------

  @Test
  @DisplayName("Sin sesión, sin rol o sin segundo factor no se aplica ninguna medida")
  void soloUnAdministradorConSegundoFactorAplicaMedidas() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    short idMedida = medidaDeAdvertencia();

    assertThat(aplicarMedida(abrirNavegador(), idCaso, idMedida, null).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(aplicarMedida(cliente, idCaso, idMedida, null).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());

    NavegadorDePrueba sinVerificar = abrirNavegador();
    assertThat(iniciarSesion(sinVerificar, CORREO_ADMIN, CLAVE).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    assertThat(aplicarMedida(sinVerificar, idCaso, idMedida, null).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
    assertThat(medidasVigentesDe(CORREO)).isZero();
  }

  @Test
  @DisplayName("Solo quien tiene el caso asignado decide sobre sus medidas")
  void soloElResponsableAplicaYRevoca() {
    NavegadorDePrueba otra = administradora(CORREO_OTRO_ADMIN);
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    short idMedida = medidaDeAdvertencia();

    HttpResponse<String> ajena = aplicarMedida(otra, idCaso, idMedida, null);

    assertThat(ajena.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(ajena)).isEqualTo("CASO_DE_OTRO_ADMINISTRADOR");
    assertThat(medidasVigentesDe(CORREO)).isZero();
  }

  @Test
  @DisplayName("Un caso inexistente responde 404 y no revela nada")
  void unCasoInexistenteResponde404() {
    short idMedida = medidaDeAdvertencia();

    HttpResponse<String> respuesta = aplicarMedida(admin, 999_999L, idMedida, null);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CASO_NO_ENCONTRADO");
  }

  // --- Desde dónde se puede aplicar ---------------------------------------

  @Test
  @DisplayName("Solo se aplica una medida desde un caso cerrado como procedente")
  void soloSeAplicaDesdeUnCasoCerradoYProcedente() {
    short idMedida = medidaDeAdvertencia();

    long abierto = casoAbierto();
    assertThat(asignar(admin, abierto, idDe(CORREO_ADMIN)).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    HttpResponse<String> sinRevisar = aplicarMedida(admin, abierto, idMedida, null);
    assertThat(sinRevisar.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(sinRevisar)).isEqualTo("MEDIDA_NO_APLICABLE");

    long enRevision = casoEnRevisionDe(admin, CORREO_ADMIN);
    HttpResponse<String> sinCerrar = aplicarMedida(admin, enRevision, idMedida, null);
    assertThat(sinCerrar.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(sinCerrar)).isEqualTo("MEDIDA_NO_APLICABLE");

    long desestimado = casoEnRevisionDe(admin, CORREO_ADMIN);
    assertThat(cerrar(admin, desestimado, "DESESTIMADO").statusCode())
        .isEqualTo(HttpStatus.OK.value());
    HttpResponse<String> traDesestimar = aplicarMedida(admin, desestimado, idMedida, null);
    assertThat(traDesestimar.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(traDesestimar)).isEqualTo("MEDIDA_NO_APLICABLE");

    assertThat(medidasVigentesDe(CORREO)).isZero();
    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
  }

  @Test
  @DisplayName("Una medida deshabilitada entre la carga y el envío ya no se puede aplicar")
  void noSeAplicaUnaMedidaDeshabilitada() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    short idMedida = medidaDeAdvertencia();

    assertThat(cambiarHabilitacion(admin, idMedida, false).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> respuesta = aplicarMedida(admin, idCaso, idMedida, null);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("MEDIDA_DESHABILITADA");
    assertThat(medidasVigentesDe(CORREO)).isZero();
  }

  @Test
  @DisplayName("El plazo tiene que coincidir con lo que la medida exige y estar en el futuro")
  void exigeUnPlazoCoherenteYFuturo() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    short advertencia = medidaDeAdvertencia();
    short restriccion = medidaDeRestriccion();

    HttpResponse<String> plazoDeMas =
        aplicarMedida(admin, idCaso, advertencia, dentroDeUnaSemana());
    assertThat(plazoDeMas.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(plazoDeMas)).isEqualTo("FECHA_FIN_NO_ADMITIDA");

    HttpResponse<String> sinPlazo = aplicarMedida(admin, idCaso, restriccion, null);
    assertThat(sinPlazo.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(sinPlazo)).isEqualTo("FECHA_FIN_REQUERIDA");

    HttpResponse<String> plazoPasado =
        aplicarMedida(admin, idCaso, restriccion, OffsetDateTime.now().minusDays(1));
    assertThat(plazoPasado.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(plazoPasado)).isEqualTo("FECHA_FIN_INVALIDA");

    assertThat(medidasVigentesDe(CORREO)).isZero();
  }

  // --- Aplicar ------------------------------------------------------------

  @Test
  @DisplayName("Aplicar una restricción proyecta el estado de cuenta con su fecha de fin")
  void aplicarProyectaElEstadoDeCuenta() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    short idMedida = medidaDeRestriccion();
    OffsetDateTime fin = dentroDeUnaSemana();

    HttpResponse<String> respuesta = aplicarMedida(admin, idCaso, idMedida, fin);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get("estadoCuentaReportada").asText())
        .isEqualTo("RESTRINGIDA_TEMPORAL");
    assertThat(json(respuesta).get("medidaVigente").get("esDeEsteCaso").asBoolean()).isTrue();
    assertThat(json(respuesta).get("medidaVigente").get("codigo").asText())
        .isEqualTo("RESTRICCION_TEMPORAL");

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("RESTRINGIDA_TEMPORAL");
    assertThat(fechaFinDeCuentaEnBase(CORREO)).isNotNull();
    assertThat(casoEnBase(idCaso).get("id_medida_administrativa_actual")).isEqualTo((int) idMedida);
    assertThat(casoEnBase(idCaso).get("fecha_fin_medida_actual")).isNotNull();
  }

  @Test
  @DisplayName("Una restricción no expulsa a la persona: su sesión sigue viva")
  void unaRestriccionConservaLaSesion() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);

    assertThat(
            aplicarMedida(admin, idCaso, medidaDeRestriccion(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(sesionesVigentesDe(CORREO)).isPositive();
    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.OK.value());
  }

  @Test
  @DisplayName("Una suspensión revoca las sesiones y la siguiente petición ya no tiene acceso")
  void unaSuspensionRevocaLasSesionesAbiertas() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);

    // Antes de sancionar, la sesión del prestador funciona.
    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(sesionesVigentesDe(CORREO)).isPositive();

    assertThat(aplicarMedida(admin, idCaso, medidaDeSuspension(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("SUSPENDIDA_TEMPORAL");
    assertThat(sesionesVigentesDe(CORREO)).isZero();
    assertThat(motivoDeRevocacionDeLasSesiones(CORREO)).isEqualTo("MEDIDA_ADMINISTRATIVA");

    // El JWT del navegador sigue sin expirar y aun así ya no sirve.
    assertThat(navegador.get(RUTA_SESION).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  @DisplayName("Una cuenta suspendida no puede volver a entrar y lee el canal de soporte")
  void unaCuentaSuspendidaNoEntraYLeeElCanalDeSoporte() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(aplicarMedida(admin, idCaso, medidaDeSuspension(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> reintento = iniciarSesion(abrirNavegador(), CORREO, CLAVE);

    assertThat(reintento.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(reintento)).isEqualTo("CUENTA_SUSPENDIDA");
    // Es el único texto que esa persona llega a leer: tiene que llevar el canal.
    assertThat(json(reintento).get("mensaje").asText()).contains("soporte@moica.ni");
  }

  @Test
  @DisplayName("Una advertencia queda registrada sin tocar el acceso de la cuenta")
  void unaAdvertenciaNoCambiaElAcceso() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);

    assertThat(aplicarMedida(admin, idCaso, medidaDeAdvertencia(), null).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
    assertThat(fechaFinDeCuentaEnBase(CORREO)).isNull();
    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
    assertThat(sesionesVigentesDe(CORREO)).isPositive();
  }

  @Test
  @DisplayName("Aplicar deja una versión MEDIDA_APLICADA con la medida y el estado resultante")
  void aplicarVersionaElHistorial() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    short idMedida = medidaDeRestriccion();

    assertThat(aplicarMedida(admin, idCaso, idMedida, dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    Map<String, Object> version = versionActual(idCaso);
    assertThat(version.get("tipo_evento")).isEqualTo("MEDIDA_APLICADA");
    assertThat(version.get("tipo_actor")).isEqualTo("ADMINISTRADOR");
    assertThat(version.get("id_actor")).isEqualTo(idDe(CORREO_ADMIN));
    assertThat(version.get("id_medida_administrativa")).isEqualTo((int) idMedida);
    assertThat(version.get("estado_cuenta")).isEqualTo("RESTRINGIDA_TEMPORAL");
    assertThat(version.get("fecha_fin_medida")).isNotNull();
    assertThat((String) version.get("detalle_cambio")).contains(JUSTIFICACION);

    assertThat(eventosEnOrden(idCaso))
        .containsExactly(
            "CASO_ABIERTO",
            "RESPONSABLE_ASIGNADO",
            "ESTADO_CASO_CAMBIADO",
            "RESOLUCION_REGISTRADA",
            "MEDIDA_APLICADA");
    comprobarCadenaScd2(idCaso);
  }

  // --- Una sola medida vigente --------------------------------------------

  @Test
  @DisplayName("Una segunda medida sin confirmar responde 409 y no cambia absolutamente nada")
  void unaSegundaMedidaSinConfirmarNoSustituyeNada() {
    long primero = casoProcedenteDe(admin, CORREO_ADMIN);
    long segundo = otroCasoProcedenteDe(admin, CORREO_ADMIN);

    short restriccion = medidaDeRestriccion();
    assertThat(aplicarMedida(admin, primero, restriccion, dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    int versionesAntes = versionesEnBase(segundo);
    short suspension = medidaDeSuspension();

    HttpResponse<String> sinConfirmar =
        aplicarMedida(admin, segundo, suspension, dentroDeUnaSemana());

    assertThat(sinConfirmar.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(sinConfirmar)).isEqualTo("MEDIDA_VIGENTE_EXISTENTE");
    // El mensaje dice cuál está vigente, que es lo que la interfaz necesita.
    assertThat(json(sinConfirmar).get("mensaje").asText()).contains(String.valueOf(primero));

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("RESTRINGIDA_TEMPORAL");
    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
    assertThat(versionesEnBase(segundo)).isEqualTo(versionesAntes);
  }

  @Test
  @DisplayName("Confirmado el reemplazo, la anterior se revoca y la nueva queda vigente")
  void confirmarElReemplazoSustituyeLaMedidaEnUnaSolaOperacion() {
    long primero = casoProcedenteDe(admin, CORREO_ADMIN);
    long segundo = otroCasoProcedenteDe(admin, CORREO_ADMIN);

    assertThat(
            aplicarMedida(admin, primero, medidaDeRestriccion(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    short suspension = medidaDeSuspension();

    HttpResponse<String> confirmado =
        aplicarMedida(admin, segundo, suspension, dentroDeUnaSemana(), true);

    assertThat(confirmado.statusCode()).isEqualTo(HttpStatus.OK.value());

    // Una sola vigente, y es la del segundo caso.
    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
    assertThat(casoEnBase(primero).get("id_medida_administrativa_actual")).isNull();
    assertThat(casoEnBase(primero).get("fecha_fin_medida_actual")).isNull();
    assertThat(casoEnBase(segundo).get("id_medida_administrativa_actual"))
        .isEqualTo((int) suspension);
    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("SUSPENDIDA_TEMPORAL");
    assertThat(sesionesVigentesDe(CORREO)).isZero();

    // Cada expediente registra su parte, y el que pierde la medida fotografía
    // el estado ya sustituido: durante ese periodo la cuenta no estuvo activa.
    assertThat(eventosEnOrden(primero)).endsWith("MEDIDA_REVOCADA");
    assertThat(versionActual(primero).get("estado_cuenta")).isEqualTo("SUSPENDIDA_TEMPORAL");
    assertThat(versionActual(primero).get("id_medida_administrativa")).isNull();
    assertThat(eventosEnOrden(segundo)).endsWith("MEDIDA_APLICADA");

    comprobarCadenaScd2(primero);
    comprobarCadenaScd2(segundo);
  }

  @Test
  @DisplayName(
      "Sustituir la medida del mismo caso deja una sola versión, no dos en el mismo instante")
  void sustituirLaMedidaDelMismoCasoDejaUnaSolaVersion() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(
            aplicarMedida(admin, idCaso, medidaDeRestriccion(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    int versionesAntes = versionesEnBase(idCaso);
    short permanente = medidaPermanente();

    HttpResponse<String> sustituida = aplicarMedida(admin, idCaso, permanente, null, true);

    assertThat(sustituida.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(versionesEnBase(idCaso)).isEqualTo(versionesAntes + 1);
    assertThat(casoEnBase(idCaso).get("id_medida_administrativa_actual"))
        .isEqualTo((int) permanente);
    assertThat(casoEnBase(idCaso).get("fecha_fin_medida_actual")).isNull();
    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("SUSPENDIDA_PERMANENTE");
    assertThat(fechaFinDeCuentaEnBase(CORREO)).isNull();
    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);
    comprobarCadenaScd2(idCaso);
  }

  @Test
  @DisplayName("El expediente avisa de una medida vigente aunque la sostenga otro caso")
  void elExpedienteAvisaDeLaMedidaDeOtroCaso() {
    long primero = casoProcedenteDe(admin, CORREO_ADMIN);
    long segundo = otroCasoProcedenteDe(admin, CORREO_ADMIN);

    assertThat(
            aplicarMedida(admin, primero, medidaDeRestriccion(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> expediente = consultarExpediente(admin, segundo);

    assertThat(json(expediente).get("medidaVigente").get("idCasoModeracion").asLong())
        .isEqualTo(primero);
    assertThat(json(expediente).get("medidaVigente").get("esDeEsteCaso").asBoolean()).isFalse();
    assertThat(json(expediente).get("estadoCuentaReportada").asText())
        .isEqualTo("RESTRINGIDA_TEMPORAL");
  }

  // --- Revocar ------------------------------------------------------------

  @Test
  @DisplayName("Revocar la única medida devuelve la cuenta a ACTIVA y lo deja en el historial")
  void revocarDevuelveLaCuentaAActiva() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(
            aplicarMedida(admin, idCaso, medidaDeRestriccion(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> revocada = revocarMedida(admin, idCaso);

    assertThat(revocada.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(revocada).get("medidaVigente").isNull()).isTrue();
    assertThat(json(revocada).get("estadoCuentaReportada").asText()).isEqualTo("ACTIVA");

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
    assertThat(fechaFinDeCuentaEnBase(CORREO)).isNull();
    assertThat(medidasVigentesDe(CORREO)).isZero();

    Map<String, Object> version = versionActual(idCaso);
    assertThat(version.get("tipo_evento")).isEqualTo("MEDIDA_REVOCADA");
    assertThat(version.get("estado_cuenta")).isEqualTo("ACTIVA");
    assertThat(version.get("id_medida_administrativa")).isNull();
    assertThat((String) version.get("detalle_cambio")).contains(MOTIVO_DE_REVOCACION);
    comprobarCadenaScd2(idCaso);
  }

  @Test
  @DisplayName("Revocar dos veces seguidas responde 409 y no vuelve a versionar")
  void revocarDosVecesEsUnConflictoControlado() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(aplicarMedida(admin, idCaso, medidaDeAdvertencia(), null).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(revocarMedida(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());

    int versionesAntes = versionesEnBase(idCaso);
    HttpResponse<String> repetida = revocarMedida(admin, idCaso);

    assertThat(repetida.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(repetida)).isEqualTo("SIN_MEDIDA_VIGENTE");
    assertThat(versionesEnBase(idCaso)).isEqualTo(versionesAntes);
  }

  @Test
  @DisplayName("Revocar una suspensión reactiva la cuenta y deja volver a entrar")
  void revocarUnaSuspensionDejaVolverAEntrar() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(aplicarMedida(admin, idCaso, medidaPermanente(), null).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(iniciarSesion(abrirNavegador(), CORREO, CLAVE).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());

    assertThat(revocarMedida(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(iniciarSesion(abrirNavegador(), CORREO, CLAVE).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
  }

  // --- Lo que P10A prometía y sigue siendo cierto -------------------------

  @Test
  @DisplayName("Cerrar como procedente sigue sin sancionar por su cuenta")
  void cerrarComoProcedenteNoSanciona() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);

    assertThat(estadoDeCuentaEnBase(CORREO)).isEqualTo("ACTIVA");
    assertThat(medidasVigentesDe(CORREO)).isZero();
    assertThat(sesionesVigentesDe(CORREO)).isPositive();

    HttpResponse<String> expediente = consultarExpediente(admin, idCaso);
    assertThat(json(expediente).get("medidaVigente").isNull()).isTrue();
    assertThat(json(expediente).get("apelacion").asText()).isEqualTo("SIN_APELACION");
  }

  @Test
  @DisplayName("El expediente no filtra datos internos de la persona reportada")
  void elExpedienteNoFiltraDatosInternos() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(aplicarMedida(admin, idCaso, medidaDeAdvertencia(), null).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    String cuerpo = consultarExpediente(admin, idCaso).body();

    assertThat(cuerpo)
        .doesNotContain("claveHash")
        .doesNotContain("clave_hash")
        .doesNotContain("secreto")
        .doesNotContain("identificadorToken")
        .doesNotContain(CLAVE);
  }

  @Test
  @DisplayName("Aplicar y revocar dejan una sola versión vigente por evento, sin solapes")
  void elRecorridoCompletoMantieneElScd2() {
    long idCaso = casoProcedenteDe(admin, CORREO_ADMIN);
    assertThat(
            aplicarMedida(admin, idCaso, medidaDeRestriccion(), dentroDeUnaSemana()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(revocarMedida(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());

    List<String> eventos = eventosEnOrden(idCaso);

    assertThat(eventos)
        .containsExactly(
            "CASO_ABIERTO",
            "RESPONSABLE_ASIGNADO",
            "ESTADO_CASO_CAMBIADO",
            "RESOLUCION_REGISTRADA",
            "MEDIDA_APLICADA",
            "MEDIDA_REVOCADA");
    comprobarCadenaScd2(idCaso);
  }
}
