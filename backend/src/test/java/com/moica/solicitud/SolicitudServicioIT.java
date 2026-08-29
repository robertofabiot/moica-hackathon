package com.moica.solicitud;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * El ciclo de una solicitud de servicio, contra PostgreSQL real.
 *
 * <p>Cubre el envío válido, los rechazos de elegibilidad, la lectura de participantes, el 404 de un
 * tercero, todas las transiciones y el trato de cuentas restringidas o suspendidas.
 */
class SolicitudServicioIT extends EscenarioDeSolicitud {

  private NavegadorDePrueba cliente;
  private long idServicio;

  @BeforeEach
  void prepararSolicitud() {
    idServicio = publicarServicioActivo();
    cliente = clienteAutenticado();
  }

  @Test
  void enviaUnaSolicitudValidaYRegistraElCambioInicial() {
    HttpResponse<String> respuesta = enviarSolicitud(cliente, idServicio);
    JsonNode cuerpo = json(respuesta);
    long id = cuerpo.get("idSolicitudServicio").asLong();

    assertThat(cuerpo.get("estadoActual").asText()).isEqualTo("PENDIENTE");
    assertThat(cuerpo.get("idServicioPublicado").asLong()).isEqualTo(idServicio);
    assertThat(cuerpo.get("descripcionNecesidad").asText()).contains("lavamanos");
    assertThat(cuerpo.get("indicacionUbicacion").asText()).contains("UCA");
    assertThat(cuerpo.get("fechaPreferida").asText()).isEqualTo("2026-09-15");
    assertThat(cuerpo.get("historial")).hasSize(1);
    assertThat(cuerpo.get("historial").get(0).get("estadoAnterior").isNull()).isTrue();
    assertThat(cuerpo.get("historial").get(0).get("estadoNuevo").asText()).isEqualTo("PENDIENTE");
    assertThat(cuerpo.toString()).doesNotContain("correoElectronico");
    assertThat(cuerpo.toString()).doesNotContain("@moica.test");

    assertThat(estadoActualEnBase(id)).isEqualTo("PENDIENTE");
    assertThat(ultimoEstadoDelHistorial(id)).isEqualTo("PENDIENTE");
    assertThat(cambiosRegistrados(id)).isEqualTo(1);
  }

  @Test
  void rechazaSolicitarElServicioPropio() {
    HttpResponse<String> respuesta =
        navegador.post(RUTA_SOLICITUDES, pedidoDeSolicitud(idServicio));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SERVICIO_PROPIO");
  }

  @Test
  void rechazaUnServicioInactivo() {
    assertThat(desactivar(idServicio).statusCode()).isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> respuesta = cliente.post(RUTA_SOLICITUDES, pedidoDeSolicitud(idServicio));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("SERVICIO_INACTIVO");
  }

  @Test
  void rechazaUnPrestadorNoDisponible() {
    dejarDisponible("NO_DISPONIBLE");

    HttpResponse<String> respuesta = cliente.post(RUTA_SOLICITUDES, pedidoDeSolicitud(idServicio));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("PRESTADOR_NO_DISPONIBLE");
  }

  @Test
  void rechazaUnPerfilSinVerificacionBasica() {
    NavegadorDePrueba sinBasica = cuentaAutenticada("sin.basica@moica.test");
    assertThat(sinBasica.post(RUTA_PERFIL, solicitudDePerfil()).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());
    Long idServicioAjeno =
        jdbc.queryForObject(
            """
            INSERT INTO servicio_publicado
                (id_prestador, id_subcategoria_servicio, nombre, descripcion, estado)
            VALUES (?, ?, 'Sin básica', 'Descripción', 'ACTIVO')
            RETURNING id_servicio_publicado
            """,
            Long.class,
            idDe("sin.basica@moica.test"),
            idSubcategoria("Plomería"));
    assertThat(idServicioAjeno).isNotNull();

    HttpResponse<String> respuesta =
        cliente.post(RUTA_SOLICITUDES, pedidoDeSolicitud(idServicioAjeno));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("VERIFICACION_BASICA_REQUERIDA");
  }

  @Test
  void rechazaUnaCuentaSolicitanteQueNoEstaActiva() {
    restringirCuenta(CORREO_CLIENTE);

    HttpResponse<String> respuesta = cliente.post(RUTA_SOLICITUDES, pedidoDeSolicitud(idServicio));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CUENTA_RESTRINGIDA");
  }

  @Test
  void rechazaUnMunicipioQueNoEstaDisponible() {
    Map<String, Object> pedido = pedidoDeSolicitud(idServicio);
    pedido.put("idMunicipio", municipioDeDepartamentoNoHabilitado());

    HttpResponse<String> respuesta = cliente.post(RUTA_SOLICITUDES, pedido);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("MUNICIPIO_NO_DISPONIBLE");
  }

  @Test
  void losDosParticipantesLeenYUnTerceroRecibe404() {
    long id = idDeSolicitud(enviarSolicitud(cliente, idServicio));

    HttpResponse<String> delCliente = cliente.get(RUTA_SOLICITUDES + "/" + id);
    HttpResponse<String> delPrestador = navegador.get(RUTA_SOLICITUDES + "/" + id);
    assertThat(delCliente.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(delPrestador.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(delCliente).get("indicacionUbicacion").asText()).contains("portón verde");

    NavegadorDePrueba tercero = cuentaAutenticada(CORREO_TERCERO);
    HttpResponse<String> ajena = tercero.get(RUTA_SOLICITUDES + "/" + id);
    assertThat(ajena.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(ajena)).isEqualTo("RECURSO_NO_ENCONTRADO");
  }

  @Test
  void lasBandejasSepararanEnviadasYRecibidas() {
    long id = idDeSolicitud(enviarSolicitud(cliente, idServicio));

    JsonNode enviadas = json(cliente.get(RUTA_SOLICITUDES + "/enviadas"));
    JsonNode recibidas = json(navegador.get(RUTA_SOLICITUDES + "/recibidas"));
    JsonNode enviadasDelPrestador = json(navegador.get(RUTA_SOLICITUDES + "/enviadas"));
    JsonNode recibidasDelCliente = json(cliente.get(RUTA_SOLICITUDES + "/recibidas"));

    assertThat(enviadas).hasSize(1);
    assertThat(enviadas.get(0).get("idSolicitudServicio").asLong()).isEqualTo(id);
    assertThat(recibidas).hasSize(1);
    assertThat(recibidas.get(0).get("idSolicitudServicio").asLong()).isEqualTo(id);
    assertThat(enviadasDelPrestador).isEmpty();
    assertThat(recibidasDelCliente).isEmpty();
  }

  @Test
  void elPrestadorAceptaYRechazaUnaPendiente() {
    long aceptada = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(aceptar(navegador, aceptada).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(estadoActualEnBase(aceptada)).isEqualTo("ACEPTADA");
    assertThat(ultimoEstadoDelHistorial(aceptada)).isEqualTo("ACEPTADA");
    assertThat(cambiosRegistrados(aceptada)).isEqualTo(2);

    long rechazada = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(rechazar(navegador, rechazada).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(estadoActualEnBase(rechazada)).isEqualTo("RECHAZADA");
    assertThat(ultimoEstadoDelHistorial(rechazada)).isEqualTo("RECHAZADA");
  }

  @Test
  void elClienteCancelaUnaPendienteSinMotivo() {
    long id = idDeSolicitud(enviarSolicitud(cliente, idServicio));

    assertThat(cancelar(cliente, id).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(estadoActualEnBase(id)).isEqualTo("CANCELADA");
    assertThat(ultimoEstadoDelHistorial(id)).isEqualTo("CANCELADA");
  }

  @Test
  void cualquieraDeLosDosCancelaUnaAceptadaConMotivoYElPrestadorLaCompleta() {
    long paraCancelar = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(aceptar(navegador, paraCancelar).statusCode()).isEqualTo(HttpStatus.OK.value());
    HttpResponse<String> cancelacion =
        cancelarConMotivo(cliente, paraCancelar, "Ya no necesito la visita.");
    assertThat(cancelacion.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(cancelacion).get("historial").get(2).get("motivo").asText())
        .isEqualTo("Ya no necesito la visita.");
    assertThat(estadoActualEnBase(paraCancelar)).isEqualTo("CANCELADA");

    long paraCompletar = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(aceptar(navegador, paraCompletar).statusCode()).isEqualTo(HttpStatus.OK.value());
    HttpResponse<String> delPrestador =
        cancelarConMotivo(navegador, paraCompletar, "No podré asistir esa fecha.");
    assertThat(delPrestador.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(estadoActualEnBase(paraCompletar)).isEqualTo("CANCELADA");

    long completada = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(aceptar(navegador, completada).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(completar(navegador, completada).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(estadoActualEnBase(completada)).isEqualTo("COMPLETADA");
    assertThat(ultimoEstadoDelHistorial(completada)).isEqualTo("COMPLETADA");
    assertThat(cambiosRegistrados(completada)).isEqualTo(3);
  }

  @Test
  void elActorIncorrectoNoCambiaElEstado() {
    long id = idDeSolicitud(enviarSolicitud(cliente, idServicio));

    assertThat(aceptar(cliente, id).statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(aceptar(cliente, id))).isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(codigoDeError(rechazar(cliente, id))).isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(codigoDeError(cancelar(navegador, id))).isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(estadoActualEnBase(id)).isEqualTo("PENDIENTE");
  }

  @Test
  void unEstadoIncorrectoYUnMotivoAusenteSeRechazan() {
    long id = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(aceptar(navegador, id).statusCode()).isEqualTo(HttpStatus.OK.value());

    assertThat(codigoDeError(aceptar(navegador, id))).isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(codigoDeError(rechazar(navegador, id))).isEqualTo("TRANSICION_NO_PERMITIDA");

    HttpResponse<String> sinMotivo = cancelar(cliente, id);
    assertThat(sinMotivo.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(sinMotivo)).isEqualTo("MOTIVO_OBLIGATORIO");

    HttpResponse<String> enBlanco = cancelarConMotivo(cliente, id, "   ");
    assertThat(enBlanco.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(enBlanco)).isEqualTo("MOTIVO_OBLIGATORIO");
    assertThat(estadoActualEnBase(id)).isEqualTo("ACEPTADA");
  }

  @Test
  void losEstadosDefinitivosNoSeReabren() {
    long rechazada = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(rechazar(navegador, rechazada).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(codigoDeError(aceptar(navegador, rechazada))).isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(codigoDeError(cancelar(cliente, rechazada))).isEqualTo("TRANSICION_NO_PERMITIDA");
    assertThat(codigoDeError(completar(navegador, rechazada))).isEqualTo("TRANSICION_NO_PERMITIDA");

    long cancelada = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(cancelar(cliente, cancelada).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(codigoDeError(aceptar(navegador, cancelada))).isEqualTo("TRANSICION_NO_PERMITIDA");

    long completada = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(aceptar(navegador, completada).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(completar(navegador, completada).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(codigoDeError(cancelarConMotivo(cliente, completada, "Tarde")))
        .isEqualTo("TRANSICION_NO_PERMITIDA");
  }

  @Test
  void unaCuentaRestringidaConsultaYCancelaPeroNoCreaNiAceptaNiRechazaNiCompleta() {
    long pendiente = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    long aceptada = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    assertThat(aceptar(navegador, aceptada).statusCode()).isEqualTo(HttpStatus.OK.value());

    restringirCuenta(CORREO_CLIENTE);
    assertThat(cliente.get(RUTA_SOLICITUDES + "/enviadas").statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(cliente.get(RUTA_SOLICITUDES + "/" + pendiente).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(codigoDeError(cliente.post(RUTA_SOLICITUDES, pedidoDeSolicitud(idServicio))))
        .isEqualTo("CUENTA_RESTRINGIDA");
    assertThat(cancelar(cliente, pendiente).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(cancelarConMotivo(cliente, aceptada, "La cuenta quedó restringida.").statusCode())
        .isEqualTo(HttpStatus.OK.value());

    long paraAceptar =
        idDeSolicitud(
            enviarSolicitud(clienteAutenticadoCon("otro.cliente@moica.test"), idServicio));
    restringirCuenta(CORREO);
    assertThat(navegador.get(RUTA_SOLICITUDES + "/recibidas").statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(codigoDeError(aceptar(navegador, paraAceptar))).isEqualTo("CUENTA_RESTRINGIDA");
    assertThat(codigoDeError(rechazar(navegador, paraAceptar))).isEqualTo("CUENTA_RESTRINGIDA");
    assertThat(estadoActualEnBase(paraAceptar)).isEqualTo("PENDIENTE");
    assertThat(cambiosRegistrados(paraAceptar)).isEqualTo(1);

    jdbc.update("UPDATE usuario SET estado_cuenta = 'ACTIVA' WHERE correo_electronico = ?", CORREO);
    assertThat(aceptar(navegador, paraAceptar).statusCode()).isEqualTo(HttpStatus.OK.value());
    restringirCuenta(CORREO);
    assertThat(codigoDeError(completar(navegador, paraAceptar))).isEqualTo("CUENTA_RESTRINGIDA");
  }

  @Test
  void unPrestadorRestringidoNoRechazaUnaPendiente() {
    long pendiente = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    int historialInicial = cambiosRegistrados(pendiente);

    restringirCuenta(CORREO);
    HttpResponse<String> respuesta = rechazar(navegador, pendiente);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("CUENTA_RESTRINGIDA");
    assertThat(estadoActualEnBase(pendiente)).isEqualTo("PENDIENTE");
    assertThat(ultimoEstadoDelHistorial(pendiente)).isEqualTo("PENDIENTE");
    assertThat(cambiosRegistrados(pendiente)).isEqualTo(historialInicial);
  }

  @Test
  void unaCuentaSuspendidaNoEjecutaAccionesAutenticadasDeNegocio() {
    long id = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    suspenderCuenta(CORREO_CLIENTE);

    HttpResponse<String> consulta = cliente.get(RUTA_SOLICITUDES + "/" + id);
    assertThat(consulta.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(codigoDeError(consulta)).isEqualTo("ACCESO_DENEGADO");
    assertThat(codigoDeError(cliente.post(RUTA_SOLICITUDES, pedidoDeSolicitud(idServicio))))
        .isEqualTo("ACCESO_DENEGADO");
  }

  @Test
  void elCuerpoNoFiltraCorreosContactosNiSecretos() {
    long id = idDeSolicitud(enviarSolicitud(cliente, idServicio));
    String detalle = cliente.get(RUTA_SOLICITUDES + "/" + id).body();
    String enviadas = cliente.get(RUTA_SOLICITUDES + "/enviadas").body();
    String recibidas = navegador.get(RUTA_SOLICITUDES + "/recibidas").body();

    for (String cuerpo : new String[] {detalle, enviadas, recibidas}) {
      assertThat(cuerpo).doesNotContain("correoElectronico");
      assertThat(cuerpo).doesNotContain("@moica.test");
      assertThat(cuerpo).doesNotContain("claveHash");
      assertThat(cuerpo).doesNotContain("secretoTotp");
      assertThat(cuerpo).doesNotContain("claveAlmacenamiento");
    }
  }

  @Test
  void sinSesionResponde401() {
    NavegadorDePrueba visitante = abrirNavegador();
    HttpResponse<String> respuesta =
        visitante.post(RUTA_SOLICITUDES, pedidoDeSolicitud(idServicio));
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("NO_AUTENTICADO");
  }

  @Test
  void unaDescripcionVaciaSeRechazaEnLaFrontera() {
    Map<String, Object> pedido = new HashMap<>(pedidoDeSolicitud(idServicio));
    pedido.put("descripcionNecesidad", "   ");

    HttpResponse<String> respuesta = cliente.post(RUTA_SOLICITUDES, pedido);
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("VALIDACION");
  }

  private NavegadorDePrueba clienteAutenticadoCon(String correo) {
    return cuentaAutenticada(correo);
  }
}
