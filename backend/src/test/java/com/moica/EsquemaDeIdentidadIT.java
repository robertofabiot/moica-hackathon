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
 * Comprueba que la migración {@code V10} impone en PostgreSQL las restricciones que exige el
 * diccionario de datos.
 *
 * <p>Se escribe SQL a propósito: lo que se está demostrando es la barrera de la base de datos, la
 * que sigue en pie aunque un día se equivoque el código de la aplicación.
 */
class EsquemaDeIdentidadIT extends PruebaDeIntegracionConPostgres {

  private static final String CLAVE_HASH_DE_PRUEBA = "$2a$10$hashDePruebaQueNoCorrespondeANadie";

  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void limpiarIdentidades() {
    jdbc.update("DELETE FROM sesion");
    jdbc.update("DELETE FROM usuario");
  }

  @Test
  void aplicaElValorPorOmisionDelEstadoDeCuenta() {
    jdbc.update(
        """
        INSERT INTO usuario (nombre_completo, correo_electronico, clave_hash)
        VALUES (?, ?, ?)
        """,
        "Persona de prueba",
        "estado@moica.test",
        CLAVE_HASH_DE_PRUEBA);

    String estado =
        jdbc.queryForObject(
            "SELECT estado_cuenta FROM usuario WHERE correo_electronico = ?",
            String.class,
            "estado@moica.test");

    assertThat(estado).isEqualTo("ACTIVA");
  }

  @Test
  void rechazaUnEstadoDeCuentaFueraDelDominio() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO usuario (nombre_completo, correo_electronico, clave_hash,
                                         estado_cuenta)
                    VALUES (?, ?, ?, ?)
                    """,
                    "Persona de prueba",
                    "dominio@moica.test",
                    CLAVE_HASH_DE_PRUEBA,
                    "VACACIONES"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rechazaDosCuentasConElMismoCorreo() {
    insertarUsuario("repetido@moica.test");

    assertThatThrownBy(() -> insertarUsuario("repetido@moica.test"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rechazaUnaSesionQueExpiraAntesDeEmpezar() {
    long idUsuario = insertarUsuario("vigencia@moica.test");
    OffsetDateTime ahora = OffsetDateTime.now();

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO sesion (id_usuario, identificador_token, fecha_inicio,
                                        fecha_expiracion)
                    VALUES (?, ?, ?, ?)
                    """,
                    idUsuario,
                    "token-imposible",
                    ahora,
                    ahora.minusMinutes(1)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rechazaUnaRevocacionSinMotivo() {
    long idSesion = insertarSesion(insertarUsuario("revocacion@moica.test"), "token-sin-motivo");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE sesion SET fecha_revocacion = ? WHERE id_sesion = ?",
                    OffsetDateTime.now(),
                    idSesion))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rechazaUnMotivoDeRevocacionSinFecha() {
    long idSesion = insertarSesion(insertarUsuario("motivo@moica.test"), "token-sin-fecha");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE sesion SET motivo_revocacion = ? WHERE id_sesion = ?",
                    "CIERRE_VOLUNTARIO",
                    idSesion))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rechazaUnMotivoDeRevocacionFueraDelDominio() {
    long idSesion = insertarSesion(insertarUsuario("inventado@moica.test"), "token-inventado");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    UPDATE sesion SET fecha_revocacion = ?, motivo_revocacion = ?
                    WHERE id_sesion = ?
                    """,
                    OffsetDateTime.now(),
                    "PORQUE_SI",
                    idSesion))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rechazaUnaSesionDeUnaCuentaInexistente() {
    assertThatThrownBy(() -> insertarSesion(999_999L, "token-huerfano"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void borraLasSesionesAlBorrarLaCuenta() {
    long idUsuario = insertarUsuario("cascada@moica.test");
    insertarSesion(idUsuario, "token-en-cascada");

    jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idUsuario);

    Integer sesiones =
        jdbc.queryForObject(
            "SELECT count(*) FROM sesion WHERE id_usuario = ?", Integer.class, idUsuario);
    assertThat(sesiones).isZero();
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

  private long insertarSesion(long idUsuario, String identificadorToken) {
    OffsetDateTime ahora = OffsetDateTime.now();
    Long id =
        jdbc.queryForObject(
            """
            INSERT INTO sesion (id_usuario, identificador_token, fecha_inicio, fecha_expiracion)
            VALUES (?, ?, ?, ?)
            RETURNING id_sesion
            """,
            Long.class,
            idUsuario,
            identificadorToken,
            ahora,
            ahora.plusDays(7));
    return (id == null) ? 0L : id;
  }
}
