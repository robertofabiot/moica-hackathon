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
 * Las migraciones {@code V20}–{@code V23} sobre PostgreSQL real.
 *
 * <p>Comprueba lo que el diccionario encarga a la base de datos: claves, unicidad, dominios,
 * valores por omisión, cascadas y la carga territorial de Managua. Las reglas que el diccionario
 * encarga a la aplicación se prueban en las pruebas de API.
 */
class EsquemaDeTerritorioYPerfilIT extends PruebaDeIntegracionConPostgres {

  @Autowired private JdbcTemplate jdbc;

  private Long idUsuario;

  @BeforeEach
  void prepararUnaCuenta() {
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
    jdbc.update(
        "DELETE FROM municipio WHERE id_departamento <> "
            + "(SELECT id_departamento FROM departamento WHERE nombre = 'Managua')");
    jdbc.update("DELETE FROM departamento WHERE nombre <> 'Managua'");

    idUsuario =
        jdbc.queryForObject(
            """
            INSERT INTO usuario (nombre_completo, correo_electronico, clave_hash)
            VALUES ('Persona de Prueba', 'esquema@moica.test',
                    '$2a$10$hashDePruebaQueNoCorrespondeANadie')
            RETURNING id_usuario
            """,
            Long.class);
  }

  @Test
  void managuaEstaHabilitadoConSusNueveMunicipios() {
    Boolean habilitado =
        jdbc.queryForObject(
            "SELECT habilitado FROM departamento WHERE nombre = 'Managua'", Boolean.class);
    assertThat(habilitado).isTrue();

    List<String> municipios =
        jdbc.queryForList(
            """
            SELECT m.nombre FROM municipio m
            JOIN departamento d ON d.id_departamento = m.id_departamento
            WHERE d.nombre = 'Managua'
            ORDER BY m.nombre
            """,
            String.class);

    assertThat(municipios)
        .containsExactly(
            "Ciudad Sandino",
            "El Crucero",
            "Managua",
            "Mateare",
            "San Francisco Libre",
            "San Rafael del Sur",
            "Ticuantepe",
            "Tipitapa",
            "Villa El Carmen");
  }

  @Test
  void unMunicipioNoSeRepiteDentroDelMismoDepartamento() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO municipio (id_departamento, nombre)
                    SELECT id_departamento, 'Managua' FROM departamento WHERE nombre = 'Managua'
                    """))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elMismoNombreDeMunicipioSiCabeEnOtroDepartamento() {
    jdbc.update("INSERT INTO departamento (nombre, habilitado) VALUES ('Rivas', FALSE)");

    assertThat(
            jdbc.update(
                """
                INSERT INTO municipio (id_departamento, nombre)
                SELECT id_departamento, 'Managua' FROM departamento WHERE nombre = 'Rivas'
                """))
        .isEqualTo(1);
  }

  @Test
  void elPerfilNaceDisponibleYSinVerificarPorOmision() {
    insertarPerfil(idUsuario);

    assertThat(
            jdbc.queryForMap(
                "SELECT disponibilidad, nivel_verificacion FROM perfil_prestador"
                    + " WHERE id_prestador = ?",
                idUsuario))
        .containsEntry("disponibilidad", "DISPONIBLE")
        .containsEntry("nivel_verificacion", "SIN_VERIFICAR");
  }

  @Test
  void laClaveCompartidaImpideUnSegundoPerfilParaLaMismaCuenta() {
    insertarPerfil(idUsuario);

    assertThatThrownBy(() -> insertarPerfil(idUsuario))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elDominioDelTipoDePrestadorSeImponeEnLaBase() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO perfil_prestador
                        (id_prestador, nombre_publico, descripcion, tipo_prestador,
                         id_municipio_principal, descripcion_cobertura)
                    VALUES (?, 'Nombre', 'Descripción', 'COOPERATIVA', ?, 'Cobertura')
                    """,
                    idUsuario,
                    idMunicipioManagua()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void unPerfilNoPuedeApuntarAUnMunicipioInexistente() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO perfil_prestador
                        (id_prestador, nombre_publico, descripcion, tipo_prestador,
                         id_municipio_principal, descripcion_cobertura)
                    VALUES (?, 'Nombre', 'Descripción', 'PYME', 999999, 'Cobertura')
                    """,
                    idUsuario))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void borrarLaCuentaArrastraPerfilContactosTrabajosEImagenes() {
    insertarPerfil(idUsuario);
    jdbc.update(
        "INSERT INTO medio_contacto_prestador (id_prestador, contenido) VALUES (?, '8888-8888')",
        idUsuario);
    Long idTrabajo =
        jdbc.queryForObject(
            """
            INSERT INTO trabajo_portafolio (id_prestador, titulo, descripcion)
            VALUES (?, 'Instalación', 'Instalación completa') RETURNING id_trabajo
            """,
            Long.class,
            idUsuario);
    jdbc.update(
        "INSERT INTO imagen_trabajo_portafolio (id_trabajo, url_imagen)"
            + " VALUES (?, 'https://imagenes.moica.test/trabajos/x.png')",
        idTrabajo);

    jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idUsuario);

    assertThat(contar("perfil_prestador")).isZero();
    assertThat(contar("medio_contacto_prestador")).isZero();
    assertThat(contar("trabajo_portafolio")).isZero();
    assertThat(contar("imagen_trabajo_portafolio")).isZero();
  }

  @Test
  void elTerritorioNoSePuedeBorrarMientrasUnPerfilLoReferencie() {
    insertarPerfil(idUsuario);

    assertThatThrownBy(
            () -> jdbc.update("DELETE FROM municipio WHERE id_municipio = ?", idMunicipioManagua()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void lasColumnasDeImagenGuardanUnaUrlNuncaUnBinario() {
    List<String> tiposDeColumna =
        jdbc.queryForList(
            """
            SELECT data_type FROM information_schema.columns
            WHERE (table_name = 'perfil_prestador' AND column_name = 'url_imagen_perfil')
               OR (table_name = 'imagen_trabajo_portafolio' AND column_name = 'url_imagen')
            """,
            String.class);

    assertThat(tiposDeColumna)
        .as("el diccionario persiste la URL pública; el binario vive en el almacén de objetos")
        .hasSize(2)
        .allSatisfy(tipo -> assertThat(tipo).isEqualTo("character varying"));
  }

  private void insertarPerfil(Long idPrestador) {
    jdbc.update(
        """
        INSERT INTO perfil_prestador
            (id_prestador, nombre_publico, descripcion, tipo_prestador,
             id_municipio_principal, descripcion_cobertura)
        VALUES (?, 'Taller de Prueba', 'Descripción de prueba', 'INDEPENDIENTE', ?, 'Cobertura')
        """,
        idPrestador,
        idMunicipioManagua());
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

  private Integer contar(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
  }
}
