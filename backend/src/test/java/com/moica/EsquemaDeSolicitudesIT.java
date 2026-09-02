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
 * La migración {@code V40} sobre PostgreSQL real.
 *
 * <p>Comprueba claves, dominios, la transición inicial nula y los índices de bandeja e historial.
 * Las reglas de autorización y de flujo se prueban en la API.
 */
class EsquemaDeSolicitudesIT extends PruebaDeIntegracionConPostgres {

  @Autowired private JdbcTemplate jdbc;

  private Long idCliente;
  private Long idPrestador;
  private Long idServicio;

  @BeforeEach
  void prepararCuentasYServicio() {
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

    idCliente = insertarUsuario("Cliente de Prueba", "esquema.cliente@moica.test");
    idPrestador = insertarUsuario("Prestador de Prueba", "esquema.prestador@moica.test");

    jdbc.update(
        """
        INSERT INTO perfil_prestador
            (id_prestador, nombre_publico, descripcion, tipo_prestador,
             id_municipio_principal, descripcion_cobertura)
        VALUES (?, 'Taller de Prueba', 'Descripción', 'INDEPENDIENTE', ?, 'Cobertura')
        """,
        idPrestador,
        idMunicipioManagua());

    idServicio =
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
  }

  @Test
  void laSolicitudNacePendienteYElCambioInicialAdmiteEstadoAnteriorNulo() {
    Long idSolicitud = insertarSolicitud("PENDIENTE");

    assertThat(
            jdbc.queryForObject(
                "SELECT estado_actual FROM solicitud_servicio WHERE id_solicitud_servicio = ?",
                String.class,
                idSolicitud))
        .isEqualTo("PENDIENTE");

    Long idCambio =
        jdbc.queryForObject(
            """
            INSERT INTO cambio_estado_solicitud
                (id_solicitud_servicio, estado_anterior, estado_nuevo, id_actor)
            VALUES (?, NULL, 'PENDIENTE', ?)
            RETURNING id_cambio_estado_solicitud
            """,
            Long.class,
            idSolicitud,
            idCliente);

    assertThat(idCambio).isNotNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT estado_anterior FROM cambio_estado_solicitud"
                    + " WHERE id_cambio_estado_solicitud = ?",
                String.class,
                idCambio))
        .isNull();
  }

  @Test
  void elDominioDelEstadoSeImponeEnLaSolicitudYEnElHistorial() {
    assertThatThrownBy(() -> insertarSolicitud("BORRADOR"))
        .isInstanceOf(DataIntegrityViolationException.class);

    Long idSolicitud = insertarSolicitud("PENDIENTE");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO cambio_estado_solicitud
                        (id_solicitud_servicio, estado_anterior, estado_nuevo, id_actor)
                    VALUES (?, 'PENDIENTE', 'ARCHIVADA', ?)
                    """,
                    idSolicitud,
                    idCliente))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void noSePuedeRegistrarUnaTransicionHaciaElMismoEstado() {
    Long idSolicitud = insertarSolicitud("PENDIENTE");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO cambio_estado_solicitud
                        (id_solicitud_servicio, estado_anterior, estado_nuevo, id_actor)
                    VALUES (?, 'PENDIENTE', 'PENDIENTE', ?)
                    """,
                    idSolicitud,
                    idCliente))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void noSePuedeBorrarElServicioNiElClienteMientrasHayaSolicitud() {
    insertarSolicitud("PENDIENTE");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM servicio_publicado WHERE id_servicio_publicado = ?", idServicio))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatThrownBy(() -> jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idCliente))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void noSePuedeBorrarLaSolicitudMientrasTengaHistorial() {
    Long idSolicitud = insertarSolicitud("PENDIENTE");
    jdbc.update(
        """
        INSERT INTO cambio_estado_solicitud
            (id_solicitud_servicio, estado_anterior, estado_nuevo, id_actor)
        VALUES (?, NULL, 'PENDIENTE', ?)
        """,
        idSolicitud,
        idCliente);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM solicitud_servicio WHERE id_solicitud_servicio = ?", idSolicitud))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void laFechaPreferidaEsDateYLosInstantesSonTimestamptz() {
    assertThat(tipoDeColumna("solicitud_servicio", "fecha_preferida")).isEqualTo("date");
    assertThat(tipoDeColumna("solicitud_servicio", "fecha_creacion"))
        .isEqualTo("timestamp with time zone");
    assertThat(tipoDeColumna("cambio_estado_solicitud", "fecha_cambio"))
        .isEqualTo("timestamp with time zone");
    assertThat(tipoDeColumna("solicitud_servicio", "id_solicitud_servicio")).isEqualTo("bigint");
  }

  @Test
  void existenLosIndicesDeBandejaPropiedadEstadoEHistorial() {
    List<String> indices =
        jdbc.queryForList(
            """
            SELECT indexname FROM pg_indexes
            WHERE tablename IN ('solicitud_servicio', 'cambio_estado_solicitud')
            ORDER BY indexname
            """,
            String.class);

    assertThat(indices)
        .contains(
            "ix_solicitud_servicio_id_cliente",
            "ix_solicitud_servicio_id_servicio_publicado",
            "ix_solicitud_servicio_estado_actual",
            "ix_cambio_estado_solicitud_id_solicitud");
  }

  private Long insertarSolicitud(String estado) {
    return jdbc.queryForObject(
        """
        INSERT INTO solicitud_servicio
            (id_cliente, id_servicio_publicado, id_municipio,
             descripcion_necesidad, indicacion_ubicacion, estado_actual)
        VALUES (?, ?, ?, 'Necesito una reparación', 'Portón verde', ?)
        RETURNING id_solicitud_servicio
        """,
        Long.class,
        idCliente,
        idServicio,
        idMunicipioManagua(),
        estado);
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
}
