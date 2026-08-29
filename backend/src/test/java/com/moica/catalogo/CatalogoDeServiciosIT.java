package com.moica.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.prestador.EscenarioDePrestador;
import java.net.http.HttpResponse;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

/** El catálogo de categorías de demostración, público y determinista. */
class CatalogoDeServiciosIT extends EscenarioDePrestador {

  private static final String RUTA = "/api/catalogos/categorias";

  @Test
  void entregaTresCategoriasConSusSubcategoriasSinSesion() {
    HttpResponse<String> respuesta = abrirNavegador().get(RUTA);

    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.OK.value());
    JsonNode categorias = json(respuesta);
    assertThat(nombresDe(categorias))
        .containsExactly(
            "Belleza y cuidado personal",
            "Hogar y mantenimiento",
            "Tecnología y servicios digitales");

    JsonNode hogar = categoria(categorias, "Hogar y mantenimiento");
    assertThat(nombresDe(hogar.get("subcategorias")))
        .containsExactly("Carpintería", "Electricidad", "Plomería");
    assertThat(hogar.get("descripcion").asText()).contains("no exhaustiva");
  }

  @Test
  void noEsUnaRutaMutable() {
    HttpResponse<String> respuesta = navegador.post(RUTA, java.util.Map.of("nombre", "Nueva"));

    assertThat(respuesta.statusCode())
        .isIn(
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.FORBIDDEN.value(),
            HttpStatus.METHOD_NOT_ALLOWED.value());
  }

  private static JsonNode categoria(JsonNode categorias, String nombre) {
    return StreamSupport.stream(categorias.spliterator(), false)
        .filter(nodo -> nombre.equals(nodo.get("nombre").asText()))
        .findFirst()
        .orElseThrow();
  }

  private static Iterable<String> nombresDe(JsonNode nodos) {
    return StreamSupport.stream(nodos.spliterator(), false)
        .map(nodo -> nodo.get("nombre").asText())
        .toList();
  }
}
