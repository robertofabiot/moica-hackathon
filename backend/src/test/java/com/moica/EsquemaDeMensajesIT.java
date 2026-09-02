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
 * La migración {@code V41} sobre PostgreSQL real.
 *
 * <p>Comprueba la tabla, sus tipos, su nulabilidad, sus claves, la restricción que rechaza un
 * mensaje en blanco y el índice que permite leer el hilo en orden estable. Las reglas de
 * autorización y de estado se prueban en la API.
 */
class EsquemaDeMensajesIT extends PruebaDeIntegracionConPostgres {

  @Autowired private JdbcTemplate jdbc;

  private Long idCliente;
  private Long idPrestador;
  private Long idSolicitud;

  @BeforeEach
  void prepararSolicitud() {
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

    idCliente = insertarUsuario("Cliente de Prueba", "mensajes.cliente@moica.test");
    idPrestador = insertarUsuario("Prestador de Prueba", "mensajes.prestador@moica.test");

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
            VALUES (?, ?, ?, 'Necesito una reparación', 'Portón verde', 'ACEPTADA')
            RETURNING id_solicitud_servicio
            """,
            Long.class,
            idCliente,
            idServicio,
            idMunicipioManagua());
  }

  @Test
  void noExisteUnaTablaConversacion() {
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'conversacion'",
                Integer.class))
        .isZero();
  }

  @Test
  void elMensajeSeGuardaConSuRemitenteYSuInstantePorOmision() {
    Long idMensaje = insertarMensaje(idCliente, "Buenos días, ¿a qué hora llega?");

    assertThat(idMensaje).isNotNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT id_remitente FROM mensaje_solicitud WHERE id_mensaje_solicitud = ?",
                Long.class,
                idMensaje))
        .isEqualTo(idCliente);
    assertThat(
            jdbc.queryForObject(
                "SELECT fecha_envio IS NOT NULL FROM mensaje_solicitud"
                    + " WHERE id_mensaje_solicitud = ?",
                Boolean.class,
                idMensaje))
        .isTrue();
  }

  @Test
  void elContenidoNoAdmiteNiVacioNiSoloEspacios() {
    assertThatThrownBy(() -> insertarMensaje(idCliente, ""))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertarMensaje(idCliente, "   "))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertarMensaje(idCliente, "\t\n "))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void laSolicitudYElRemitenteSonObligatorios() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO mensaje_solicitud (id_solicitud_servicio, id_remitente, contenido)
                    VALUES (NULL, ?, 'Sin solicitud')
                    """,
                    idCliente))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO mensaje_solicitud (id_solicitud_servicio, id_remitente, contenido)
                    VALUES (?, NULL, 'Sin remitente')
                    """,
                    idSolicitud))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void unMensajeNoPuedeApuntarAUnaSolicitudNiAUnUsuarioInexistentes() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO mensaje_solicitud (id_solicitud_servicio, id_remitente, contenido)
                    VALUES (?, ?, 'Hilo inexistente')
                    """,
                    idSolicitud + 10_000,
                    idCliente))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO mensaje_solicitud (id_solicitud_servicio, id_remitente, contenido)
                    VALUES (?, ?, 'Remitente inexistente')
                    """,
                    idSolicitud,
                    idCliente + 10_000))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void noSeBorraEnCascadaNiLaSolicitudNiElRemitente() {
    insertarMensaje(idPrestador, "Voy en camino.");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM solicitud_servicio WHERE id_solicitud_servicio = ?", idSolicitud))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatThrownBy(() -> jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idPrestador))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void losTiposSonLosDelDiccionario() {
    assertThat(tipoDeColumna("mensaje_solicitud", "id_mensaje_solicitud")).isEqualTo("bigint");
    assertThat(tipoDeColumna("mensaje_solicitud", "id_solicitud_servicio")).isEqualTo("bigint");
    assertThat(tipoDeColumna("mensaje_solicitud", "id_remitente")).isEqualTo("bigint");
    assertThat(tipoDeColumna("mensaje_solicitud", "contenido")).isEqualTo("text");
    assertThat(tipoDeColumna("mensaje_solicitud", "fecha_envio"))
        .isEqualTo("timestamp with time zone");
    assertThat(esIdentidad("mensaje_solicitud", "id_mensaje_solicitud")).isEqualTo("ALWAYS");
  }

  @Test
  void elIndiceDelHiloOrdenaPorSolicitudFechaEIdentificador() {
    List<String> indices =
        jdbc.queryForList(
            """
            SELECT indexname FROM pg_indexes
            WHERE tablename = 'mensaje_solicitud'
            ORDER BY indexname
            """,
            String.class);

    assertThat(indices).contains("pk_mensaje_solicitud", "ix_mensaje_solicitud_id_solicitud");
    assertThat(
            jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname ="
                    + " 'ix_mensaje_solicitud_id_solicitud'",
                String.class))
        .contains("id_solicitud_servicio", "fecha_envio", "id_mensaje_solicitud");
  }

  private Long insertarMensaje(Long idRemitente, String contenido) {
    return jdbc.queryForObject(
        """
        INSERT INTO mensaje_solicitud (id_solicitud_servicio, id_remitente, contenido)
        VALUES (?, ?, ?)
        RETURNING id_mensaje_solicitud
        """,
        Long.class,
        idSolicitud,
        idRemitente,
        contenido);
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
