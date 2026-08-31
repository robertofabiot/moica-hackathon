package com.moica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Las migraciones {@code V31} y {@code V90} sobre PostgreSQL real.
 *
 * <p>Comprueba claves, unicidad, dominios, precio, relaciones y la taxonomía de demostración. Las
 * reglas de activación y visibilidad se prueban en las pruebas de API.
 */
class EsquemaDeServiciosIT extends PruebaDeIntegracionConPostgres {

  @Autowired private JdbcTemplate jdbc;

  private Long idUsuario;

  @BeforeEach
  void prepararUnaCuentaConPerfil() {
    jdbc.update("DELETE FROM mensaje_solicitud");
    jdbc.update("DELETE FROM cambio_estado_solicitud");
    jdbc.update("DELETE FROM solicitud_servicio");
    jdbc.update("DELETE FROM solicitud_verificacion_prestador");
    jdbc.update("DELETE FROM imagen_servicio_publicado");
    jdbc.update("DELETE FROM servicio_publicado");
    jdbc.update("DELETE FROM usuario");

    idUsuario =
        jdbc.queryForObject(
            """
            INSERT INTO usuario (nombre_completo, correo_electronico, clave_hash)
            VALUES ('Persona de Prueba', 'esquema.servicio@moica.test',
                    '$2a$10$hashDePruebaQueNoCorrespondeANadie')
            RETURNING id_usuario
            """,
            Long.class);

    jdbc.update(
        """
        INSERT INTO perfil_prestador
            (id_prestador, nombre_publico, descripcion, tipo_prestador,
             id_municipio_principal, descripcion_cobertura)
        VALUES (?, 'Taller de Prueba', 'Descripción', 'INDEPENDIENTE', ?, 'Cobertura')
        """,
        idUsuario,
        idMunicipioManagua());
  }

  @Test
  void laTaxonomiaDeDemostracionTieneTresCategoriasConTresSubcategoriasCadaUna() {
    List<String> categorias =
        jdbc.queryForList("SELECT nombre FROM categoria_servicio ORDER BY nombre", String.class);

    assertThat(categorias)
        .containsExactly(
            "Belleza y cuidado personal",
            "Hogar y mantenimiento",
            "Tecnología y servicios digitales");

    assertThat(subcategoriasDe("Hogar y mantenimiento"))
        .containsExactly("Carpintería", "Electricidad", "Plomería");
    assertThat(subcategoriasDe("Belleza y cuidado personal"))
        .containsExactly("Barbería/peluquería", "Maquillaje", "Uñas");
    assertThat(subcategoriasDe("Tecnología y servicios digitales"))
        .containsExactly("Diseño gráfico", "Reparación de computadoras", "Soporte técnico");
  }

  @Test
  void elNombreDeCategoriaNoSeRepite() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO categoria_servicio (nombre) VALUES ('Hogar y mantenimiento')"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elNombreDeSubcategoriaNoSeRepiteDentroDeLaMismaCategoria() {
    Short idHogar = idCategoria("Hogar y mantenimiento");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO subcategoria_servicio (id_categoria_servicio, nombre)"
                        + " VALUES (?, 'Plomería')",
                    idHogar))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elMismoNombreDeSubcategoriaSiCabeEnOtraCategoria() {
    Short idBelleza = idCategoria("Belleza y cuidado personal");

    Integer insertada =
        jdbc.queryForObject(
            "INSERT INTO subcategoria_servicio (id_categoria_servicio, nombre)"
                + " VALUES (?, 'Plomería') RETURNING id_subcategoria_servicio",
            Integer.class,
            idBelleza);

    assertThat(insertada).isNotNull();
    jdbc.update("DELETE FROM subcategoria_servicio WHERE id_subcategoria_servicio = ?", insertada);
  }

  @Test
  void elServicioNaceActivoPorOmisionEnLaBase() {
    Long id =
        jdbc.queryForObject(
            """
            INSERT INTO servicio_publicado
                (id_prestador, id_subcategoria_servicio, nombre, descripcion)
            VALUES (?, ?, 'Reparación de fuga', 'Cambio de empaques')
            RETURNING id_servicio_publicado
            """,
            Long.class,
            idUsuario,
            idSubcategoria("Hogar y mantenimiento", "Plomería"));

    assertThat(
            jdbc.queryForObject(
                "SELECT estado FROM servicio_publicado WHERE id_servicio_publicado = ?",
                String.class,
                id))
        .isEqualTo("ACTIVO");
  }

  @Test
  void elDominioDelEstadoSeImponeEnLaBase() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO servicio_publicado
                        (id_prestador, id_subcategoria_servicio, nombre, descripcion, estado)
                    VALUES (?, ?, 'X', 'Y', 'BORRADOR')
                    """,
                    idUsuario,
                    idSubcategoria("Hogar y mantenimiento", "Plomería")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elPrecioNuloSeAdmiteYElCeroONegativoNo() {
    Integer idSub = idSubcategoria("Hogar y mantenimiento", "Plomería");

    assertThat(
            jdbc.update(
                """
                INSERT INTO servicio_publicado
                    (id_prestador, id_subcategoria_servicio, nombre, descripcion, precio_referencia)
                VALUES (?, ?, 'A convenir', 'Sin precio', NULL)
                """,
                idUsuario,
                idSub))
        .isEqualTo(1);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO servicio_publicado
                        (id_prestador, id_subcategoria_servicio, nombre, descripcion,
                         precio_referencia)
                    VALUES (?, ?, 'Gratis', 'Cero', 0)
                    """,
                    idUsuario,
                    idSub))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO servicio_publicado
                        (id_prestador, id_subcategoria_servicio, nombre, descripcion,
                         precio_referencia)
                    VALUES (?, ?, 'Negativo', 'Menos', -10)
                    """,
                    idUsuario,
                    idSub))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void unPrecioPositivoSeGuardaConDosDecimales() {
    jdbc.update(
        """
        INSERT INTO servicio_publicado
            (id_prestador, id_subcategoria_servicio, nombre, descripcion, precio_referencia)
        VALUES (?, ?, 'Visita', 'Diagnóstico', 350.50)
        """,
        idUsuario,
        idSubcategoria("Hogar y mantenimiento", "Plomería"));

    BigDecimal precio =
        jdbc.queryForObject(
            "SELECT precio_referencia FROM servicio_publicado WHERE nombre = 'Visita'",
            BigDecimal.class);

    assertThat(precio).isEqualByComparingTo("350.50");
  }

  @Test
  void noSePuedeBorrarElPerfilMientrasTengaServicios() {
    insertarServicio("Instalación", "INACTIVO");

    assertThatThrownBy(() -> jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idUsuario))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void noSePuedeBorrarUnaSubcategoriaReferenciada() {
    insertarServicio("Instalación", "INACTIVO");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM subcategoria_servicio WHERE id_subcategoria_servicio = ?",
                    idSubcategoria("Hogar y mantenimiento", "Plomería")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void borrarUnServicioArrastraSusImagenes() {
    Long idServicio = insertarServicio("Con imagen", "INACTIVO");
    jdbc.update(
        "INSERT INTO imagen_servicio_publicado (id_servicio_publicado, url_imagen)"
            + " VALUES (?, 'https://imagenes.moica.test/servicios/x.png')",
        idServicio);

    jdbc.update("DELETE FROM servicio_publicado WHERE id_servicio_publicado = ?", idServicio);

    assertThat(jdbc.queryForObject("SELECT count(*) FROM imagen_servicio_publicado", Integer.class))
        .isZero();
  }

  @Test
  void lasColumnasDeImagenGuardanUnaUrlNuncaUnBinario() {
    String tipo =
        jdbc.queryForObject(
            """
            SELECT data_type FROM information_schema.columns
            WHERE table_name = 'imagen_servicio_publicado' AND column_name = 'url_imagen'
            """,
            String.class);

    assertThat(tipo).isEqualTo("character varying");
  }

  @Test
  void existenLosIndicesJustificados() {
    List<String> indices =
        jdbc.queryForList(
            """
            SELECT indexname FROM pg_indexes
            WHERE tablename IN (
                'servicio_publicado', 'imagen_servicio_publicado', 'subcategoria_servicio')
            ORDER BY indexname
            """,
            String.class);

    assertThat(indices)
        .contains(
            "ix_servicio_publicado_id_prestador",
            "ix_servicio_publicado_id_subcategoria",
            "ix_servicio_publicado_estado",
            "ix_imagen_servicio_publicado_id_servicio",
            "ix_subcategoria_servicio_id_categoria");
  }

  private Long insertarServicio(String nombre, String estado) {
    return jdbc.queryForObject(
        """
        INSERT INTO servicio_publicado
            (id_prestador, id_subcategoria_servicio, nombre, descripcion, estado)
        VALUES (?, ?, ?, 'Descripción', ?)
        RETURNING id_servicio_publicado
        """,
        Long.class,
        idUsuario,
        idSubcategoria("Hogar y mantenimiento", "Plomería"),
        nombre,
        estado);
  }

  private List<String> subcategoriasDe(String categoria) {
    return jdbc.queryForList(
        """
        SELECT s.nombre FROM subcategoria_servicio s
        JOIN categoria_servicio c ON c.id_categoria_servicio = s.id_categoria_servicio
        WHERE c.nombre = ?
        ORDER BY s.nombre
        """,
        String.class,
        categoria);
  }

  private Short idCategoria(String nombre) {
    return jdbc.queryForObject(
        "SELECT id_categoria_servicio FROM categoria_servicio WHERE nombre = ?",
        Short.class,
        nombre);
  }

  private Integer idSubcategoria(String categoria, String nombre) {
    return jdbc.queryForObject(
        """
        SELECT s.id_subcategoria_servicio
        FROM subcategoria_servicio s
        JOIN categoria_servicio c ON c.id_categoria_servicio = s.id_categoria_servicio
        WHERE c.nombre = ? AND s.nombre = ?
        """,
        Integer.class,
        categoria,
        nombre);
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
}
