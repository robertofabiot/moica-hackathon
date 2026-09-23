package com.moica.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moica.PruebaDeIntegracionConPostgres;
import com.moica.comun.almacenamiento.PropiedadesDeAlmacenamiento;
import com.moica.demo.repository.DemoRepository;
import com.moica.demo.service.DemoService;
import com.moica.servicio.service.DescubrimientoDeServiciosService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** PostgreSQL real; los cambios de cada escenario se revierten sin tocar el catálogo compartido. */
@Transactional
@TestPropertySource(properties = "MOICA_SEED_DEMO_ENABLED=false")
class DemoIT extends PruebaDeIntegracionConPostgres {
  // Claves sintéticas exclusivamente para comprobar relaciones, sin llamadas a R2.
  private static final String IMAGEN = "servicios/0123456789abcdef0123456789abcdef.jpg";
  private static final String OTRA_IMAGEN = "servicios/fedcba9876543210fedcba9876543210.png";
  private static final PropiedadesDeAlmacenamiento ALMACENAMIENTO =
      new PropiedadesDeAlmacenamiento(
          "prueba", "prueba", "prueba", "prueba", "https://imagenes.example.test");

  @Autowired private JdbcTemplate jdbc;
  @Autowired private DemoRepository repositorio;
  @Autowired private PasswordEncoder codificador;
  @Autowired private DemoService deshabilitado;
  @Autowired private PropiedadesDeDemo propiedades;
  @Autowired private DescubrimientoDeServiciosService descubrimiento;
  @Autowired private PlatformTransactionManager transacciones;

  private DemoService demo;

  @BeforeEach
  void preparar() {
    Map<String, List<String>> mapeo =
        DatosDeDemostracion.SERVICIOS.stream()
            .collect(
                Collectors.toMap(
                    DatosDeDemostracion.Servicio::clave, s -> List.of(IMAGEN, OTRA_IMAGEN)));
    demo = servicio(new PropiedadesDeDemo(true, mapeo));
  }

  private DemoService servicio(PropiedadesDeDemo configuracion) {
    return new DemoService(repositorio, codificador, configuracion, ALMACENAMIENTO);
  }

  @Test
  void deshabilitadoNoEscribeAunqueSeLlameDirectamente() {
    assertThat(propiedades.enabled()).isFalse();
    var antes = instantanea();
    assertThat(deshabilitado.sincronizar())
        .isEqualTo(new DemoService.Resumen(0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(instantanea()).isEqualTo(antes);
    assertThat(usuarios()).isEmpty();
  }

  @Test
  void primeraEjecucionCreaRelacionesEstadosYNueveSubcategorias() {
    assertThat(demo.sincronizar()).isEqualTo(new DemoService.Resumen(6, 6, 9, 18, 0, 0, 0, 0, 0));
    assertThat(usuarios()).hasSize(6);
    assertThat(
            jdbc.queryForList(
                """
        SELECT DISTINCT sc.nombre FROM subcategoria_servicio sc
        JOIN servicio_publicado s USING (id_subcategoria_servicio)
        JOIN usuario u ON u.id_usuario = s.id_prestador
        WHERE u.correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid'
        """,
                String.class))
        .containsExactlyInAnyOrderElementsOf(
            DatosDeDemostracion.SERVICIOS.stream()
                .map(DatosDeDemostracion.Servicio::subcategoria)
                .toList());
    assertThat(
            jdbc.queryForObject(
                """
        SELECT count(*) FROM imagen_servicio_publicado i
        JOIN servicio_publicado s USING (id_servicio_publicado)
        JOIN perfil_prestador p ON p.id_prestador = s.id_prestador
        JOIN usuario u ON u.id_usuario = p.id_prestador
        JOIN municipio m ON m.id_municipio = p.id_municipio_principal
        JOIN departamento d USING (id_departamento)
        WHERE u.correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid'
          AND u.estado_cuenta = 'ACTIVA' AND p.disponibilidad = 'DISPONIBLE'
          AND p.nivel_verificacion IN ('VERIFICADO_BASICO', 'PROFESIONAL_VERIFICADO')
          AND s.estado = 'ACTIVO' AND d.nombre = 'Managua'
          AND i.texto_alternativo IS NOT NULL AND i.orden_visualizacion IN (0,1)
        """,
                Integer.class))
        .isEqualTo(18);
    assertThat(
            jdbc.queryForObject(
                """
        SELECT count(*) FROM perfil_prestador p JOIN usuario u ON u.id_usuario = p.id_prestador
        WHERE u.correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid'
          AND p.nivel_verificacion = 'PROFESIONAL_VERIFICADO'
        """,
                Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                """
        SELECT count(*) FROM solicitud_verificacion_prestador v JOIN usuario u ON u.id_usuario = v.id_prestador
        WHERE u.correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid'
        """,
                Integer.class))
        .isZero();
  }

  @Test
  void segundaEjecucionConservaTodosLosIdsHashesYFechas() {
    demo.sincronizar();
    var antes = instantanea();
    assertThat(demo.sincronizar()).isEqualTo(new DemoService.Resumen(0, 0, 0, 0, 6, 6, 9, 18, 0));
    assertThat(instantanea()).isEqualTo(antes);
  }

  @Test
  void completaDatosParcialesYSincronizaCamposSinReemplazarIds() {
    demo.sincronizar();
    Long julio = repositorio.usuarios(DatosDeDemostracion.PRESTADORES.getFirst()).getFirst();
    var plomeria = DatosDeDemostracion.SERVICIOS.getFirst();
    Long servicio = repositorio.servicios(julio, repositorio.subcategoria(plomeria)).getFirst();
    jdbc.update("DELETE FROM imagen_servicio_publicado WHERE id_servicio_publicado = ?", servicio);
    jdbc.update(
        "UPDATE servicio_publicado SET nombre = 'Edición demo', estado = 'INACTIVO' WHERE id_servicio_publicado = ?",
        servicio);
    jdbc.update(
        "UPDATE perfil_prestador SET disponibilidad = 'NO_DISPONIBLE', nivel_verificacion = 'SIN_VERIFICAR' WHERE id_prestador = ?",
        julio);
    var sinPerfil = DatosDeDemostracion.PRESTADORES.get(1);
    Long otro = repositorio.usuarios(sinPerfil).getFirst();
    jdbc.update("DELETE FROM servicio_publicado WHERE id_prestador = ?", otro);
    jdbc.update("DELETE FROM perfil_prestador WHERE id_prestador = ?", otro);
    assertThat(demo.sincronizar()).isEqualTo(new DemoService.Resumen(0, 1, 1, 4, 6, 5, 8, 14, 0));
    assertThat(repositorio.servicios(julio, repositorio.subcategoria(plomeria)))
        .containsExactly(servicio);
    assertThat(descubrimiento.detallar(servicio).nombre()).isEqualTo(plomeria.nombre());
    var completa = instantanea();
    demo.sincronizar();
    assertThat(instantanea()).isEqualTo(completa);
  }

  @Test
  void consultasPublicasFiltrosDetallesYPerfilesVenElContenido() {
    demo.sincronizar();
    var lista = descubrimiento.buscar("ficticio para demostración", null, null, null);
    assertThat(lista).hasSize(9);
    assertThat(lista.stream().filter(s -> s.precioReferencia() == null)).hasSize(3);
    for (var s : lista) {
      var detalle = descubrimiento.detallar(s.idServicioPublicado());
      assertThat(detalle.imagenes()).hasSize(2);
      assertThat(detalle.admiteContratacion()).isTrue();
      assertThat(
              descubrimiento.buscar(
                  null,
                  detalle.idCategoriaServicio(),
                  detalle.idSubcategoriaServicio(),
                  detalle.prestador().municipioPrincipal().idMunicipio()))
          .hasSize(1);
      assertThat(descubrimiento.perfilPublico(detalle.prestador().idPrestador()).servicios())
          .isNotEmpty();
    }
    Long julio = repositorio.usuarios(DatosDeDemostracion.PRESTADORES.getFirst()).getFirst();
    assertThat(descubrimiento.perfilPublico(julio).servicios()).hasSize(2);
    jdbc.update(
        "UPDATE perfil_prestador SET disponibilidad = 'NO_DISPONIBLE' WHERE id_prestador = ?",
        julio);
    assertThat(descubrimiento.buscar("ficticio para demostración", null, null, null)).hasSize(7);
  }

  @Test
  void contrasenasSonHashesBcryptDistintosYNoSeRegeneran() {
    demo.sincronizar();
    List<String> hashes = usuarios().stream().map(u -> (String) u.get("clave_hash")).toList();
    assertThat(hashes)
        .doesNotHaveDuplicates()
        .allSatisfy(
            hash -> {
              assertThat(hash).matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");
              assertThat(codificador.matches("", hash)).isFalse();
              assertThat(codificador.matches("Moica123!", hash)).isFalse();
            });
    demo.sincronizar();
    assertThat(usuarios().stream().map(u -> (String) u.get("clave_hash")).toList())
        .isEqualTo(hashes);
  }

  @Test
  void noModificaUnaCuentaRealNiSuPerfilServicioOImagen() {
    var p = DatosDeDemostracion.PRESTADORES.getFirst();
    Long id =
        jdbc.queryForObject(
            """
        INSERT INTO usuario(nombre_completo, correo_electronico, clave_hash, estado_cuenta)
        VALUES ('Cuenta ajena', 'ajena-seed@example.test', ?, 'SUSPENDIDA_PERMANENTE') RETURNING id_usuario
        """,
            Long.class,
            codificador.encode(java.util.UUID.randomUUID().toString()));
    repositorio.sincronizarPerfil(id, p, repositorio.municipio("Managua"));
    var s = DatosDeDemostracion.SERVICIOS.getFirst();
    Long publicado = repositorio.crearServicio(id, repositorio.subcategoria(s), s);
    repositorio.crearImagen(publicado, "https://imagenes.example.test/real.jpg", "Imagen ajena", 0);
    var usuario = jdbc.queryForMap("SELECT * FROM usuario WHERE id_usuario = ?", id);
    var perfil = jdbc.queryForMap("SELECT * FROM perfil_prestador WHERE id_prestador = ?", id);
    var servicio =
        jdbc.queryForMap(
            "SELECT * FROM servicio_publicado WHERE id_servicio_publicado = ?", publicado);
    var imagen =
        jdbc.queryForMap(
            "SELECT * FROM imagen_servicio_publicado WHERE id_servicio_publicado = ?", publicado);
    demo.sincronizar();
    demo.sincronizar();
    assertThat(jdbc.queryForMap("SELECT * FROM usuario WHERE id_usuario = ?", id))
        .isEqualTo(usuario);
    assertThat(jdbc.queryForMap("SELECT * FROM perfil_prestador WHERE id_prestador = ?", id))
        .isEqualTo(perfil);
    assertThat(
            jdbc.queryForMap(
                "SELECT * FROM servicio_publicado WHERE id_servicio_publicado = ?", publicado))
        .isEqualTo(servicio);
    assertThat(
            jdbc.queryForMap(
                "SELECT * FROM imagen_servicio_publicado WHERE id_servicio_publicado = ?",
                publicado))
        .isEqualTo(imagen);
  }

  @Test
  void sinMapeoCreaTodoYSuAusenciaPosteriorConservaImagenes() {
    DemoService sinImagenes = servicio(new PropiedadesDeDemo(true, Map.of()));
    assertThat(sinImagenes.sincronizar())
        .isEqualTo(new DemoService.Resumen(6, 6, 9, 0, 0, 0, 0, 0, 9));
    assertThat(demo.sincronizar().imagenesCreadas()).isEqualTo(18);
    var antes = instantanea();
    sinImagenes.sincronizar();
    assertThat(instantanea()).isEqualTo(antes);
  }

  @Test
  void bucketComprobadoReutilizaDosImagenesExistentesSinMapeoAdicional() {
    var r2 =
        new PropiedadesDeAlmacenamiento(
            "prueba", "prueba", "prueba", "prueba", ImagenesDeDemostracion.BASE_COMPROBADA);
    var recuperadas =
        new DemoService(repositorio, codificador, new PropiedadesDeDemo(true, Map.of()), r2);
    assertThat(recuperadas.sincronizar())
        .isEqualTo(new DemoService.Resumen(6, 6, 9, 2, 0, 0, 0, 0, 8));
    var imagenes =
        jdbc.queryForList(
            """
        SELECT i.url_imagen FROM imagen_servicio_publicado i
        JOIN servicio_publicado s USING (id_servicio_publicado)
        JOIN usuario u ON u.id_usuario = s.id_prestador
        WHERE u.correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid'
        ORDER BY i.orden_visualizacion
        """,
            String.class);
    assertThat(imagenes)
        .containsExactly(
            r2.urlPublicaDe(ImagenesDeDemostracion.LAPTOP),
            r2.urlPublicaDe(ImagenesDeDemostracion.PROCESADOR));
    var antes = instantanea();
    assertThat(recuperadas.sincronizar().imagenesCreadas()).isZero();
    assertThat(instantanea()).isEqualTo(antes);
  }

  @Test
  void rechazaUrlsFirmadasClavesPrivadasDuplicadasYMasDeTresImagenesSinEscribir() {
    for (var claves :
        List.of(
            List.of("https://privado.example.test/objeto?firma=prueba"),
            List.of("expedientes/0123456789abcdef0123456789abcdef.jpg"),
            List.of(IMAGEN, IMAGEN),
            List.of(IMAGEN, OTRA_IMAGEN, IMAGEN, OTRA_IMAGEN))) {
      var antes = instantanea();
      assertThatThrownBy(
              () -> servicio(new PropiedadesDeDemo(true, Map.of("plomeria", claves))).sincronizar())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageNotContaining("firma=prueba");
      assertThat(instantanea()).isEqualTo(antes);
    }
  }

  @Test
  void resuelveCatalogosConIdentificadoresDiferentesSinReiniciarSecuencias() {
    // Las filas originales se conservan con otro nombre; nuevas filas obtienen IDENTITY nuevo.
    // Todo revierte al acabar la prueba, incluidos los nombres originales.
    for (var p :
        DatosDeDemostracion.PRESTADORES.stream()
            .map(DatosDeDemostracion.Prestador::municipio)
            .distinct()
            .toList()) {
      Integer antiguo = repositorio.municipio(p);
      jdbc.update(
          "UPDATE municipio SET nombre = nombre || ' original seed' WHERE id_municipio = ?",
          antiguo);
      jdbc.update(
          "INSERT INTO municipio(id_departamento, nombre) SELECT id_departamento, ? FROM municipio WHERE id_municipio = ?",
          p,
          antiguo);
      assertThat(repositorio.municipio(p)).isNotEqualTo(antiguo);
    }
    for (String categoria :
        DatosDeDemostracion.SERVICIOS.stream()
            .map(DatosDeDemostracion.Servicio::categoria)
            .distinct()
            .toList()) {
      jdbc.update(
          "UPDATE categoria_servicio SET nombre = nombre || ' original seed' WHERE nombre = ?",
          categoria);
      jdbc.update("INSERT INTO categoria_servicio(nombre) VALUES (?)", categoria);
    }
    for (var s : DatosDeDemostracion.SERVICIOS) {
      jdbc.update(
          "INSERT INTO subcategoria_servicio(id_categoria_servicio, nombre) SELECT id_categoria_servicio, ? FROM categoria_servicio WHERE nombre = ?",
          s.subcategoria(),
          s.categoria());
    }
    assertThat(demo.sincronizar().serviciosCreados()).isEqualTo(9);
    for (var s : DatosDeDemostracion.SERVICIOS) {
      var prestador =
          DatosDeDemostracion.PRESTADORES.stream()
              .filter(p -> p.clave().equals(s.prestador()))
              .findFirst()
              .orElseThrow();
      assertThat(
              repositorio.servicios(
                  repositorio.usuarios(prestador).getFirst(), repositorio.subcategoria(s)))
          .hasSize(1);
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void conflictoDePropiedadRevierteLaTransaccionCompleta() {
    var p = DatosDeDemostracion.PRESTADORES.getLast();
    Long id =
        jdbc.queryForObject(
            "INSERT INTO usuario(nombre_completo, correo_electronico, clave_hash) VALUES ('Ajeno', ?, ?) RETURNING id_usuario",
            Long.class,
            p.correo(),
            codificador.encode(java.util.UUID.randomUUID().toString()));
    try {
      var antes = instantanea();
      assertThatThrownBy(
              () -> new TransactionTemplate(transacciones).execute(s -> demo.sincronizar()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Conflicto de propiedad");
      assertThat(instantanea()).isEqualTo(antes);
    } finally {
      jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", id);
    }
  }

  private List<Map<String, Object>> usuarios() {
    return jdbc.queryForList(
        "SELECT * FROM usuario WHERE correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid' ORDER BY id_usuario");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void dosArranquesConcurrentesCreanUnaSolaCopia() throws Exception {
    var inicio = new java.util.concurrent.CountDownLatch(1);
    try (var ejecutor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<DemoService.Resumen> tarea =
          () -> {
            inicio.await();
            return new TransactionTemplate(transacciones).execute(s -> demo.sincronizar());
          };
      var primero = ejecutor.submit(tarea);
      var segundo = ejecutor.submit(tarea);
      inicio.countDown();
      var uno = primero.get(30, java.util.concurrent.TimeUnit.SECONDS);
      var dos = segundo.get(30, java.util.concurrent.TimeUnit.SECONDS);
      assertThat(uno.usuariosCreados() + dos.usuariosCreados()).isEqualTo(6);
      assertThat(uno.serviciosCreados() + dos.serviciosCreados()).isEqualTo(9);
      assertThat(uno.imagenesCreadas() + dos.imagenesCreadas()).isEqualTo(18);
      assertThat(usuarios()).hasSize(6);
    } finally {
      new TransactionTemplate(transacciones)
          .executeWithoutResult(
              s -> {
                for (var p : DatosDeDemostracion.PRESTADORES) {
                  for (Long id : repositorio.usuarios(p)) {
                    jdbc.update("DELETE FROM servicio_publicado WHERE id_prestador = ?", id);
                    jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", id);
                  }
                }
              });
    }
  }

  private List<List<Map<String, Object>>> instantanea() {
    return List.of(
        usuarios(),
        jdbc.queryForList("SELECT * FROM perfil_prestador ORDER BY id_prestador"),
        jdbc.queryForList("SELECT * FROM servicio_publicado ORDER BY id_servicio_publicado"),
        jdbc.queryForList(
            "SELECT * FROM imagen_servicio_publicado ORDER BY id_imagen_servicio_publicado"));
  }
}
