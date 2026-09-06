package com.moica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Las migraciones {@code V50} y {@code V51} sobre PostgreSQL real.
 *
 * <p>Comprueba las tres tablas de moderación, sus tipos, su nulabilidad, sus claves y las
 * restricciones que la base sostiene por sí sola: los dominios controlados, que cada participante
 * abra como máximo un caso por solicitud, que nadie se reporte a sí mismo, la coherencia del cierre
 * y de la vigencia, y la exclusión temporal que impide que dos versiones SCD2 del mismo caso se
 * superpongan.
 *
 * <p>Las reglas que dependen de otras tablas —que la solicitud llegara a estar aceptada y que quien
 * reporta participe en ella— se prueban en la API, en {@code ReporteDeParticipanteIT}.
 */
class EsquemaDeCasosDeModeracionIT extends PruebaDeIntegracionConPostgres {

  @Autowired private JdbcTemplate jdbc;

  private Long idReportante;
  private Long idReportado;
  private Long idTercero;
  private Long idSolicitud;

  @BeforeEach
  void prepararSolicitudAceptada() {
    jdbc.update("DELETE FROM historial_caso");
    jdbc.update("DELETE FROM caso_moderacion");
    jdbc.update("DELETE FROM medida_administrativa");
    jdbc.update("DELETE FROM calificacion_usuario");
    jdbc.update("DELETE FROM mensaje_solicitud");
    jdbc.update("DELETE FROM cambio_estado_solicitud");
    jdbc.update("DELETE FROM solicitud_servicio");
    jdbc.update("DELETE FROM solicitud_verificacion_prestador");
    jdbc.update("DELETE FROM imagen_servicio_publicado");
    jdbc.update("DELETE FROM servicio_publicado");
    jdbc.update("DELETE FROM usuario");

    idReportante = insertarUsuario("Cliente de Prueba", "moderacion.cliente@moica.test");
    idReportado = insertarUsuario("Prestador de Prueba", "moderacion.prestador@moica.test");
    idTercero = insertarUsuario("Tercero de Prueba", "moderacion.tercero@moica.test");

    jdbc.update(
        """
        INSERT INTO perfil_prestador
            (id_prestador, nombre_publico, descripcion, tipo_prestador,
             id_municipio_principal, descripcion_cobertura)
        VALUES (?, 'Taller de Prueba', 'Descripción', 'INDEPENDIENTE', ?, 'Cobertura')
        """,
        idReportado,
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
            idReportado,
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
            idReportante,
            idServicio,
            idMunicipioManagua());
  }

  // --- Estructura ---------------------------------------------------------

  @Test
  void lasTresTablasDeModeracionExisten() {
    assertThat(tablasDelEsquema())
        .contains("medida_administrativa", "caso_moderacion", "historial_caso");
  }

  @Test
  void losTiposDelCasoSonLosDelDiccionario() {
    assertThat(tipoDeColumna("caso_moderacion", "id_caso_moderacion")).isEqualTo("bigint");
    assertThat(tipoDeColumna("caso_moderacion", "id_solicitud_servicio")).isEqualTo("bigint");
    assertThat(tipoDeColumna("caso_moderacion", "id_reportante")).isEqualTo("bigint");
    assertThat(tipoDeColumna("caso_moderacion", "id_reportado")).isEqualTo("bigint");
    assertThat(tipoDeColumna("caso_moderacion", "id_administrador_responsable"))
        .isEqualTo("bigint");
    assertThat(tipoDeColumna("caso_moderacion", "id_medida_administrativa_actual"))
        .isEqualTo("smallint");
    assertThat(tipoDeColumna("caso_moderacion", "motivo")).isEqualTo("character varying");
    assertThat(longitudDeColumna("caso_moderacion", "motivo")).isEqualTo(120);
    assertThat(tipoDeColumna("caso_moderacion", "descripcion")).isEqualTo("text");
    assertThat(tipoDeColumna("caso_moderacion", "estado_actual")).isEqualTo("character varying");
    assertThat(tipoDeColumna("caso_moderacion", "resultado_actual")).isEqualTo("character varying");
    assertThat(tipoDeColumna("caso_moderacion", "resolucion_actual")).isEqualTo("text");
    assertThat(tipoDeColumna("caso_moderacion", "fecha_fin_medida_actual"))
        .isEqualTo("timestamp with time zone");
    assertThat(tipoDeColumna("caso_moderacion", "fecha_apertura"))
        .isEqualTo("timestamp with time zone");
    assertThat(tipoDeColumna("caso_moderacion", "fecha_cierre_actual"))
        .isEqualTo("timestamp with time zone");
    assertThat(tipoDeColumna("caso_moderacion", "fecha_actualizacion"))
        .isEqualTo("timestamp with time zone");
    assertThat(esIdentidad("caso_moderacion", "id_caso_moderacion")).isEqualTo("ALWAYS");
  }

  @Test
  void losTiposDelHistorialSonLosDelDiccionario() {
    assertThat(tipoDeColumna("historial_caso", "id_historial_caso")).isEqualTo("bigint");
    assertThat(tipoDeColumna("historial_caso", "id_caso_moderacion")).isEqualTo("bigint");
    assertThat(tipoDeColumna("historial_caso", "id_usuario_afectado")).isEqualTo("bigint");
    assertThat(tipoDeColumna("historial_caso", "id_actor")).isEqualTo("bigint");
    assertThat(tipoDeColumna("historial_caso", "id_administrador_responsable")).isEqualTo("bigint");
    assertThat(tipoDeColumna("historial_caso", "id_medida_administrativa")).isEqualTo("smallint");
    assertThat(tipoDeColumna("historial_caso", "numero_version")).isEqualTo("integer");
    assertThat(tipoDeColumna("historial_caso", "tipo_actor")).isEqualTo("character varying");
    assertThat(tipoDeColumna("historial_caso", "tipo_evento")).isEqualTo("character varying");
    assertThat(tipoDeColumna("historial_caso", "estado_caso")).isEqualTo("character varying");
    assertThat(tipoDeColumna("historial_caso", "resultado_caso")).isEqualTo("character varying");
    assertThat(tipoDeColumna("historial_caso", "estado_cuenta")).isEqualTo("character varying");
    assertThat(tipoDeColumna("historial_caso", "resolucion")).isEqualTo("text");
    assertThat(tipoDeColumna("historial_caso", "detalle_cambio")).isEqualTo("text");
    assertThat(tipoDeColumna("historial_caso", "es_version_actual")).isEqualTo("boolean");
    assertThat(tipoDeColumna("historial_caso", "fecha_inicio_vigencia"))
        .isEqualTo("timestamp with time zone");
    assertThat(tipoDeColumna("historial_caso", "fecha_fin_vigencia"))
        .isEqualTo("timestamp with time zone");
    assertThat(tipoDeColumna("historial_caso", "fecha_registro"))
        .isEqualTo("timestamp with time zone");
    assertThat(esIdentidad("historial_caso", "id_historial_caso")).isEqualTo("ALWAYS");
  }

  @Test
  void losTiposDeLaMedidaSonLosDelDiccionario() {
    assertThat(tipoDeColumna("medida_administrativa", "id_medida_administrativa"))
        .isEqualTo("smallint");
    assertThat(tipoDeColumna("medida_administrativa", "codigo")).isEqualTo("character varying");
    assertThat(longitudDeColumna("medida_administrativa", "codigo")).isEqualTo(50);
    assertThat(tipoDeColumna("medida_administrativa", "nombre")).isEqualTo("character varying");
    assertThat(longitudDeColumna("medida_administrativa", "nombre")).isEqualTo(100);
    assertThat(tipoDeColumna("medida_administrativa", "descripcion")).isEqualTo("text");
    assertThat(tipoDeColumna("medida_administrativa", "nivel_severidad")).isEqualTo("smallint");
    assertThat(tipoDeColumna("medida_administrativa", "estado_cuenta_resultante"))
        .isEqualTo("character varying");
    assertThat(tipoDeColumna("medida_administrativa", "requiere_fecha_fin")).isEqualTo("boolean");
    assertThat(tipoDeColumna("medida_administrativa", "habilitada")).isEqualTo("boolean");
    assertThat(esIdentidad("medida_administrativa", "id_medida_administrativa"))
        .isEqualTo("ALWAYS");
  }

  @Test
  void elCasoNaceAbiertoYConSusFechasPorOmision() {
    Long idCaso = abrirCaso(idReportante, idReportado);

    assertThat(
            jdbc.queryForObject(
                "SELECT estado_actual FROM caso_moderacion WHERE id_caso_moderacion = ?",
                String.class,
                idCaso))
        .isEqualTo("ABIERTO");
    assertThat(
            jdbc.queryForObject(
                """
                SELECT fecha_apertura IS NOT NULL AND fecha_actualizacion IS NOT NULL
                FROM caso_moderacion WHERE id_caso_moderacion = ?
                """,
                Boolean.class,
                idCaso))
        .isTrue();
  }

  @Test
  void laMedidaNaceHabilitadaYSinExigirFechaFin() {
    Short idMedida = insertarMedida("ADVERTENCIA", "Advertencia", 1, null);

    assertThat(
            jdbc.queryForObject(
                """
                SELECT habilitada AND NOT requiere_fecha_fin
                FROM medida_administrativa WHERE id_medida_administrativa = ?
                """,
                Boolean.class,
                idMedida))
        .isTrue();
  }

  @Test
  void elCatalogoDeMedidasLlegaVacio() {
    // P9 crea la tabla porque las dos siguientes la referencian, pero no siembra
    // el catálogo: elegir, gestionar y aplicar medidas es P10B.
    jdbc.update("DELETE FROM medida_administrativa");

    assertThat(jdbc.queryForObject("SELECT count(*) FROM medida_administrativa", Integer.class))
        .isZero();
  }

  // --- Dominios controlados ----------------------------------------------

  @Test
  void elEstadoDelCasoSoloAdmiteLosCuatroDelDominio() {
    for (String estado : List.of("ABIERTO", "EN_REVISION", "REABIERTO")) {
      assertThatCode(() -> insertarCasoConEstado(estado)).doesNotThrowAnyException();
      jdbc.update("DELETE FROM caso_moderacion");
    }
    assertThatThrownBy(() -> insertarCasoConEstado("ARCHIVADO"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elResultadoDelCasoSoloAdmiteProcedenteYDesestimado() {
    assertThatThrownBy(() -> insertarCasoCerrado("PARCIAL"))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatCode(() -> insertarCasoCerrado("PROCEDENTE")).doesNotThrowAnyException();
    jdbc.update("DELETE FROM caso_moderacion");
    assertThatCode(() -> insertarCasoCerrado("DESESTIMADO")).doesNotThrowAnyException();
  }

  @Test
  void elTipoDeActorYElTipoDeEventoSoloAdmitenSuDominio() {
    Long idCaso = abrirCaso(idReportante, idReportado);

    assertThatThrownBy(
            () -> insertarVersion(idCaso, 2, "MODERADOR", "CASO_ABIERTO", idReportante, ahora(), 1))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> insertarVersion(idCaso, 2, "USUARIO", "CASO_ARCHIVADO", idReportante, ahora(), 1))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elEstadoDeCuentaDelHistorialSoloAdmiteSuDominio() {
    Long idCaso = abrirCaso(idReportante, idReportado);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO historial_caso
                        (id_caso_moderacion, id_usuario_afectado, id_actor, numero_version,
                         tipo_actor, tipo_evento, estado_caso, estado_cuenta, detalle_cambio,
                         fecha_inicio_vigencia, es_version_actual)
                    VALUES (?, ?, ?, 2, 'USUARIO', 'CASO_ABIERTO', 'ABIERTO', 'BLOQUEADA',
                            'Detalle', CURRENT_TIMESTAMP, FALSE)
                    """,
                    idCaso,
                    idReportado,
                    idReportante))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elEstadoResultanteDeUnaMedidaSoloAdmiteElDominioDeEstadoCuenta() {
    assertThatThrownBy(() -> insertarMedida("BLOQUEO", "Bloqueo", 1, "CONGELADA"))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatCode(() -> insertarMedida("SUSPENSION", "Suspensión", 3, "SUSPENDIDA_TEMPORAL"))
        .doesNotThrowAnyException();
  }

  @Test
  void elNivelDeSeveridadDebeSerPositivo() {
    assertThatThrownBy(() -> insertarMedida("NULA", "Nula", 0, null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertarMedida("NEGATIVA", "Negativa", -1, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  // --- Reglas del caso ----------------------------------------------------

  @Test
  void cadaParticipanteAbreComoMaximoUnCasoPorSolicitud() {
    abrirCaso(idReportante, idReportado);

    assertThatThrownBy(() -> abrirCaso(idReportante, idTercero))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void unaSolicitudAdmiteUnCasoDeCadaParticipante() {
    abrirCaso(idReportante, idReportado);
    abrirCaso(idReportado, idReportante);

    assertThat(casosDeLaSolicitud()).isEqualTo(2);
  }

  @Test
  void nadiePuedeReportarseASiMismo() {
    assertThatThrownBy(() -> abrirCaso(idReportante, idReportante))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elCierreExigeResultadoResolucionYFechaALaVez() {
    // Cerrado sin los tres campos: la base lo rechaza en cualquier combinación.
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO caso_moderacion
                        (id_solicitud_servicio, id_reportante, id_reportado, motivo,
                         descripcion, estado_actual, resultado_actual)
                    VALUES (?, ?, ?, 'Motivo', 'Descripción', 'CERRADO', 'PROCEDENTE')
                    """,
                    idSolicitud,
                    idReportante,
                    idReportado))
        .isInstanceOf(DataIntegrityViolationException.class);

    // Y un caso que no está cerrado no puede arrastrar una decisión vigente.
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO caso_moderacion
                        (id_solicitud_servicio, id_reportante, id_reportado, motivo,
                         descripcion, estado_actual, resultado_actual, resolucion_actual,
                         fecha_cierre_actual)
                    VALUES (?, ?, ?, 'Motivo', 'Descripción', 'ABIERTO', 'PROCEDENTE',
                            'Resolución', CURRENT_TIMESTAMP)
                    """,
                    idSolicitud,
                    idReportante,
                    idReportado))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatCode(() -> insertarCasoCerrado("PROCEDENTE")).doesNotThrowAnyException();
  }

  @Test
  void laFechaFinDeLaMedidaDebeSerPosteriorALaApertura() {
    Short idMedida = insertarMedida("RESTRICCION", "Restricción", 2, "RESTRINGIDA_TEMPORAL");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO caso_moderacion
                        (id_solicitud_servicio, id_reportante, id_reportado, motivo,
                         descripcion, id_medida_administrativa_actual,
                         fecha_apertura, fecha_fin_medida_actual)
                    VALUES (?, ?, ?, 'Motivo', 'Descripción', ?,
                            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP - INTERVAL '1 day')
                    """,
                    idSolicitud,
                    idReportante,
                    idReportado,
                    idMedida))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elCasoNoPuedeApuntarAUnaSolicitudNiAUnUsuarioInexistentes() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO caso_moderacion
                        (id_solicitud_servicio, id_reportante, id_reportado, motivo, descripcion)
                    VALUES (?, ?, ?, 'Motivo', 'Descripción')
                    """,
                    idSolicitud + 10_000,
                    idReportante,
                    idReportado))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> abrirCaso(idReportante + 10_000, idReportado))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> abrirCaso(idReportante, idReportado + 10_000))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void noSeBorraEnCascadaNiLaSolicitudNiNingunaDeLasDosPersonas() {
    abrirCaso(idReportante, idReportado);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM solicitud_servicio WHERE id_solicitud_servicio = ?", idSolicitud))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idReportante))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idReportado))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void unaMedidaReferenciadaNoSeBorraFisicamente() {
    Short idMedida = insertarMedida("ADVERTENCIA", "Advertencia", 1, null);
    jdbc.update(
        """
        INSERT INTO caso_moderacion
            (id_solicitud_servicio, id_reportante, id_reportado, motivo,
             descripcion, id_medida_administrativa_actual)
        VALUES (?, ?, ?, 'Motivo', 'Descripción', ?)
        """,
        idSolicitud,
        idReportante,
        idReportado,
        idMedida);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "DELETE FROM medida_administrativa WHERE id_medida_administrativa = ?",
                    idMedida))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  // --- Reglas del historial SCD2 -----------------------------------------

  /**
   * Dos versiones vigentes a la vez son imposibles, y lo son por partida doble: chocan con el
   * índice único parcial y también con la exclusión temporal, porque dos periodos abiertos por
   * arriba siempre se superponen. La prueba afirma el resultado, no cuál de las dos actuó.
   */
  @Test
  void soloExisteUnaVersionActualPorCaso() {
    Long idCaso = abrirCaso(idReportante, idReportado);
    insertarVersion(idCaso, 1, "USUARIO", "CASO_ABIERTO", idReportante, ahora(), null);

    assertThatThrownBy(
            () ->
                insertarVersion(
                    idCaso, 2, "ADMINISTRADOR", "ESTADO_CASO_CAMBIADO", idReportado, ahora(), null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(versionesDelCaso(idCaso)).isEqualTo(1);
  }

  @Test
  void elIndiceDeVersionActualEsUnicoYParcial() {
    List<String> indices =
        jdbc.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename = 'historial_caso' ORDER BY indexname",
            String.class);

    assertThat(indices)
        .contains(
            "pk_historial_caso", "uq_historial_caso_version", "uq_historial_caso_version_actual");

    String definicion =
        jdbc.queryForObject(
            "SELECT indexdef FROM pg_indexes WHERE indexname = 'uq_historial_caso_version_actual'",
            String.class);
    assertThat(definicion).contains("UNIQUE").contains("id_caso_moderacion").contains("WHERE");
  }

  @Test
  void noSeRepiteUnNumeroDeVersionDentroDelMismoCaso() {
    Long idCaso = abrirCaso(idReportante, idReportado);
    OffsetDateTime inicio = ahora();
    insertarVersion(idCaso, 1, "USUARIO", "CASO_ABIERTO", idReportante, inicio, 30);

    assertThatThrownBy(
            () ->
                insertarVersion(
                    idCaso, 1, "ADMINISTRADOR", "ESTADO_CASO_CAMBIADO", idReportado, inicio, 60))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elNumeroDeVersionDebeSerPositivo() {
    Long idCaso = abrirCaso(idReportante, idReportado);

    assertThatThrownBy(
            () -> insertarVersion(idCaso, 0, "USUARIO", "CASO_ABIERTO", idReportante, ahora(), 30))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> insertarVersion(idCaso, -1, "USUARIO", "CASO_ABIERTO", idReportante, ahora(), 30))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void unEventoDelSistemaNoTieneActorYLosDemasSiLoTienen() {
    Long idCaso = abrirCaso(idReportante, idReportado);

    // SISTEMA con actor identificado: incoherente.
    assertThatThrownBy(
            () ->
                insertarVersion(idCaso, 1, "SISTEMA", "MEDIDA_EXPIRADA", idReportante, ahora(), 30))
        .isInstanceOf(DataIntegrityViolationException.class);
    // USUARIO sin actor: igual de incoherente.
    assertThatThrownBy(
            () -> insertarVersion(idCaso, 1, "USUARIO", "CASO_ABIERTO", null, ahora(), 30))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatCode(
            () -> insertarVersion(idCaso, 1, "SISTEMA", "MEDIDA_EXPIRADA", null, ahora(), 30))
        .doesNotThrowAnyException();
  }

  @Test
  void laVersionActualNoTieneFinYUnaCerradaTerminaDespuesDeEmpezar() {
    Long idCaso = abrirCaso(idReportante, idReportado);
    OffsetDateTime inicio = ahora();

    // Actual con fin declarado.
    assertThatThrownBy(
            () -> insertarVersionActualCon(idCaso, 1, inicio, inicio.plusMinutes(30), true))
        .isInstanceOf(DataIntegrityViolationException.class);
    // Cerrada sin fin.
    assertThatThrownBy(() -> insertarVersionActualCon(idCaso, 1, inicio, null, false))
        .isInstanceOf(DataIntegrityViolationException.class);
    // Cerrada con un fin anterior a su inicio.
    assertThatThrownBy(
            () -> insertarVersionActualCon(idCaso, 1, inicio, inicio.minusMinutes(1), false))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatCode(() -> insertarVersionActualCon(idCaso, 1, inicio, null, true))
        .doesNotThrowAnyException();
  }

  @Test
  void elDetalleDelCambioNoPuedeQuedarEnBlanco() {
    Long idCaso = abrirCaso(idReportante, idReportado);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO historial_caso
                        (id_caso_moderacion, id_usuario_afectado, id_actor, numero_version,
                         tipo_actor, tipo_evento, estado_caso, estado_cuenta, detalle_cambio,
                         fecha_inicio_vigencia, es_version_actual)
                    VALUES (?, ?, ?, 1, 'USUARIO', 'CASO_ABIERTO', 'ABIERTO', 'ACTIVA', '   ',
                            CURRENT_TIMESTAMP, TRUE)
                    """,
                    idCaso,
                    idReportado,
                    idReportante))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void noSeBorraEnCascadaNiElCasoNiLaCuentaAfectada() {
    Long idCaso = abrirCaso(idReportante, idReportado);
    insertarVersion(idCaso, 1, "USUARIO", "CASO_ABIERTO", idReportante, ahora(), null);

    assertThatThrownBy(
            () -> jdbc.update("DELETE FROM caso_moderacion WHERE id_caso_moderacion = ?", idCaso))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idReportado))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  // --- Exclusión temporal (V51) -------------------------------------------

  @Test
  void laExtensionBtreeGistEstaInstalada() {
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'btree_gist'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void laRestriccionDeExclusionEstaDeclaradaSobreElCasoYElPeriodo() {
    String definicion =
        jdbc.queryForObject(
            """
            SELECT pg_get_constraintdef(c.oid)
            FROM pg_constraint c
            JOIN pg_class t ON t.oid = c.conrelid
            WHERE t.relname = 'historial_caso' AND c.conname = 'ex_historial_caso_vigencia'
            """,
            String.class);

    assertThat(definicion)
        .isNotNull()
        .contains("EXCLUDE USING gist")
        .contains("id_caso_moderacion WITH =")
        .contains("tstzrange")
        .contains("'[)'::text")
        .contains("WITH &&");
  }

  @Test
  void dosVersionesDelMismoCasoNoPuedenSuperponerse() {
    Long idCaso = abrirCaso(idReportante, idReportado);
    OffsetDateTime inicio = ahora();
    // Versión cerrada que ocupa [inicio, inicio + 60 min).
    insertarVersion(idCaso, 1, "USUARIO", "CASO_ABIERTO", idReportante, inicio, 60);

    // Otra que empieza dentro de ese periodo: se solapan.
    assertThatThrownBy(
            () ->
                insertarVersion(
                    idCaso,
                    2,
                    "ADMINISTRADOR",
                    "ESTADO_CASO_CAMBIADO",
                    idReportado,
                    inicio.plusMinutes(30),
                    90))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void dosVersionesConsecutivasCompartenElInstanteDeTransicion() {
    Long idCaso = abrirCaso(idReportante, idReportado);
    OffsetDateTime inicio = ahora();
    OffsetDateTime transicion = inicio.plusMinutes(60);

    insertarVersion(idCaso, 1, "USUARIO", "CASO_ABIERTO", idReportante, inicio, 60);

    // El fin es exclusivo: empezar justo donde la anterior terminó no se solapa.
    assertThatCode(
            () ->
                insertarVersionActualCon(
                    idCaso, 2, transicion, null, true, "ADMINISTRADOR", idReportado))
        .doesNotThrowAnyException();
    assertThat(versionesDelCaso(idCaso)).isEqualTo(2);
  }

  @Test
  void laVersionActualExcluyeCualquierPeriodoPosterior() {
    Long idCaso = abrirCaso(idReportante, idReportado);
    OffsetDateTime inicio = ahora();
    // Sin fin, el rango queda abierto por arriba.
    insertarVersion(idCaso, 1, "USUARIO", "CASO_ABIERTO", idReportante, inicio, null);

    assertThatThrownBy(
            () ->
                insertarVersionActualCon(
                    idCaso,
                    2,
                    inicio.plusDays(1),
                    inicio.plusDays(2),
                    false,
                    "ADMINISTRADOR",
                    idReportado))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void dosCasosDistintosPuedenTenerPeriodosIguales() {
    Long primero = abrirCaso(idReportante, idReportado);
    Long segundo = abrirCaso(idReportado, idReportante);
    OffsetDateTime inicio = ahora();

    insertarVersion(primero, 1, "USUARIO", "CASO_ABIERTO", idReportante, inicio, null);

    // La exclusión es por caso: dos expedientes distintos son independientes.
    assertThatCode(
            () -> insertarVersion(segundo, 1, "USUARIO", "CASO_ABIERTO", idReportado, inicio, null))
        .doesNotThrowAnyException();
  }

  // --- Utilidades ---------------------------------------------------------

  private static OffsetDateTime ahora() {
    return OffsetDateTime.now();
  }

  private Long abrirCaso(Long reportante, Long reportado) {
    return jdbc.queryForObject(
        """
        INSERT INTO caso_moderacion
            (id_solicitud_servicio, id_reportante, id_reportado, motivo, descripcion)
        VALUES (?, ?, ?, 'Trato irrespetuoso', 'Descripción de los hechos reportados.')
        RETURNING id_caso_moderacion
        """,
        Long.class,
        idSolicitud,
        reportante,
        reportado);
  }

  private void insertarCasoConEstado(String estado) {
    jdbc.update(
        """
        INSERT INTO caso_moderacion
            (id_solicitud_servicio, id_reportante, id_reportado, motivo,
             descripcion, estado_actual)
        VALUES (?, ?, ?, 'Motivo', 'Descripción', ?)
        """,
        idSolicitud,
        idReportante,
        idReportado,
        estado);
  }

  private void insertarCasoCerrado(String resultado) {
    jdbc.update(
        """
        INSERT INTO caso_moderacion
            (id_solicitud_servicio, id_reportante, id_reportado, motivo, descripcion,
             estado_actual, resultado_actual, resolucion_actual, fecha_cierre_actual)
        VALUES (?, ?, ?, 'Motivo', 'Descripción', 'CERRADO', ?, 'Resolución',
                CURRENT_TIMESTAMP)
        """,
        idSolicitud,
        idReportante,
        idReportado,
        resultado);
  }

  private Short insertarMedida(
      String codigo, String nombre, int severidad, String estadoResultante) {
    return jdbc.queryForObject(
        """
        INSERT INTO medida_administrativa
            (codigo, nombre, nivel_severidad, estado_cuenta_resultante)
        VALUES (?, ?, ?, ?)
        RETURNING id_medida_administrativa
        """,
        Short.class,
        codigo,
        nombre,
        severidad,
        estadoResultante);
  }

  /**
   * Una versión del historial con un periodo de {@code minutos} de duración, o abierto por arriba
   * —es decir, vigente— cuando {@code minutos} es nulo.
   */
  private void insertarVersion(
      Long idCaso,
      int version,
      String tipoActor,
      String tipoEvento,
      Long idActor,
      OffsetDateTime inicio,
      Integer minutos) {

    boolean vigente = minutos == null;
    insertarVersionActualCon(
        idCaso,
        version,
        inicio,
        vigente ? null : inicio.plusMinutes(minutos),
        vigente,
        tipoActor,
        idActor,
        tipoEvento);
  }

  private void insertarVersionActualCon(
      Long idCaso, int version, OffsetDateTime inicio, OffsetDateTime fin, boolean actual) {
    insertarVersionActualCon(
        idCaso, version, inicio, fin, actual, "USUARIO", idReportante, "CASO_ABIERTO");
  }

  private void insertarVersionActualCon(
      Long idCaso,
      int version,
      OffsetDateTime inicio,
      OffsetDateTime fin,
      boolean actual,
      String tipoActor,
      Long idActor) {
    insertarVersionActualCon(
        idCaso, version, inicio, fin, actual, tipoActor, idActor, "ESTADO_CASO_CAMBIADO");
  }

  private void insertarVersionActualCon(
      Long idCaso,
      int version,
      OffsetDateTime inicio,
      OffsetDateTime fin,
      boolean actual,
      String tipoActor,
      Long idActor,
      String tipoEvento) {

    jdbc.update(
        """
        INSERT INTO historial_caso
            (id_caso_moderacion, id_usuario_afectado, id_actor, numero_version,
             tipo_actor, tipo_evento, estado_caso, estado_cuenta, detalle_cambio,
             fecha_inicio_vigencia, fecha_fin_vigencia, es_version_actual)
        VALUES (?, ?, ?, ?, ?, ?, 'ABIERTO', 'ACTIVA', 'Detalle del cambio.', ?, ?, ?)
        """,
        idCaso,
        idReportado,
        idActor,
        version,
        tipoActor,
        tipoEvento,
        inicio,
        fin,
        actual);
  }

  private Integer casosDeLaSolicitud() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM caso_moderacion WHERE id_solicitud_servicio = ?",
        Integer.class,
        idSolicitud);
  }

  private Integer versionesDelCaso(Long idCaso) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM historial_caso WHERE id_caso_moderacion = ?", Integer.class, idCaso);
  }

  private List<String> tablasDelEsquema() {
    return jdbc.queryForList(
        """
        SELECT table_name FROM information_schema.tables
        WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
        ORDER BY table_name
        """,
        String.class);
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

  private Integer longitudDeColumna(String tabla, String columna) {
    return jdbc.queryForObject(
        """
        SELECT character_maximum_length FROM information_schema.columns
        WHERE table_name = ? AND column_name = ?
        """,
        Integer.class,
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
