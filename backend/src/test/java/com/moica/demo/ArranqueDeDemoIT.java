package com.moica.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.PruebaDeIntegracionConPostgres;
import com.moica.demo.service.DemoService;
import java.util.HashSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Comprueba el ApplicationRunner real y la transacción del bean gestionado por Spring. */
@TestPropertySource(properties = "MOICA_SEED_DEMO_ENABLED=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ArranqueDeDemoIT extends PruebaDeIntegracionConPostgres {
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DemoService demo;
  @Autowired private ObjectMapper mapeador;
  @LocalServerPort private int puerto;

  @Test
  void arranqueActivadoYaConfirmoLosDatosYReejecutarNoDuplica() {
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid'",
                Integer.class))
        .isEqualTo(6);
    var resumen = demo.sincronizar();
    assertThat(resumen.usuariosCreados()).isZero();
    assertThat(resumen.perfilesCreados()).isZero();
    assertThat(resumen.serviciosCreados()).isZero();
    assertThat(resumen.imagenesCreadas()).isZero();
    assertThat(resumen.serviciosExistentes()).isEqualTo(9);
  }

  @Test
  void visitanteAnonimoVeLosNueveServiciosSusFiltrosDetallesYSeisPerfilesPorHttp() {
    var visitante = new NavegadorDePrueba(puerto, mapeador);
    JsonNode lista =
        consultar(visitante, "/api/servicios?texto=ficticio%20para%20demostraci%C3%B3n");
    assertThat(lista).hasSize(9);
    var prestadores = new HashSet<Long>();
    var subcategorias = new HashSet<String>();
    int preciosAConvenir = 0;
    for (JsonNode servicio : lista) {
      long id = servicio.get("idServicioPublicado").asLong();
      JsonNode detalle = consultar(visitante, "/api/servicios/" + id);
      assertThat(detalle.get("nombre").asText()).isEqualTo(servicio.get("nombre").asText());
      assertThat(detalle.get("admiteContratacion").asBoolean()).isTrue();
      JsonNode prestador = detalle.get("prestador");
      long idPrestador = prestador.get("idPrestador").asLong();
      prestadores.add(idPrestador);
      subcategorias.add(detalle.get("nombreSubcategoria").asText());
      if (detalle.get("precioReferencia").isNull()) {
        preciosAConvenir++;
      }
      JsonNode perfil = consultar(visitante, "/api/prestadores/" + idPrestador);
      assertThat(perfil.get("admiteContratacion").asBoolean()).isTrue();
      assertThat(perfil.get("servicios")).isNotEmpty();
      JsonNode filtrados =
          consultar(
              visitante,
              "/api/servicios?texto=ficticio%20para%20demostraci%C3%B3n&idCategoria="
                  + detalle.get("idCategoriaServicio").asInt()
                  + "&idSubcategoria="
                  + detalle.get("idSubcategoriaServicio").asInt()
                  + "&idMunicipio="
                  + prestador.get("municipioPrincipal").get("idMunicipio").asInt());
      assertThat(filtrados).hasSize(1);
      assertThat(filtrados.get(0).get("idServicioPublicado").asLong()).isEqualTo(id);
    }
    assertThat(prestadores).hasSize(6);
    assertThat(preciosAConvenir).isEqualTo(3);
    assertThat(subcategorias)
        .containsExactlyInAnyOrderElementsOf(
            DatosDeDemostracion.SERVICIOS.stream()
                .map(DatosDeDemostracion.Servicio::subcategoria)
                .toList());
  }

  private JsonNode consultar(NavegadorDePrueba visitante, String ruta) {
    var respuesta = visitante.get(ruta);
    assertThat(respuesta.statusCode()).isEqualTo(200);
    assertThat(respuesta.body())
        .doesNotContain("claveHash", "clave_hash", "correoElectronico", "contactos", "observacion");
    return mapeador.readTree(respuesta.body());
  }

  @AfterAll
  static void retirarSoloLaDemoDeEsteArranque(@Autowired JdbcTemplate jdbc) {
    jdbc.update(
        "DELETE FROM servicio_publicado WHERE id_prestador IN (SELECT id_usuario FROM usuario WHERE correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid')");
    jdbc.update(
        "DELETE FROM usuario WHERE correo_electronico LIKE 'moica-demo-v1-%@demo.moica.invalid'");
  }
}
