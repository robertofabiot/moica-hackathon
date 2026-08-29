package com.moica.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * Descubrimiento público: visitante sin sesión, filtros combinados y exclusión de lo no visible.
 */
class DescubrimientoIT extends EscenarioDeServicio {

  @Test
  void unVisitanteSinSesionVeSoloServiciosHabilitados() {
    publicarServicioVisible("Destape urgente");

    HttpResponse<String> respuesta = abrirNavegador().get(RUTA_SERVICIOS_PUBLICOS);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    JsonNode lista = json(respuesta);
    assertThat(lista).hasSize(1);
    assertThat(lista.get(0).get("nombre").asText()).isEqualTo("Destape urgente");
    assertThat(lista.get(0).get("precioReferencia").isNull()).isTrue();
    assertThat(lista.get(0).has("correoElectronico")).isFalse();
    assertThat(lista.get(0).has("contactos")).isFalse();
    assertThat(respuesta.body()).doesNotContain("claveAlmacenamiento", "observacion");
  }

  @Test
  void excluyeInactivosNoDisponiblesSinVerificarYCuentasNoOperativas() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    aprobarBasica(admin);

    long inactivo = idDe(crearServicio("Inactivo"));
    long activo = idDe(crearServicio("Visible"));
    assertThat(activar(activo).statusCode()).isEqualTo(HttpStatus.OK.value());

    NavegadorDePrueba noDisponible = prestadorVerificado("nodisp@moica.test", admin);
    publicarDesde(noDisponible, "No disponible");
    noDisponible.put(RUTA_DISPONIBILIDAD, Map.of("disponibilidad", "NO_DISPONIBLE"));

    NavegadorDePrueba sinVerificar = abrirNavegador();
    registrar(sinVerificar, "sinver@moica.test", CLAVE);
    iniciarSesion(sinVerificar, "sinver@moica.test", CLAVE);
    sinVerificar.post(RUTA_PERFIL, solicitudDePerfil());
    sinVerificar.post(
        RUTA_SERVICIOS_PROPIOS, solicitudDeServicio("Privado", idSubcategoria("Plomería")));

    NavegadorDePrueba restringido = prestadorVerificado("restringido@moica.test", admin);
    long idRestringido = publicarDesde(restringido, "Restringido");
    restringirCuenta("restringido@moica.test");

    JsonNode lista = json(abrirNavegador().get(RUTA_SERVICIOS_PUBLICOS));
    assertThat(nombresDe(lista)).containsExactly("Visible");
    assertThat(abrirNavegador().get(RUTA_SERVICIOS_PUBLICOS + "/" + inactivo).statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(abrirNavegador().get(RUTA_SERVICIOS_PUBLICOS + "/" + idRestringido).statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void combinaTextoCategoriaSubcategoriaYMunicipio() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    aprobarBasica(admin);

    long plomeria = idDe(crearServicio("Destape de lavadero"));
    assertThat(activar(plomeria).statusCode()).isEqualTo(HttpStatus.OK.value());

    HttpResponse<String> electricidad =
        navegador.post(
            RUTA_SERVICIOS_PROPIOS,
            solicitudDeServicio("Instalación eléctrica", idSubcategoria("Electricidad")));
    assertThat(activar(idDe(electricidad)).statusCode()).isEqualTo(HttpStatus.OK.value());

    NavegadorDePrueba tipitapa = prestadorVerificado("tipitapa@moica.test", admin);
    Integer idTipitapa =
        jdbc.queryForObject(
            "SELECT id_municipio FROM municipio WHERE nombre = 'Tipitapa'", Integer.class);
    tipitapa.put(
        RUTA_PERFIL,
        Map.of(
            "nombrePublico",
            "Taller Tipitapa",
            "descripcion",
            "Servicios en Tipitapa.",
            "tipoPrestador",
            "INDEPENDIENTE",
            "idMunicipioPrincipal",
            idTipitapa,
            "descripcionCobertura",
            "Tipitapa centro"));
    publicarDesde(tipitapa, "Destape en Tipitapa");

    NavegadorDePrueba visitante = abrirNavegador();

    JsonNode porTexto = json(visitante.get(RUTA_SERVICIOS_PUBLICOS + "?texto=destape"));
    assertThat(nombresDe(porTexto)).containsExactly("Destape de lavadero", "Destape en Tipitapa");

    JsonNode porCategoria =
        json(
            visitante.get(
                RUTA_SERVICIOS_PUBLICOS + "?idCategoria=" + idCategoria("Hogar y mantenimiento")));
    assertThat(porCategoria.size()).isEqualTo(3);

    JsonNode porSubcategoria =
        json(
            visitante.get(
                RUTA_SERVICIOS_PUBLICOS + "?idSubcategoria=" + idSubcategoria("Electricidad")));
    assertThat(nombresDe(porSubcategoria)).containsExactly("Instalación eléctrica");

    JsonNode combinado =
        json(
            visitante.get(
                RUTA_SERVICIOS_PUBLICOS
                    + "?texto=destape&idSubcategoria="
                    + idSubcategoria("Plomería")
                    + "&idMunicipio="
                    + idMunicipioManagua()));
    assertThat(nombresDe(combinado)).containsExactly("Destape de lavadero");
  }

  @Test
  void elDetallePublicoTraeInsigniaAdvertenciaYNoDatosPrivados() {
    long id = publicarServicioVisible("Visita técnica");

    HttpResponse<String> respuesta = abrirNavegador().get(RUTA_SERVICIOS_PUBLICOS + "/" + id);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    JsonNode cuerpo = json(respuesta);
    assertThat(cuerpo.get("admiteContratacion").asBoolean()).isTrue();
    assertThat(cuerpo.get("prestador").get("nivelVerificacion").asText())
        .isEqualTo("VERIFICADO_BASICO");
    assertThat(cuerpo.get("prestador").get("advertenciaDeInsignia").asText())
        .contains("No garantiza la calidad futura");
    assertThat(cuerpo.get("prestador").has("contactos")).isFalse();
    assertThat(respuesta.body())
        .doesNotContain("claveAlmacenamiento", "correoElectronico", "observacionResolucion");
  }

  @Test
  void elPerfilPublicoIncluyePortafolioYOmiteContactos() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    aprobarBasica(admin);
    navegador.post(
        RUTA_TRABAJOS,
        Map.of("titulo", "Cambio de tubería", "descripcion", "Trabajo anterior documentado."));
    long idServicio = idDe(crearServicio("Servicio actual"));
    assertThat(activar(idServicio).statusCode()).isEqualTo(HttpStatus.OK.value());

    Long idPrestador = idDe(CORREO);
    HttpResponse<String> respuesta =
        abrirNavegador().get(RUTA_PRESTADORES_PUBLICOS + "/" + idPrestador);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    JsonNode cuerpo = json(respuesta);
    assertThat(cuerpo.get("prestador").get("nombrePublico").asText())
        .isEqualTo("Taller La Esperanza");
    assertThat(cuerpo.get("portafolio")).hasSize(1);
    assertThat(cuerpo.get("portafolio").get(0).get("titulo").asText())
        .isEqualTo("Cambio de tubería");
    assertThat(cuerpo.get("servicios")).hasSize(1);
    assertThat(cuerpo.get("admiteContratacion").asBoolean()).isTrue();
    assertThat(cuerpo.get("prestador").has("contactos")).isFalse();
    assertThat(respuesta.body()).doesNotContain("8888", "correoElectronico");
  }

  @Test
  void unPerfilNoDisponibleSigueVisibleSinServiciosNiContratacion() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    aprobarBasica(admin);
    navegador.post(
        RUTA_TRABAJOS,
        Map.of("titulo", "Cambio de tubería", "descripcion", "Trabajo anterior documentado."));
    long idServicio = idDe(crearServicio("Servicio actual"));
    assertThat(activar(idServicio).statusCode()).isEqualTo(HttpStatus.OK.value());
    dejarDisponible("NO_DISPONIBLE");

    Long idPrestador = idDe(CORREO);
    NavegadorDePrueba visitante = abrirNavegador();
    HttpResponse<String> respuesta = visitante.get(RUTA_PRESTADORES_PUBLICOS + "/" + idPrestador);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    JsonNode cuerpo = json(respuesta);
    assertThat(cuerpo.get("prestador").get("nombrePublico").asText())
        .isEqualTo("Taller La Esperanza");
    assertThat(cuerpo.get("portafolio")).hasSize(1);
    assertThat(cuerpo.get("portafolio").get(0).get("titulo").asText())
        .isEqualTo("Cambio de tubería");
    assertThat(cuerpo.get("servicios")).isEmpty();
    assertThat(cuerpo.get("admiteContratacion").asBoolean()).isFalse();
    assertThat(visitante.get(RUTA_SERVICIOS_PUBLICOS + "/" + idServicio).statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(nombresDe(json(visitante.get(RUTA_SERVICIOS_PUBLICOS)))).isEmpty();
  }

  @Test
  void unPerfilSinVerificarResponde404EnPublico() {
    Long idPrestador = idDe(CORREO);

    HttpResponse<String> respuesta =
        abrirNavegador().get(RUTA_PRESTADORES_PUBLICOS + "/" + idPrestador);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(codigoDeError(respuesta)).isEqualTo("RECURSO_NO_ENCONTRADO");
  }

  @Test
  void elOrdenPublicoEsDeterminista() {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    aprobarBasica(admin);
    long zeta = idDe(crearServicio("Zeta"));
    long alfa = idDe(crearServicio("Alfa"));
    assertThat(activar(zeta).statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(activar(alfa).statusCode()).isEqualTo(HttpStatus.OK.value());

    JsonNode lista = json(abrirNavegador().get(RUTA_SERVICIOS_PUBLICOS));
    assertThat(nombresDe(lista)).containsExactly("Alfa", "Zeta");
  }

  @Test
  void lasRutasPropiasSiguenExigiendoSesion() {
    HttpResponse<String> propia = abrirNavegador().get(RUTA_SERVICIOS_PROPIOS);
    HttpResponse<String> mutable =
        abrirNavegador()
            .post(RUTA_SERVICIOS_PROPIOS, solicitudDeServicio("X", idSubcategoria("Plomería")));

    assertThat(propia.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(mutable.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  private long publicarServicioVisible(String nombre) {
    NavegadorDePrueba admin = administradora(CORREO_ADMIN);
    aprobarBasica(admin);
    long id = idDe(crearServicio(nombre));
    assertThat(activar(id).statusCode()).isEqualTo(HttpStatus.OK.value());
    return id;
  }

  private NavegadorDePrueba prestadorVerificado(String correo, NavegadorDePrueba admin) {
    NavegadorDePrueba persona = abrirNavegador();
    registrar(persona, correo, CLAVE);
    iniciarSesion(persona, correo, CLAVE);
    assertThat(persona.post(RUTA_PERFIL, solicitudDePerfil()).statusCode())
        .isEqualTo(HttpStatus.CREATED.value());

    HttpResponse<String> envio = enviarExpediente(persona, "BASICA", cedula());
    assertThat(envio.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    long solicitud = json(envio).get("idSolicitudVerificacion").asLong();
    assertThat(admin.post(RUTA_REVISION + "/" + solicitud + "/toma", Map.of()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    assertThat(admin.post(RUTA_REVISION + "/" + solicitud + "/aprobacion", Map.of()).statusCode())
        .isEqualTo(HttpStatus.OK.value());
    return persona;
  }

  private long publicarDesde(NavegadorDePrueba desde, String nombre) {
    HttpResponse<String> creado =
        desde.post(RUTA_SERVICIOS_PROPIOS, solicitudDeServicio(nombre, idSubcategoria("Plomería")));
    assertThat(creado.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    long id = json(creado).get("idServicioPublicado").asLong();
    assertThat(
            desde
                .put(RUTA_SERVICIOS_PROPIOS + "/" + id + "/estado", Map.of("estado", "ACTIVO"))
                .statusCode())
        .isEqualTo(HttpStatus.OK.value());
    return id;
  }

  private static Iterable<String> nombresDe(JsonNode lista) {
    return java.util.stream.StreamSupport.stream(lista.spliterator(), false)
        .map(nodo -> nodo.get("nombre").asText())
        .toList();
  }
}
