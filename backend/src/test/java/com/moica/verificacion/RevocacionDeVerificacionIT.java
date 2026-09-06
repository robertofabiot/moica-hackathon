package com.moica.verificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * Qué deja sin efecto una revocación.
 *
 * <p>La regla difícil es la dependiente: si se revoca la básica, la profesional deja de surtir
 * efecto **en la misma transacción**, con el mismo motivo, el mismo administrador y el mismo
 * instante. Una profesional que sobreviviera diría que Moica respalda la trayectoria de alguien
 * cuya identidad ya no respalda.
 */
class RevocacionDeVerificacionIT extends EscenarioDeVerificacion {

  private static final String MOTIVO = "El documento presentado resultó no ser auténtico.";

  private NavegadorDePrueba admin;

  @BeforeEach
  void prepararAdministradora() {
    admin = administradora(CORREO_ADMIN);
  }

  @Test
  void revocarLaProfesionalDegradaElPerfilABasico() {
    long basica = aprobarBasica(admin);
    long profesional = aprobarProfesional();

    HttpResponse<String> respuesta = revocar(profesional);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(estadoDeLaSolicitud(profesional)).isEqualTo("REVOCADA");
    assertThat(estadoDeLaSolicitud(basica)).as("la básica no se toca").isEqualTo("APROBADA");
    assertThat(nivelDelPerfil()).isEqualTo("VERIFICADO_BASICO");
  }

  @Test
  void revocarLaBasicaDevuelveElPerfilASinVerificarYAnulaTambienLaProfesional() {
    long basica = aprobarBasica(admin);
    long profesional = aprobarProfesional();

    revocar(basica);

    assertThat(estadoDeLaSolicitud(basica)).isEqualTo("REVOCADA");
    assertThat(estadoDeLaSolicitud(profesional))
        .as("la profesional deja de surtir efecto con la básica")
        .isEqualTo("REVOCADA");
    assertThat(nivelDelPerfil()).isEqualTo("SIN_VERIFICAR");
  }

  @Test
  void laRevocacionDependienteUsaElMismoMotivoAdministradorEInstante() {
    long basica = aprobarBasica(admin);
    long profesional = aprobarProfesional();

    revocar(basica);

    List<Map<String, Object>> filas =
        jdbc.queryForList(
            """
            SELECT observacion_resolucion, id_administrador_revisor, fecha_resolucion
            FROM solicitud_verificacion_prestador
            WHERE id_solicitud_verificacion IN (?, ?)
            """,
            basica,
            profesional);

    assertThat(filas).hasSize(2);
    assertThat(filas.get(0)).isEqualTo(filas.get(1));
    assertThat(filas.get(0).get("observacion_resolucion")).isEqualTo(MOTIVO);
    assertThat(filas.get(0).get("id_administrador_revisor")).isEqualTo(idDe(CORREO_ADMIN));
  }

  @Test
  void unaProfesionalRevocadaNoRevivePorConseguirOtraBasica() {
    long primeraBasica = aprobarBasica(admin);
    long profesional = aprobarProfesional();
    revocar(primeraBasica);

    long segundaBasica = aprobarBasica(admin);

    assertThat(estadoDeLaSolicitud(segundaBasica)).isEqualTo("APROBADA");
    assertThat(estadoDeLaSolicitud(profesional)).isEqualTo("REVOCADA");
    assertThat(nivelDelPerfil())
        .as("recuperar la insignia profesional exige una solicitud nueva")
        .isEqualTo("VERIFICADO_BASICO");

    JsonNode estado = json(navegador.get(RUTA_VERIFICACION_PROPIA));
    assertThat(estado.get("puedeSolicitarProfesional").asBoolean()).isTrue();
  }

  @Test
  void revocarExigeUnMotivoNoVacio() {
    long basica = aprobarBasica(admin);

    HttpResponse<String> respuesta =
        admin.post(RUTA_REVISION + "/" + basica + "/revocacion", Map.of("observacion", "  "));

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("VALIDACION");
    assertThat(estadoDeLaSolicitud(basica)).isEqualTo("APROBADA");
    assertThat(nivelDelPerfil()).isEqualTo("VERIFICADO_BASICO");
  }

  @Test
  void soloSeRevocaLoQueEstabaAprobado() {
    long solicitud = enviarBasicaCorrecta();

    HttpResponse<String> pendiente = revocar(solicitud);
    assertThat(pendiente.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(codigoDeError(pendiente)).isEqualTo("TRANSICION_NO_PERMITIDA");

    admin.post(RUTA_REVISION + "/" + solicitud + "/toma", Map.of());
    assertThat(revocar(solicitud).statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(estadoDeLaSolicitud(solicitud)).isEqualTo("EN_REVISION");
  }

  @Test
  void unaRevocacionNoBorraLaEvidenciaDelExpediente() {
    long basica = aprobarBasica(admin);
    int documentosAntes = documentosGuardados();
    int objetosAntes = documentos.cantidadDeObjetos();

    revocar(basica);

    assertThat(documentosGuardados()).isEqualTo(documentosAntes);
    assertThat(documentos.cantidadDeObjetos())
        .as("los archivos de una solicitud resuelta no se retiran del bucket")
        .isEqualTo(objetosAntes);
    assertThat(documentos.clavesEliminadas()).isEmpty();
  }

  @Test
  void elMotivoDeLaRevocacionLlegaAQuienPerdioLaInsignia() {
    long basica = aprobarBasica(admin);

    revocar(basica);

    JsonNode propia = json(navegador.get(RUTA_SOLICITUDES_PROPIAS + "/" + basica));
    assertThat(propia.get("estadoSolicitud").asText()).isEqualTo("REVOCADA");
    assertThat(propia.get("observacionResolucion").asText()).isEqualTo(MOTIVO);

    JsonNode estado = json(navegador.get(RUTA_VERIFICACION_PROPIA));
    assertThat(estado.get("nivelVerificacion").asText()).isEqualTo("SIN_VERIFICAR");
    assertThat(estado.get("puedeSolicitarBasica").asBoolean())
        .as("después de una revocación se puede volver a presentar el expediente")
        .isTrue();
  }

  @Test
  void tambienSeRevocaDesdeLaColaDeResueltas() {
    long basica = aprobarBasica(admin);

    JsonNode aprobadas = json(admin.get(RUTA_REVISION + "?estado=APROBADA"));

    assertThat(aprobadas).hasSize(1);
    assertThat(aprobadas.get(0).get("idSolicitudVerificacion").asLong()).isEqualTo(basica);
  }

  private long aprobarProfesional() {
    long profesional =
        json(enviarExpediente("PROFESIONAL", certificado()))
            .get("idSolicitudVerificacion")
            .asLong();
    assertThat(admin.post(RUTA_REVISION + "/" + profesional + "/toma", Map.of()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(admin.post(RUTA_REVISION + "/" + profesional + "/aprobacion", Map.of()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(nivelDelPerfil()).isEqualTo("PROFESIONAL_VERIFICADO");
    return profesional;
  }

  private HttpResponse<String> revocar(long idSolicitud) {
    return admin.post(
        RUTA_REVISION + "/" + idSolicitud + "/revocacion", Map.of("observacion", MOTIVO));
  }
}
