package com.moica.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.moica.PruebaDeIntegracionConPostgres;
import com.moica.usuario.service.AdministradorService;
import com.moica.usuario.service.AdministradorService.ResultadoDeAsignacion;
import com.moica.usuario.service.BootstrapDeAdministrador;
import com.moica.usuario.service.PropiedadesDeAdministracion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Asignación del rol administrativo al arrancar.
 *
 * <p>Es la única vía por la que existe un administrador en Moica, así que lo que importa es que sea
 * idempotente —se ejecuta en cada arranque— y que no invente nada: ni cuentas, ni roles sobre
 * correos que no corresponden a ninguna.
 */
class BootstrapDeAdministradorIT extends PruebaDeIntegracionConPostgres {

  private static final String CORREO = "administradora@moica.test";
  private static final String CLAVE_HASH = "$2a$10$hashDePruebaQueNoCorrespondeANadie";

  @Autowired private AdministradorService administradores;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DataSource origenDeDatos;

  @BeforeEach
  void limpiarCuentas() {
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
  }

  @Test
  void asignaElRolACuentaExistenteQueTodaviaNoLoTenia() {
    long idUsuario = registrarCuenta(CORREO);

    assertThat(administradores.asignarloA(CORREO)).isEqualTo(ResultadoDeAsignacion.ASIGNADO);
    assertThat(administradores.esAdministrador(idUsuario)).isTrue();
    assertThat(administradores.fechaDeAsignacion(idUsuario)).isPresent();
  }

  @Test
  void repetirloNoCambiaNadaNiFalla() {
    long idUsuario = registrarCuenta(CORREO);
    administradores.asignarloA(CORREO);
    OffsetDateTime fechaOriginal = administradores.fechaDeAsignacion(idUsuario).orElseThrow();

    assertThat(administradores.asignarloA(CORREO)).isEqualTo(ResultadoDeAsignacion.YA_LO_TENIA);
    assertThat(cantidadDeAdministradores()).isEqualTo(1);
    assertThat(administradores.fechaDeAsignacion(idUsuario)).contains(fechaOriginal);
  }

  @Test
  void noRegistraNadaCuandoElCorreoNoCorrespondeANingunaCuenta() {
    assertThat(administradores.asignarloA("nadie@moica.test"))
        .isEqualTo(ResultadoDeAsignacion.CUENTA_INEXISTENTE);
    assertThat(cantidadDeAdministradores()).isZero();
    assertThat(cantidadDeCuentas()).isZero();
  }

  @Test
  void encuentraLaCuentaAunqueElCorreoSeEscribaDeOtraForma() {
    long idUsuario = registrarCuenta(CORREO);

    assertThat(administradores.asignarloA("  Administradora@MOICA.test  "))
        .isEqualTo(ResultadoDeAsignacion.ASIGNADO);
    assertThat(administradores.esAdministrador(idUsuario)).isTrue();
  }

  @Test
  void elArranqueNoPromueveANadieSinLaVariableConfigurada() {
    registrarCuenta(CORREO);

    ejecutarArranqueCon("");
    ejecutarArranqueCon("   ");

    assertThat(cantidadDeAdministradores()).isZero();
  }

  @Test
  void elArranquePromueveLaCuentaIndicadaYPuedeRepetirse() {
    long idUsuario = registrarCuenta(CORREO);

    ejecutarArranqueCon(CORREO);
    ejecutarArranqueCon(CORREO);

    assertThat(administradores.esAdministrador(idUsuario)).isTrue();
    assertThat(cantidadDeAdministradores()).isEqualTo(1);
  }

  /**
   * Con dos réplicas, las dos ejecutan el arranque a la vez. La que llega segunda encuentra la fila
   * de la otra todavía sin confirmar; al confirmarse, no puede abortar su propio arranque.
   */
  @Test
  @Timeout(60)
  void otraReplicaQueLoConcedeALaVezNoHaceFallarEsteArranque() throws Exception {
    long idUsuario = registrarCuenta(CORREO);
    ExecutorService hilo = Executors.newSingleThreadExecutor();

    try (Connection otraReplica = origenDeDatos.getConnection()) {
      otraReplica.setAutoCommit(false);
      try (PreparedStatement insercion =
          otraReplica.prepareStatement("INSERT INTO administrador (id_administrador) VALUES (?)")) {
        insercion.setLong(1, idUsuario);
        insercion.executeUpdate();
      }

      Future<ResultadoDeAsignacion> esteArranque =
          hilo.submit(() -> administradores.asignarloA(CORREO));
      esperarAQueAlguienEspereUnBloqueo(jdbc);
      otraReplica.commit();

      assertThat(esteArranque.get()).isEqualTo(ResultadoDeAsignacion.YA_LO_TENIA);
    } finally {
      hilo.shutdownNow();
    }
    assertThat(cantidadDeAdministradores()).isEqualTo(1);
  }

  @Test
  void elArranqueNoSeDetieneCuandoLaCuentaIndicadaNoExiste() {
    assertThatCode(() -> ejecutarArranqueCon("todavia-no-existe@moica.test"))
        .doesNotThrowAnyException();
    assertThat(cantidadDeAdministradores()).isZero();
  }

  private void ejecutarArranqueCon(String correo) {
    new BootstrapDeAdministrador(new PropiedadesDeAdministracion(correo), administradores)
        .run(null);
  }

  private long registrarCuenta(String correo) {
    Long id =
        jdbc.queryForObject(
            """
            INSERT INTO usuario (nombre_completo, correo_electronico, clave_hash)
            VALUES (?, ?, ?)
            RETURNING id_usuario
            """,
            Long.class,
            "Persona Administradora",
            correo,
            CLAVE_HASH);
    return (id == null) ? 0L : id;
  }

  private Integer cantidadDeAdministradores() {
    return jdbc.queryForObject("SELECT count(*) FROM administrador", Integer.class);
  }

  private Integer cantidadDeCuentas() {
    return jdbc.queryForObject("SELECT count(*) FROM usuario", Integer.class);
  }
}
