package com.moica.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * La gestión del catálogo de medidas administrativas, de extremo a extremo.
 *
 * <p>Cubre lo que fija el criterio de salida de P10B para el catálogo: se administra con rol y
 * segundo factor, sus datos son coherentes y <b>una medida no se elimina físicamente</b>: se
 * deshabilita, deja de ofrecerse para aplicaciones nuevas y sigue describiendo las decisiones que
 * la citaron.
 *
 * <p>Lo que la medida hace al aplicarse —estado de cuenta, sesiones, historial— se comprueba en
 * {@link MedidasDeCasoIT}. Aquí solo se administra el catálogo.
 */
class CatalogoDeMedidasIT extends EscenarioDeMedidas {

  private NavegadorDePrueba admin;

  @BeforeEach
  void prepararAdministradora() {
    admin = administradora(CORREO_ADMIN);
  }

  // --- Autorización -------------------------------------------------------

  @Test
  @DisplayName("Sin sesión no se llega a ninguna ruta del catálogo")
  void sinSesionNoSeLlegaAlCatalogo() {
    NavegadorDePrueba anonimo = abrirNavegador();

    assertThat(consultarCatalogo(anonimo).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(crearMedidaCon(anonimo, "X", "X", (short) 1, null, false).statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(medidasEnCatalogo()).isZero();
  }

  @Test
  @DisplayName("Una cuenta sin rol administrativo no lee ni escribe el catálogo")
  void unaCuentaCorrienteNoTocaElCatalogo() {
    assertThat(consultarCatalogo(cliente).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(crearMedidaCon(cliente, "X", "X", (short) 1, null, false).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(medidasEnCatalogo()).isZero();
  }

  @Test
  @DisplayName("Con rol administrativo pero sin segundo factor verificado tampoco se entra")
  void unAdministradorSinSegundoFactorVerificadoNoEntra() {
    // Es la misma cuenta administradora de siempre, con su segundo factor ya
    // activo. Lo que cambia es la sesión: esta acaba de abrirse y todavía no ha
    // presentado ningún código.
    NavegadorDePrueba otraSesion = abrirNavegador();
    assertThat(iniciarSesion(otraSesion, CORREO_ADMIN, CLAVE).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    assertThat(consultarCatalogo(otraSesion).statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(crearMedidaCon(otraSesion, "X", "X", (short) 1, null, false).statusCode())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  // --- Alta ---------------------------------------------------------------

  @Test
  @DisplayName("El catálogo empieza vacío y una medida creada aparece con todos sus datos")
  void creaUnaMedidaValida() {
    assertThat(json(consultarCatalogo(admin))).isEmpty();

    HttpResponse<String> creada =
        crearMedidaCon(admin, "ADVERTENCIA", "Advertencia", (short) 1, null, false);

    assertThat(creada.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(creada).get("codigo").asText()).isEqualTo("ADVERTENCIA");
    assertThat(json(creada).get("nombre").asText()).isEqualTo("Advertencia");
    assertThat(json(creada).get("nivelSeveridad").asInt()).isEqualTo(1);
    assertThat(json(creada).get("estadoCuentaResultante").isNull()).isTrue();
    assertThat(json(creada).get("requiereFechaFin").asBoolean()).isFalse();
    assertThat(json(creada).get("habilitada").asBoolean()).isTrue();

    assertThat(json(consultarCatalogo(admin))).hasSize(1);
  }

  @Test
  @DisplayName("El código se normaliza a mayúsculas y no admite otros caracteres")
  void normalizaYValidaElCodigo() {
    HttpResponse<String> creada =
        crearMedidaCon(admin, " advertencia ", "Advertencia", (short) 1, null, false);

    assertThat(creada.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(json(creada).get("codigo").asText()).isEqualTo("ADVERTENCIA");

    HttpResponse<String> conEspacios =
        crearMedidaCon(admin, "CON ESPACIO", "Otra", (short) 1, null, false);

    assertThat(conEspacios.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(conEspacios)).isEqualTo("VALIDACION");
    assertThat(medidasEnCatalogo()).isEqualTo(1);
  }

  @Test
  @DisplayName("Rechaza una medida sin código, sin nombre o con severidad fuera de rango")
  void rechazaLosDatosQueNoCuadran() {
    Map<String, Object> sinNada = new HashMap<>();
    sinNada.put("codigo", "  ");
    sinNada.put("nombre", "  ");
    sinNada.put("nivelSeveridad", 0);
    sinNada.put("requiereFechaFin", false);

    HttpResponse<String> respuesta = admin.post(RUTA_MEDIDAS, sinNada);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("VALIDACION");
    assertThat(medidasEnCatalogo()).isZero();
  }

  @Test
  @DisplayName("Una medida temporal tiene que exigir fecha de fin, y una permanente no puede")
  void exigeCoherenciaEntreElPlazoYElEstadoResultante() {
    HttpResponse<String> temporalSinPlazo =
        crearMedidaCon(admin, "MAL_UNO", "Mal uno", (short) 2, "SUSPENDIDA_TEMPORAL", false);

    assertThat(temporalSinPlazo.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(temporalSinPlazo)).isEqualTo("MEDIDA_INCOHERENTE");

    HttpResponse<String> permanenteConPlazo =
        crearMedidaCon(admin, "MAL_DOS", "Mal dos", (short) 4, "SUSPENDIDA_PERMANENTE", true);

    assertThat(permanenteConPlazo.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(permanenteConPlazo)).isEqualTo("MEDIDA_INCOHERENTE");

    HttpResponse<String> advertenciaConPlazo =
        crearMedidaCon(admin, "MAL_TRES", "Mal tres", (short) 1, null, true);

    assertThat(advertenciaConPlazo.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(advertenciaConPlazo)).isEqualTo("MEDIDA_INCOHERENTE");

    assertThat(medidasEnCatalogo()).isZero();
  }

  @Test
  @DisplayName("El código y el nombre no se repiten, ni cambiando mayúsculas")
  void rechazaCodigoONombreDuplicado() {
    assertThat(
            crearMedidaCon(admin, "ADVERTENCIA", "Advertencia", (short) 1, null, false)
                .statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    HttpResponse<String> mismoCodigo =
        crearMedidaCon(admin, "advertencia", "Otro nombre", (short) 1, null, false);
    assertThat(mismoCodigo.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(mismoCodigo)).isEqualTo("MEDIDA_DUPLICADA");

    HttpResponse<String> mismoNombre =
        crearMedidaCon(admin, "OTRO_CODIGO", "advertencia", (short) 1, null, false);
    assertThat(mismoNombre.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(mismoNombre)).isEqualTo("MEDIDA_DUPLICADA");

    assertThat(medidasEnCatalogo()).isEqualTo(1);
  }

  // --- Edición ------------------------------------------------------------

  @Test
  @DisplayName("Editar reescribe la medida y conserva su código")
  void editaLaMedidaSinTocarElCodigo() {
    short idMedida = medidaDeRestriccion();

    HttpResponse<String> editada =
        editarMedida(
            admin, idMedida, "Restricción de funciones", (short) 5, "RESTRINGIDA_TEMPORAL", true);

    assertThat(editada.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(editada).get("nombre").asText()).isEqualTo("Restricción de funciones");
    assertThat(json(editada).get("nivelSeveridad").asInt()).isEqualTo(5);
    assertThat(json(editada).get("codigo").asText()).isEqualTo("RESTRICCION_TEMPORAL");
  }

  @Test
  @DisplayName("Editar no puede robarle el nombre a otra medida ni romper la coherencia del plazo")
  void rechazaUnaEdicionInvalida() {
    short advertencia = medidaDeAdvertencia();
    short restriccion = medidaDeRestriccion();

    HttpResponse<String> nombreAjeno =
        editarMedida(admin, restriccion, "Advertencia", (short) 2, "RESTRINGIDA_TEMPORAL", true);
    assertThat(nombreAjeno.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(nombreAjeno)).isEqualTo("MEDIDA_DUPLICADA");

    HttpResponse<String> plazoIncoherente =
        editarMedida(admin, advertencia, "Advertencia", (short) 1, "SUSPENDIDA_TEMPORAL", false);
    assertThat(plazoIncoherente.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(plazoIncoherente)).isEqualTo("MEDIDA_INCOHERENTE");
  }

  @Test
  @DisplayName("Una medida inexistente responde 404 y no revela nada")
  void unaMedidaInexistenteResponde404() {
    HttpResponse<String> respuesta =
        editarMedida(admin, (short) 9999, "Cualquiera", (short) 1, null, false);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("MEDIDA_NO_ENCONTRADA");
  }

  // --- Deshabilitar en lugar de borrar ------------------------------------

  @Test
  @DisplayName("Deshabilitar y volver a habilitar cambian solo esa marca")
  void deshabilitaYVuelveAHabilitar() {
    short idMedida = medidaDeAdvertencia();

    HttpResponse<String> apagada = cambiarHabilitacion(admin, idMedida, false);
    assertThat(apagada.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(apagada).get("habilitada").asBoolean()).isFalse();
    assertThat(json(apagada).get("codigo").asText()).isEqualTo("ADVERTENCIA");

    // Sigue en el catálogo: deshabilitar no es borrar.
    assertThat(medidasEnCatalogo()).isEqualTo(1);
    assertThat(json(consultarCatalogo(admin))).hasSize(1);

    HttpResponse<String> encendida = cambiarHabilitacion(admin, idMedida, true);
    assertThat(encendida.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(encendida).get("habilitada").asBoolean()).isTrue();
  }

  @Test
  @DisplayName("La API no ofrece ninguna forma de eliminar una medida")
  void noExisteNingunBorrado() {
    short idMedida = medidaDeAdvertencia();

    HttpResponse<String> borrado = admin.delete(RUTA_MEDIDAS + "/" + idMedida);

    assertThat(borrado.statusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    assertThat(medidasEnCatalogo()).isEqualTo(1);
  }

  @Test
  @DisplayName("Una medida ya aplicada sobrevive a deshabilitarla y sigue describiendo su historia")
  void unaMedidaReferenciadaNoSeBorraYSigueDescribiendoElHistorial() {
    NavegadorDePrueba responsable = administradora(CORREO_OTRO_ADMIN);
    long idCaso = casoProcedenteDe(responsable, CORREO_OTRO_ADMIN);
    short idMedida = medidaDeAdvertencia();

    assertThat(aplicarMedida(responsable, idCaso, idMedida, null).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    assertThat(cambiarHabilitacion(admin, idMedida, false).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    // La fila sigue ahí y la medida sigue vigente sobre la cuenta.
    assertThat(medidasEnCatalogo()).isEqualTo(1);
    assertThat(medidasVigentesDe(CORREO)).isEqualTo(1);

    // Y el historial la sigue nombrando, aunque ya no se ofrezca.
    HttpResponse<String> expediente = consultarExpediente(responsable, idCaso);
    var versiones = json(expediente).get("historial");
    var ultima = versiones.get(versiones.size() - 1);

    assertThat(ultima.get("tipoEvento").asText()).isEqualTo("MEDIDA_APLICADA");
    assertThat(ultima.get("nombreMedida").asText()).isEqualTo("Advertencia");
  }
}
