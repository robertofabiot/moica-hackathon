package com.moica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La migración {@code V42} sobre PostgreSQL real.
 *
 * <p>Comprueba la tabla, sus tipos, su nulabilidad, sus claves y las cuatro restricciones que la
 * base sostiene por sí sola: el rango de la puntuación, que nadie se califique a sí mismo, que cada
 * participante califique una vez y que reciba como máximo una. Las reglas que dependen de otras
 * tablas —estado completado, participación y correspondencia del rol— se prueban en la API.
 */
class EsquemaDeCalificacionesIT extends PruebaDeIntegracionConPostgres {

  @Autowired private JdbcTemplate jdbc;

  private Long idCliente;
  private Long idPrestador;
  private Long idTercero;
  private Long idSolicitud;

  @BeforeEach
  void prepararSolicitudCompletada() {
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

    idCliente = insertarUsuario("Cliente de Prueba", "calificacion.cliente@moica.test");
    idPrestador = insertarUsuario("Prestador de Prueba", "calificacion.prestador@moica.test");
    idTercero = insertarUsuario("Tercero de Prueba", "calificacion.tercero@moica.test");

    jdbc.update(
        """
        INSERT INTO perfil_prestador
            (id_prestador, nombre_publico, descripcion, tipo_prestador,
             id_municipio_principal, descripcion_cobertura)
        VALUES (?, 'Taller de Prueba', 'Descripción', 'INDEPENDIENTE', ?, 'Cobertura')
        """,
        idPrestador,
        idMunicipioManagua());

    Long idServicio =
        jdbc.queryForObject(
            """
            INSERT INTO servicio_publicado
                (id_prestador, id_subcategoria_servicio, nombre, descripcion, estado)
            VALUES (?, ?, 'Reparación', 'Descripción', 'ACTIVO')
            RETURNING id_servicio_publicado
            """,
            Long.class,
            idPrestador,
            idSubcategoriaPlomeria());

    idSolicitud =
        jdbc.queryForObject(
            """
            INSERT INTO solicitud_servicio
                (id_cliente, id_servicio_publicado, id_municipio,
                 descripcion_necesidad, indicacion_ubicacion, estado_actual)
            VALUES (?, ?, ?, 'Necesito una reparación', 'Portón verde', 'COMPLETADA')
            RETURNING id_solicitud_servicio
            """,
            Long.class,
            idCliente,
            idServicio,
            idMunicipioManagua());
  }

  @Test
  void noExisteUnaTablaReputacion() {
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'reputacion'",
                Integer.class))
        .isZero();
  }

  @Test
  void laCalificacionSeGuardaConSuRolYSuInstantePorOmision() {
    Long idCalificacion = insertar(idCliente, idPrestador, "PRESTADOR", 5, "Trabajo impecable.");

    assertThat(idCalificacion).isNotNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT rol_calificado FROM calificacion_usuario"
                    + " WHERE id_calificacion_usuario = ?",
                String.class,
                idCalificacion))
        .isEqualTo("PRESTADOR");
    assertThat(
            jdbc.queryForObject(
                "SELECT fecha_creacion IS NOT NULL FROM calificacion_usuario"
                    + " WHERE id_calificacion_usuario = ?",
                Boolean.class,
                idCalificacion))
        .isTrue();
  }

  @Test
  void elComentarioEsOpcional() {
    Long idCalificacion = insertar(idCliente, idPrestador, "PRESTADOR", 4, null);

    assertThat(
            jdbc.queryForObject(
                "SELECT comentario FROM calificacion_usuario WHERE id_calificacion_usuario = ?",
                String.class,
                idCalificacion))
        .isNull();
  }

  @Test
  void laPuntuacionSoloAdmiteDeUnaACincoEstrellas() {
    assertThatThrownBy(() -> insertar(idCliente, idPrestador, "PRESTADOR", 0, null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertar(idCliente, idPrestador, "PRESTADOR", 6, null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertar(idCliente, idPrestador, "PRESTADOR", -1, null))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(insertar(idCliente, idPrestador, "PRESTADOR", 1, null)).isNotNull();
    assertThat(insertar(idPrestador, idCliente, "CLIENTE", 5, null)).isNotNull();
  }

  @Test
  void nadiePuedeCalificarseASiMismo() {
    assertThatThrownBy(() -> insertar(idCliente, idCliente, "CLIENTE", 5, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elRolSoloAdmiteClienteYPrestador() {
    assertThatThrownBy(() -> insertar(idCliente, idPrestador, "AMBOS", 5, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void cadaParticipanteCalificaUnaSolaVezPorSolicitud() {
    insertar(idCliente, idPrestador, "PRESTADOR", 5, null);

    assertThatThrownBy(() -> insertar(idCliente, idTercero, "PRESTADOR", 4, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void cadaParticipanteRecibeComoMaximoUnaCalificacionPorSolicitud() {
    insertar(idCliente, idPrestador, "PRESTADOR", 5, null);

    assertThatThrownBy(() -> insertar(idTercero, idPrestador, "PRESTADOR", 1, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void unaSolicitudAdmiteLasDosCalificaciones() {
    insertar(idCliente, idPrestador, "PRESTADOR", 5, "Muy puntual.");
    insertar(idPrestador, idCliente, "CLIENTE", 4, "Todo claro desde el principio.");

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM calificacion_usuario WHERE id_solicitud_servicio = ?",
                Integer.class,
                idSolicitud))
        .isEqualTo(2);
  }

  @Test
  void laSolicitudYLasDosPersonasSonObligatorias() {
    assertThatThrownBy(() -> insertarEn(null, idCliente, idPrestador, "PRESTADOR", 5))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertarEn(idSolicitud, null, idPrestador, "PRESTADOR", 5))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertarEn(idSolicitud, idCliente, null, "PRESTADOR", 5))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void unaCalificacionNoPuedeApuntarAUnaSolicitudNiAUnUsuarioInexistentes() {
    assertThatThrownBy(
            () -> insertarEn(idSolicitud + 10_000, idCliente, idPrestador, "PRESTADOR", 5))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> insertarEn(idSolicitud, idCliente + 10_000, idPrestador, "PRESTADOR", 5))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> insertarEn(idSolicitud, idCliente, idPrestador + 10_000, "PRESTADOR", 5))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void noSeBorraEnCascadaNiLaSolicitudNiNingunaDeLasDosPersonas() {
    insertar(idCliente, idPrestador, "PRESTADOR", 5, null);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM solicitud_servicio WHERE id_solicitud_servicio = ?", idSolicitud))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idCliente))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idPrestador))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void losTiposSonLosDelDiccionario() {
    assertThat(tipoDeColumna("calificacion_usuario", "id_calificacion_usuario"))
        .isEqualTo("bigint");
    assertThat(tipoDeColumna("calificacion_usuario", "id_solicitud_servicio")).isEqualTo("bigint");
    assertThat(tipoDeColumna("calificacion_usuario", "id_calificador")).isEqualTo("bigint");
    assertThat(tipoDeColumna("calificacion_usuario", "id_calificado")).isEqualTo("bigint");
    assertThat(tipoDeColumna("calificacion_usuario", "rol_calificado"))
        .isEqualTo("character varying");
    assertThat(tipoDeColumna("calificacion_usuario", "puntuacion")).isEqualTo("smallint");
    assertThat(tipoDeColumna("calificacion_usuario", "comentario")).isEqualTo("text");
    assertThat(tipoDeColumna("calificacion_usuario", "fecha_creacion"))
        .isEqualTo("timestamp with time zone");
    assertThat(esIdentidad("calificacion_usuario", "id_calificacion_usuario")).isEqualTo("ALWAYS");
  }

  @Test
  void elIndiceDeReputacionAgrupaPorCalificadoRolYPuntuacion() {
    List<String> indices =
        jdbc.queryForList(
            """
            SELECT indexname FROM pg_indexes
            WHERE tablename = 'calificacion_usuario'
            ORDER BY indexname
            """,
            String.class);

    assertThat(indices)
        .contains(
            "pk_calificacion_usuario",
            "uq_calificacion_usuario_solicitud_calificador",
            "uq_calificacion_usuario_solicitud_calificado",
            "ix_calificacion_usuario_calificado_rol");
    assertThat(
            jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname ="
                    + " 'ix_calificacion_usuario_calificado_rol'",
                String.class))
        .contains("id_calificado", "rol_calificado", "puntuacion");
  }

  private Long insertar(
      Long idCalificador, Long idCalificado, String rol, int puntuacion, String comentario) {
    return jdbc.queryForObject(
        """
        INSERT INTO calificacion_usuario
            (id_solicitud_servicio, id_calificador, id_calificado,
             rol_calificado, puntuacion, comentario)
        VALUES (?, ?, ?, ?, ?, ?)
        RETURNING id_calificacion_usuario
        """,
        Long.class,
        idSolicitud,
        idCalificador,
        idCalificado,
        rol,
        puntuacion,
        comentario);
  }

  private void insertarEn(
      Long idSolicitudServicio, Long idCalificador, Long idCalificado, String rol, int puntuacion) {
    jdbc.update(
        """
        INSERT INTO calificacion_usuario
            (id_solicitud_servicio, id_calificador, id_calificado, rol_calificado, puntuacion)
        VALUES (?, ?, ?, ?, ?)
        """,
        idSolicitudServicio,
        idCalificador,
        idCalificado,
        rol,
        puntuacion);
  }

  private Long insertarUsuario(String nombre, String correo) {
    return jdbc.queryForObject(
        """
        INSERT INTO usuario (nombre_completo, correo_electronico, clave_hash)
        VALUES (?, ?, '$2a$10$hashDePruebaQueNoCorrespondeANadie')
        RETURNING id_usuario
        """,
        Long.class,
        nombre,
        correo);
  }

  private Integer idMunicipioManagua() {
    return jdbc.queryForObject(
        """
        SELECT m.id_municipio FROM municipio m
        JOIN departamento d ON d.id_departamento = m.id_departamento
        WHERE d.nombre = 'Managua' AND m.nombre = 'Managua'
        """,
        Integer.class);
  }

  private Integer idSubcategoriaPlomeria() {
    return jdbc.queryForObject(
        """
        SELECT s.id_subcategoria_servicio
        FROM subcategoria_servicio s
        JOIN categoria_servicio c ON c.id_categoria_servicio = s.id_categoria_servicio
        WHERE c.nombre = 'Hogar y mantenimiento' AND s.nombre = 'Plomería'
        """,
        Integer.class);
  }

  private String tipoDeColumna(String tabla, String columna) {
    return jdbc.queryForObject(
        """
        SELECT data_type FROM information_schema.columns
        WHERE table_name = ? AND column_name = ?
        """,
        String.class,
        tabla,
        columna);
  }

  private String esIdentidad(String tabla, String columna) {
    return jdbc.queryForObject(
        """
        SELECT identity_generation FROM information_schema.columns
        WHERE table_name = ? AND column_name = ?
        """,
        String.class,
        tabla,
        columna);
  }
}
