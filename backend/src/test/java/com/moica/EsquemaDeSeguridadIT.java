package com.moica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Comprueba que la migración {@code V11} impone en PostgreSQL las restricciones que exige el
 * diccionario de datos para el rol administrativo y el segundo factor.
 *
 * <p>Se escribe SQL a propósito: lo que se está demostrando es la barrera de la base de datos, la
 * que sigue en pie aunque un día se equivoque el código de la aplicación.
 */
class EsquemaDeSeguridadIT extends PruebaDeIntegracionConPostgres {

  private static final String CLAVE_HASH_DE_PRUEBA = "$2a$10$hashDePruebaQueNoCorrespondeANadie";
  private static final String SECRETO_CIFRADO = "c2VjcmV0by1jaWZyYWRvLWRlLXBydWViYQ==";

  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void limpiarIdentidades() {
    jdbc.update("DELETE FROM sesion");
    jdbc.update("DELETE FROM solicitud_verificacion_prestador");
    jdbc.update("DELETE FROM imagen_servicio_publicado");
    jdbc.update("DELETE FROM servicio_publicado");
    jdbc.update("DELETE FROM usuario");
  }

  @Test
  void guardaElRolAdministrativoConSuFechaPorOmision() {
    long idUsuario = insertarUsuario("admin@moica.test");

    jdbc.update("INSERT INTO administrador (id_administrador) VALUES (?)", idUsuario);

    assertThat(
            jdbc.queryForObject(
                "SELECT fecha_asignacion FROM administrador WHERE id_administrador = ?",
                OffsetDateTime.class,
                idUsuario))
        .isNotNull();
  }

  @Test
  void impideQueUnaCuentaTengaDosVecesElRolAdministrativo() {
    long idUsuario = insertarUsuario("repetido-admin@moica.test");
    jdbc.update("INSERT INTO administrador (id_administrador) VALUES (?)", idUsuario);

    assertThatThrownBy(
            () -> jdbc.update("INSERT INTO administrador (id_administrador) VALUES (?)", idUsuario))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rechazaUnAdministradorSinCuenta() {
    assertThatThrownBy(
            () -> jdbc.update("INSERT INTO administrador (id_administrador) VALUES (?)", 999_999L))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void retiraElRolAdministrativoAlBorrarLaCuenta() {
    long idUsuario = insertarUsuario("cascada-admin@moica.test");
    jdbc.update("INSERT INTO administrador (id_administrador) VALUES (?)", idUsuario);

    jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idUsuario);

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM administrador WHERE id_administrador = ?",
                Integer.class,
                idUsuario))
        .isZero();
  }

  @Test
  void unSegundoFactorNaceEnPendienteDeActivacion() {
    long idUsuario = insertarUsuario("pendiente@moica.test");

    insertarSegundoFactor(idUsuario, null, null);

    assertThat(estadoDelSegundoFactor(idUsuario)).isEqualTo("PENDIENTE_ACTIVACION");
  }

  @Test
  void impideQueUnaCuentaRegistreDosSegundosFactores() {
    long idUsuario = insertarUsuario("dos-factores@moica.test");
    insertarSegundoFactor(idUsuario, null, null);

    assertThatThrownBy(() -> insertarSegundoFactor(idUsuario, null, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rechazaUnEstadoDeSegundoFactorFueraDelDominio() {
    long idUsuario = insertarUsuario("estado-inventado@moica.test");

    assertThatThrownBy(() -> insertarSegundoFactor(idUsuario, "CASI_ACTIVO", null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rechazaUnSegundoFactorActivoSinFechaDeActivacion() {
    long idUsuario = insertarUsuario("activo-sin-fecha@moica.test");

    assertThatThrownBy(() -> insertarSegundoFactor(idUsuario, "ACTIVO", null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void aceptaUnSegundoFactorActivoConSuFechaDeActivacion() {
    long idUsuario = insertarUsuario("activo-con-fecha@moica.test");

    insertarSegundoFactor(idUsuario, "ACTIVO", OffsetDateTime.now());

    assertThat(estadoDelSegundoFactor(idUsuario)).isEqualTo("ACTIVO");
  }

  @Test
  void conservaLaFechaDeActivacionAlDesactivarlo() {
    long idUsuario = insertarUsuario("desactivado@moica.test");
    OffsetDateTime activacion = OffsetDateTime.now();
    insertarSegundoFactor(idUsuario, "ACTIVO", activacion);

    jdbc.update(
        "UPDATE segundo_factor_usuario SET estado_segundo_factor = 'DESACTIVADO' WHERE id_usuario = ?",
        idUsuario);

    assertThat(estadoDelSegundoFactor(idUsuario)).isEqualTo("DESACTIVADO");
    assertThat(
            jdbc.queryForObject(
                "SELECT fecha_activacion FROM segundo_factor_usuario WHERE id_usuario = ?",
                OffsetDateTime.class,
                idUsuario))
        .as("la restricción solo exige la fecha mientras el estado sea ACTIVO")
        .isNotNull();
  }

  @Test
  void rechazaUnSegundoFactorDeUnaCuentaInexistente() {
    assertThatThrownBy(() -> insertarSegundoFactor(999_999L, null, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void borraElSegundoFactorAlBorrarLaCuenta() {
    long idUsuario = insertarUsuario("cascada-2fa@moica.test");
    insertarSegundoFactor(idUsuario, null, null);

    jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idUsuario);

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM segundo_factor_usuario WHERE id_usuario = ?",
                Integer.class,
                idUsuario))
        .isZero();
  }

  @Test
  void existeElIndiceQuePermiteRevocarTodasLasSesionesDeUnaCuenta() {
    assertThat(
            jdbc.queryForObject(
                """
                SELECT count(*) FROM pg_indexes
                WHERE tablename = 'sesion' AND indexname = 'ix_sesion_id_usuario'
                """,
                Integer.class))
        .as("sin él, revocar las sesiones de una cuenta recorrería la tabla entera")
        .isEqualTo(1);
  }

  private long insertarUsuario(String correo) {
    Long id =
        jdbc.queryForObject(
            """
            INSERT INTO usuario (nombre_completo, correo_electronico, clave_hash)
            VALUES (?, ?, ?)
            RETURNING id_usuario
            """,
            Long.class,
            "Persona de prueba",
            correo,
            CLAVE_HASH_DE_PRUEBA);
    return (id == null) ? 0L : id;
  }

  private void insertarSegundoFactor(
      long idUsuario, String estado, OffsetDateTime fechaActivacion) {

    if (estado == null) {
      jdbc.update(
          "INSERT INTO segundo_factor_usuario (id_usuario, secreto_totp) VALUES (?, ?)",
          idUsuario,
          SECRETO_CIFRADO);
      return;
    }

    jdbc.update(
        """
        INSERT INTO segundo_factor_usuario (id_usuario, secreto_totp, estado_segundo_factor,
                                            fecha_activacion)
        VALUES (?, ?, ?, ?)
        """,
        idUsuario,
        SECRETO_CIFRADO,
        estado,
        fechaActivacion);
  }

  private String estadoDelSegundoFactor(long idUsuario) {
    return jdbc.queryForObject(
        "SELECT estado_segundo_factor FROM segundo_factor_usuario WHERE id_usuario = ?",
        String.class,
        idUsuario);
  }
}
