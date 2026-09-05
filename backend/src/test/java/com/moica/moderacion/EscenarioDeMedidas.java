package com.moica.moderacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Punto de partida de las pruebas de medidas administrativas y apelaciones.
 *
 * <p>Hereda el escenario de P10A —hay un caso reportable, cuentas administradoras con segundo
 * factor y las rutas de revisión— y añade lo propio de P10B: el catálogo de medidas y las llamadas
 * de {@code /api/admin/medidas} y {@code /api/admin/casos/{id}/…}.
 *
 * <p>Quien acaba sancionado es siempre el <b>prestador</b> de la sesión base ({@link #CORREO}, cuyo
 * navegador es {@code navegador}): el caso lo abre el cliente contra él. Tenerlo a mano es lo que
 * permite comprobar el estado de la cuenta y la suerte de sus sesiones sin montar nada más.
 *
 * <p>El catálogo lo vacía {@code EscenarioDeSeguridad} junto con el resto del escenario, después de
 * los casos y sus versiones, que son quienes lo referencian. Hasta P10B ninguna prueba lo escribía;
 * ahora sí, y sin esa limpieza el código y el nombre únicos de una medida chocarían con los que
 * dejó la clase anterior.
 */
public abstract class EscenarioDeMedidas extends EscenarioDeRevisionDeCasos {

  protected static final String RUTA_MEDIDAS = "/api/admin/medidas";

  protected static final String JUSTIFICACION =
      "La conducta acreditada en el expediente amerita limitar la cuenta mientras se corrige.";
  protected static final String MOTIVO_DE_REVOCACION =
      "La persona aportó pruebas de que el incidente no ocurrió como se describió.";
  protected static final String RELATO_DE_APELACION =
      "Escribió al correo de soporte pidiendo revisar la decisión y aportando capturas.";
  protected static final String RESOLUCION_DE_APELACION =
      "Las capturas contradicen la versión del reporte, así que la apelación prospera.";
  protected static final String MOTIVO_DE_REAPERTURA =
      "La apelación aceptada obliga a revisar de nuevo los hechos del expediente.";

  // --- Catálogo -----------------------------------------------------------

  /** Una advertencia: no cambia el acceso y no termina sola. */
  protected short medidaDeAdvertencia() {
    return crearMedida("ADVERTENCIA", "Advertencia", (short) 1, null, false);
  }

  /** Una restricción temporal: limita funciones y exige fecha de fin. */
  protected short medidaDeRestriccion() {
    return crearMedida(
        "RESTRICCION_TEMPORAL", "Restricción temporal", (short) 2, "RESTRINGIDA_TEMPORAL", true);
  }

  /** Una suspensión temporal: cierra el acceso hasta una fecha. */
  protected short medidaDeSuspension() {
    return crearMedida(
        "SUSPENSION_TEMPORAL", "Suspensión temporal", (short) 3, "SUSPENDIDA_TEMPORAL", true);
  }

  /** Una suspensión permanente: cierra el acceso sin fecha de reactivación. */
  protected short medidaPermanente() {
    return crearMedida(
        "SUSPENSION_PERMANENTE",
        "Suspensión permanente",
        (short) 4,
        "SUSPENDIDA_PERMANENTE",
        false);
  }

  protected short crearMedida(
      String codigo,
      String nombre,
      short nivelSeveridad,
      String estadoCuentaResultante,
      boolean requiereFechaFin) {

    HttpResponse<String> respuesta =
        crearMedidaCon(
            catalogadora(),
            codigo,
            nombre,
            nivelSeveridad,
            estadoCuentaResultante,
            requiereFechaFin);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return (short) json(respuesta).get("idMedidaAdministrativa").asInt();
  }

  protected HttpResponse<String> crearMedidaCon(
      NavegadorDePrueba desde,
      String codigo,
      String nombre,
      short nivelSeveridad,
      String estadoCuentaResultante,
      boolean requiereFechaFin) {

    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("codigo", codigo);
    cuerpo.put("nombre", nombre);
    cuerpo.put("descripcion", "Medida del catálogo de pruebas.");
    cuerpo.put("nivelSeveridad", nivelSeveridad);
    cuerpo.put("estadoCuentaResultante", estadoCuentaResultante);
    cuerpo.put("requiereFechaFin", requiereFechaFin);
    return desde.post(RUTA_MEDIDAS, cuerpo);
  }

  protected HttpResponse<String> consultarCatalogo(NavegadorDePrueba desde) {
    return desde.get(RUTA_MEDIDAS);
  }

  protected HttpResponse<String> editarMedida(
      NavegadorDePrueba desde,
      short idMedida,
      String nombre,
      short nivelSeveridad,
      String estadoCuentaResultante,
      boolean requiereFechaFin) {

    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("nombre", nombre);
    cuerpo.put("descripcion", "Descripción reescrita.");
    cuerpo.put("nivelSeveridad", nivelSeveridad);
    cuerpo.put("estadoCuentaResultante", estadoCuentaResultante);
    cuerpo.put("requiereFechaFin", requiereFechaFin);
    return desde.put(RUTA_MEDIDAS + "/" + idMedida, cuerpo);
  }

  protected HttpResponse<String> cambiarHabilitacion(
      NavegadorDePrueba desde, short idMedida, boolean habilitada) {
    return desde.put(
        RUTA_MEDIDAS + "/" + idMedida + "/habilitacion", Map.of("habilitada", habilitada));
  }

  /**
   * La administradora que puebla el catálogo en las fábricas de arriba.
   *
   * <p>Se abre una sola vez por prueba y se reutiliza, porque cada llamada registra una cuenta,
   * inicia sesión y activa un segundo factor: hacerlo por cada medida multiplicaría el trabajo sin
   * comprobar nada nuevo.
   */
  protected NavegadorDePrueba catalogadora() {
    if (catalogadora == null) {
      catalogadora = administradora(CORREO_CATALOGO);
    }
    return catalogadora;
  }

  private static final String CORREO_CATALOGO = "catalogadora@moica.test";
  private NavegadorDePrueba catalogadora;

  // --- Medidas de un caso -------------------------------------------------

  /**
   * Un caso ya cerrado como procedente por esa administradora: lo único desde lo que se sanciona.
   */
  protected long casoProcedenteDe(NavegadorDePrueba admin, String correoDelAdmin) {
    long idCaso = casoEnRevisionDe(admin, correoDelAdmin);
    assertThat(cerrar(admin, idCaso, "PROCEDENTE").statusCode()).isEqualTo(HttpStatus.OK.value());
    return idCaso;
  }

  /**
   * Un segundo expediente procedente sobre la <b>misma</b> persona, abierto por otro cliente.
   *
   * <p>Hace falta otro cliente porque cada participante abre como mucho un caso por solicitud y
   * porque una solicitud viva impide enviar otra igual. Los dos expedientes apuntan al mismo
   * prestador, que es lo que permite comprobar la regla de una sola medida vigente <em>por
   * cuenta</em>: la que no se ve bloqueando una sola fila.
   *
   * <p>Conviene abrirlo <b>antes</b> de sancionar. Una cuenta ya restringida no acepta solicitudes
   * nuevas, así que montar el segundo caso después de la primera medida fallaría al aceptar.
   */
  protected long otroCasoProcedenteDe(NavegadorDePrueba admin, String correoDelAdmin) {
    NavegadorDePrueba otroCliente = cuentaAutenticada(CORREO_TERCERO);
    long idSolicitud = idDeSolicitud(enviarSolicitud(otroCliente, idServicio));
    assertThat(aceptar(navegador, idSolicitud).statusCode()).isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> reporte = reportar(otroCliente, idSolicitud);
    assertThat(reporte.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    long idCaso = idDeCaso(reporte);

    assertThat(asignar(admin, idCaso, idDe(correoDelAdmin)).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(iniciarRevision(admin, idCaso).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(cerrar(admin, idCaso, "PROCEDENTE").statusCode()).isEqualTo(HttpStatus.OK.value());
    return idCaso;
  }

  protected HttpResponse<String> aplicarMedida(
      NavegadorDePrueba desde, long idCaso, short idMedida, OffsetDateTime fechaFin) {
    return aplicarMedida(desde, idCaso, idMedida, fechaFin, false);
  }

  protected HttpResponse<String> aplicarMedida(
      NavegadorDePrueba desde,
      long idCaso,
      short idMedida,
      OffsetDateTime fechaFin,
      boolean confirmaReemplazo) {

    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("idMedidaAdministrativa", idMedida);
    cuerpo.put("fechaFinMedida", fechaFin == null ? null : fechaFin.toString());
    cuerpo.put("justificacion", JUSTIFICACION);
    cuerpo.put("confirmaReemplazo", confirmaReemplazo);
    return desde.post(rutaDeExpediente(idCaso) + "/medida", cuerpo);
  }

  protected HttpResponse<String> revocarMedida(NavegadorDePrueba desde, long idCaso) {
    return desde.post(
        rutaDeExpediente(idCaso) + "/medida/revocacion", Map.of("motivo", MOTIVO_DE_REVOCACION));
  }

  // --- Apelaciones --------------------------------------------------------

  protected HttpResponse<String> registrarApelacion(NavegadorDePrueba desde, long idCaso) {
    return desde.post(
        rutaDeExpediente(idCaso) + "/apelacion", Map.of("relato", RELATO_DE_APELACION));
  }

  protected HttpResponse<String> resolverApelacion(
      NavegadorDePrueba desde, long idCaso, boolean aceptada) {
    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("aceptada", aceptada);
    cuerpo.put("resolucion", RESOLUCION_DE_APELACION);
    return desde.post(rutaDeExpediente(idCaso) + "/apelacion/resolucion", cuerpo);
  }

  protected HttpResponse<String> reabrir(NavegadorDePrueba desde, long idCaso) {
    return desde.post(
        rutaDeExpediente(idCaso) + "/reapertura", Map.of("motivo", MOTIVO_DE_REAPERTURA));
  }

  // --- Lecturas de la base ------------------------------------------------

  protected OffsetDateTime fechaFinDeCuentaEnBase(String correo) {
    return jdbc.queryForObject(
        "SELECT fecha_fin_estado_cuenta FROM usuario WHERE correo_electronico = ?",
        OffsetDateTime.class,
        correo);
  }

  /** Cuántas sesiones de esa cuenta siguen sin revocar. */
  protected int sesionesVigentesDe(String correo) {
    Integer total =
        jdbc.queryForObject(
            """
            SELECT count(*) FROM sesion s
            JOIN usuario u ON u.id_usuario = s.id_usuario
            WHERE u.correo_electronico = ? AND s.fecha_revocacion IS NULL
            """,
            Integer.class,
            correo);
    assertThat(total).isNotNull();
    return total;
  }

  protected String motivoDeRevocacionDeLasSesiones(String correo) {
    return jdbc.queryForObject(
        """
        SELECT DISTINCT s.motivo_revocacion FROM sesion s
        JOIN usuario u ON u.id_usuario = s.id_usuario
        WHERE u.correo_electronico = ? AND s.fecha_revocacion IS NOT NULL
        """,
        String.class,
        correo);
  }

  /** Cuántos expedientes de esa persona sostienen una medida. El esquema solo admite uno. */
  protected int medidasVigentesDe(String correo) {
    Integer total =
        jdbc.queryForObject(
            """
            SELECT count(*) FROM caso_moderacion c
            JOIN usuario u ON u.id_usuario = c.id_reportado
            WHERE u.correo_electronico = ? AND c.id_medida_administrativa_actual IS NOT NULL
            """,
            Integer.class,
            correo);
    assertThat(total).isNotNull();
    return total;
  }

  protected int medidasEnCatalogo() {
    Integer total =
        jdbc.queryForObject("SELECT count(*) FROM medida_administrativa", Integer.class);
    assertThat(total).isNotNull();
    return total;
  }

  /**
   * Envejece la medida de un caso para que el barrido la vea vencida sin esperar tiempo real.
   *
   * <p>Retrasa también la apertura del expediente. {@code ck_caso_moderacion_fecha_fin_medida}
   * exige que la fecha de fin sea posterior a la apertura, y un caso recién creado en la prueba
   * nació hace segundos: sin mover la apertura, un plazo en el pasado sería un estado que la base
   * no admite ni siquiera en producción. Envejecer las dos fechas describe lo que de verdad pasaría
   * —un caso antiguo cuya medida ya cumplió— en lugar de forzar una fila imposible.
   */
  protected void vencerLaMedidaDe(long idCaso) {
    jdbc.update(
        """
        UPDATE caso_moderacion
        SET fecha_apertura = fecha_apertura - INTERVAL '2 days',
            fecha_fin_medida_actual = CURRENT_TIMESTAMP - INTERVAL '1 minute'
        WHERE id_caso_moderacion = ?
        """,
        idCaso);
  }

  protected List<String> eventosEnOrden(long idCaso) {
    return versionesEnOrden(idCaso).stream().map(fila -> (String) fila.get("tipo_evento")).toList();
  }

  /** Comprueba lo que el esquema SCD2 promete: una sola vigente, encadenadas y sin solapes. */
  protected void comprobarCadenaScd2(long idCaso) {
    List<Map<String, Object>> versiones = versionesEnOrden(idCaso);

    assertThat(versionesVigentes(idCaso)).isEqualTo(1);
    assertThat(vigenciasSuperpuestas(idCaso)).isZero();

    Map<String, Object> ultima = versiones.get(versiones.size() - 1);
    assertThat(ultima.get("es_version_actual")).isEqualTo(true);
    assertThat(ultima.get("fecha_fin_vigencia")).isNull();

    for (int i = 0; i < versiones.size() - 1; i++) {
      assertThat(versiones.get(i).get("fecha_fin_vigencia"))
          .as("la versión %d cierra al empezar la siguiente", i + 1)
          .isEqualTo(versiones.get(i + 1).get("fecha_inicio_vigencia"));
      assertThat(versiones.get(i).get("es_version_actual")).isEqualTo(false);
    }
  }
}
