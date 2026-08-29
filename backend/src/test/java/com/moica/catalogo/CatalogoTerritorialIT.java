package com.moica.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.prestador.EscenarioDePrestador;
import java.net.http.HttpResponse;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/**
 * El catálogo territorial que consumen los formularios del perfil.
 *
 * <p>Publica únicamente los departamentos habilitados —Managua en el MVP— con sus municipios en
 * orden alfabético. Desde P5 es lectura pública para el filtro territorial.
 */
class CatalogoTerritorialIT extends EscenarioDePrestador {

  @Test
  void entregaManaguaConSusNueveMunicipiosEnOrden() {
    HttpResponse<String> respuesta = navegador.get(RUTA_CATALOGO);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());

    JsonNode departamentos = json(respuesta);
    assertThat(departamentos).hasSize(1);
    assertThat(departamentos.get(0).get("nombre").asText()).isEqualTo("Managua");

    JsonNode municipios = departamentos.get(0).get("municipios");
    assertThat(nombresDe(municipios))
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
  void noPublicaUnDepartamentoDeshabilitado() {
    municipioDeDepartamentoNoHabilitado();

    JsonNode departamentos = json(navegador.get(RUTA_CATALOGO));

    assertThat(departamentos).hasSize(1);
    assertThat(departamentos.get(0).get("nombre").asText()).isEqualTo("Managua");
  }

  @Test
  void sinSesionElCatalogoSiSeEntrega() {
    HttpResponse<String> respuesta = abrirNavegador().get(RUTA_CATALOGO);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(json(respuesta).get(0).get("nombre").asText()).isEqualTo("Managua");
  }

  private static Iterable<String> nombresDe(JsonNode municipios) {
    return StreamSupport.stream(municipios.spliterator(), false)
        .map(municipio -> municipio.get("nombre").asText())
        .toList();
  }
}
