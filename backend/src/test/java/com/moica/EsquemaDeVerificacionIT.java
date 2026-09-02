package com.moica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La migración {@code V30} sobre PostgreSQL real.
 *
 * <p>Comprueba lo que el diccionario encarga a la base de datos: dominios, valores por omisión, el
 * índice parcial de la solicitud abierta, la coherencia de fechas, la observación obligatoria de
 * toda decisión negativa, el tope de tamaño y las dos cascadas. Las reglas que el diccionario
 * encarga a la aplicación —las transiciones, el nivel previo, la propiedad y el segundo factor del
 * revisor— se prueban en las pruebas de API.
 *
 * <p>Se usa PostgreSQL real y no H2 porque un índice parcial con {@code WHERE} sobre un conjunto de
 * estados no existe en H2: probarlo allí demostraría otra cosa.
 */
class EsquemaDeVerificacionIT extends PruebaDeIntegracionConPostgres {

  @Autowired private JdbcTemplate jdbc;

  private Long idPrestador;
  private Long idAdministrador;

  @BeforeEach
  void prepararUnPerfilYUnAdministrador() {
    jdbc.update("DELETE FROM historial_caso");
    jdbc.update("DELETE FROM caso_moderacion");
    jdbc.update("DELETE FROM calificacion_usuario");
    jdbc.update("DELETE FROM mensaje_solicitud");
    jdbc.update("DELETE FROM cambio_estado_solicitud");
    jdbc.update("DELETE FROM solicitud_servicio");
    jdbc.update("DELETE FROM solicitud_verificacion_prestador");
    jdbc.update("DELETE FROM imagen_servicio_publicado");
    jdbc.update("DELETE FROM servicio_publicado");
    jdbc.update("DELETE FROM usuario");

    idPrestador = crearCuenta("prestador.esquema@moica.test");
    idAdministrador = crearCuenta("admin.esquema@moica.test");

    jdbc.update("INSERT INTO administrador (id_administrador) VALUES (?)", idAdministrador);
    jdbc.update(
        """
        INSERT INTO perfil_prestador (
            id_prestador, nombre_publico, descripcion, tipo_prestador,
            id_municipio_principal, descripcion_cobertura)
        SELECT ?, 'Taller de Esquema', 'Descripcion', 'INDEPENDIENTE', m.id_municipio, 'Managua'
        FROM municipio m
        JOIN departamento d ON d.id_departamento = m.id_departamento
        WHERE d.nombre = 'Managua' AND m.nombre = 'Managua'
        """,
        idPrestador);
  }

  @Test
  void unaSolicitudNaceComoPendienteYSinRevisor() {
    long id = crearSolicitud("BASICA");

    assertThat(estadoDe(id)).isEqualTo("PENDIENTE");
    assertThat(
            jdbc.queryForObject(
                "SELECT id_administrador_revisor FROM solicitud_verificacion_prestador"
                    + " WHERE id_solicitud_verificacion = ?",
                Long.class,
                id))
        .isNull();
  }

  @Test
  void noAdmiteDosSolicitudesAbiertasDelMismoNivel() {
    crearSolicitud("BASICA");

    assertThatThrownBy(() -> crearSolicitud("BASICA"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void siAdmiteUnaAbiertaDeCadaNivel() {
    crearSolicitud("BASICA");

    assertThatCode(() -> crearSolicitud("PROFESIONAL")).doesNotThrowAnyException();
  }

  @Test
  void unaVezResueltaDejaSitioParaLaSiguiente() {
    long primera = crearSolicitud("BASICA");
    resolver(primera, "RECHAZADA", "Falta el reverso del documento.");

    assertThatCode(() -> crearSolicitud("BASICA")).doesNotThrowAnyException();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM solicitud_verificacion_prestador", Integer.class))
        .as("la rechazada se conserva como evidencia")
        .isEqualTo(2);
  }

  @Test
  void elIndiceParcialSoloAlcanzaAlMismoPerfil() {
    crearSolicitud("BASICA");

    Long otroPrestador = crearCuenta("otro.esquema@moica.test");
    jdbc.update(
        """
        INSERT INTO perfil_prestador (
            id_prestador, nombre_publico, descripcion, tipo_prestador,
            id_municipio_principal, descripcion_cobertura)
        SELECT ?, 'Otro taller', 'Descripcion', 'PYME', m.id_municipio, 'Managua'
        FROM municipio m
        JOIN departamento d ON d.id_departamento = m.id_departamento
        WHERE d.nombre = 'Managua' AND m.nombre = 'Managua'
        """,
        otroPrestador);

    assertThatCode(
            () ->
                jdbc.update(
                    "INSERT INTO solicitud_verificacion_prestador (id_prestador, nivel_solicitado)"
                        + " VALUES (?, 'BASICA')",
                    otroPrestador))
        .doesNotThrowAnyException();
  }

  @Test
  void rechazaUnNivelOUnEstadoFueraDelDominio() {
    assertThatThrownBy(() -> crearSolicitud("PREMIUM"))
        .isInstanceOf(DataIntegrityViolationException.class);

    long id = crearSolicitud("BASICA");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE solicitud_verificacion_prestador SET estado_solicitud = 'ARCHIVADA'"
                        + " WHERE id_solicitud_verificacion = ?",
                    id))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void unaDecisionNegativaExigeObservacionNoVacia() {
    long id = crearSolicitud("BASICA");

    assertThatThrownBy(() -> resolver(id, "RECHAZADA", null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> resolver(id, "REVOCADA", "   "))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatCode(() -> resolver(id, "RECHAZADA", "Documento ilegible."))
        .doesNotThrowAnyException();
  }

  @Test
  void unaAprobacionNoNecesitaObservacionPeroSiFechaDeResolucion() {
    long id = crearSolicitud("BASICA");

    assertThatCode(() -> resolver(id, "APROBADA", null)).doesNotThrowAnyException();

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE solicitud_verificacion_prestador SET fecha_resolucion = NULL"
                        + " WHERE id_solicitud_verificacion = ?",
                    id))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void laRevisionYLaResolucionNuncaPrecedenAlEnvio() {
    long id = crearSolicitud("BASICA");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    UPDATE solicitud_verificacion_prestador
                    SET fecha_inicio_revision = fecha_solicitud - INTERVAL '1 minute'
                    WHERE id_solicitud_verificacion = ?
                    """,
                    id))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    UPDATE solicitud_verificacion_prestador
                    SET estado_solicitud = 'APROBADA',
                        fecha_resolucion = fecha_solicitud - INTERVAL '1 second'
                    WHERE id_solicitud_verificacion = ?
                    """,
                    id))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void unDocumentoExigeUnTipoUnMimeYUnTamanoDelDominio() {
    long id = crearSolicitud("BASICA");

    assertThatCode(() -> crearDocumento(id, "IDENTIDAD", "application/pdf", 1024))
        .doesNotThrowAnyException();

    assertThatThrownBy(() -> crearDocumento(id, "SELFIE", "application/pdf", 1024))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> crearDocumento(id, "IDENTIDAD", "image/webp", 1024))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> crearDocumento(id, "IDENTIDAD", "image/png", 0))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> crearDocumento(id, "IDENTIDAD", "image/png", 5 * 1024 * 1024 + 1))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatCode(() -> crearDocumento(id, "IDENTIDAD", "image/png", 5 * 1024 * 1024))
        .doesNotThrowAnyException();
  }

  @Test
  void laClaveDeAlmacenamientoIdentificaUnUnicoArchivo() {
    long id = crearSolicitud("BASICA");
    jdbc.update(
        """
        INSERT INTO documento_verificacion_prestador (
            id_solicitud_verificacion, tipo_documento, clave_almacenamiento,
            nombre_original, tipo_mime, tamano_bytes)
        VALUES (?, 'IDENTIDAD', 'expedientes/repetida.png', 'cedula.png', 'image/png', 10)
        """,
        id);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO documento_verificacion_prestador (
                        id_solicitud_verificacion, tipo_documento, clave_almacenamiento,
                        nombre_original, tipo_mime, tamano_bytes)
                    VALUES (?, 'CONSTANCIA', 'expedientes/repetida.png', 'otra.png', 'image/png', 10)
                    """,
                    id))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void borrarElPerfilArrastraSusSolicitudesYSusDocumentos() {
    long id = crearSolicitud("BASICA");
    crearDocumento(id, "IDENTIDAD", "image/png", 1024);

    jdbc.update("DELETE FROM perfil_prestador WHERE id_prestador = ?", idPrestador);

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM solicitud_verificacion_prestador", Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM documento_verificacion_prestador", Integer.class))
        .isZero();
  }

  @Test
  void quienRevisoUnExpedienteNoDesaparaceDelHistorial() {
    long id = crearSolicitud("BASICA");
    resolver(id, "APROBADA", null);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM administrador WHERE id_administrador = ?", idAdministrador))
        .as("la clave foránea al administrador es RESTRICT, no CASCADE")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private Long crearCuenta(String correo) {
    return jdbc.queryForObject(
        """
        INSERT INTO usuario (nombre_completo, correo_electronico, clave_hash)
        VALUES ('Persona de Prueba', ?, '$2a$10$hashDePruebaQueNoCorrespondeANadie')
        RETURNING id_usuario
        """,
        Long.class,
        correo);
  }

  private long crearSolicitud(String nivel) {
    return Objects.requireNonNull(
        jdbc.queryForObject(
            "INSERT INTO solicitud_verificacion_prestador (id_prestador, nivel_solicitado)"
                + " VALUES (?, ?) RETURNING id_solicitud_verificacion",
            Long.class,
            idPrestador,
            nivel));
  }

  private void crearDocumento(long idSolicitud, String tipo, String mime, int tamano) {
    jdbc.update(
        """
        INSERT INTO documento_verificacion_prestador (
            id_solicitud_verificacion, tipo_documento, clave_almacenamiento,
            nombre_original, tipo_mime, tamano_bytes)
        VALUES (?, ?, 'expedientes/' || gen_random_uuid() || '.bin', 'documento', ?, ?)
        """,
        idSolicitud,
        tipo,
        mime,
        tamano);
  }

  private void resolver(long idSolicitud, String estado, String observacion) {
    jdbc.update(
        """
        UPDATE solicitud_verificacion_prestador
        SET estado_solicitud = ?,
            observacion_resolucion = ?,
            id_administrador_revisor = ?,
            fecha_resolucion = CURRENT_TIMESTAMP
        WHERE id_solicitud_verificacion = ?
        """,
        estado,
        observacion,
        idAdministrador,
        idSolicitud);
  }

  private String estadoDe(long idSolicitud) {
    return jdbc.queryForObject(
        "SELECT estado_solicitud FROM solicitud_verificacion_prestador"
            + " WHERE id_solicitud_verificacion = ?",
        String.class,
        idSolicitud);
  }
}
