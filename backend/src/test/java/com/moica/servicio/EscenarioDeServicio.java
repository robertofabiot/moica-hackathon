package com.moica.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.NavegadorDePrueba;
import com.moica.verificacion.EscenarioDeVerificacion;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Punto de partida de las pruebas de servicios y descubrimiento.
 *
 * <p>Amplía el escenario de verificación con el catálogo de demostración y fábricas para crear,
 * activar y publicar un servicio.
 */
public abstract class EscenarioDeServicio extends EscenarioDeVerificacion {

  protected static final String RUTA_SERVICIOS_PROPIOS = "/api/prestador/servicios";
  protected static final String RUTA_SERVICIOS_PUBLICOS = "/api/servicios";
  protected static final String RUTA_PRESTADORES_PUBLICOS = "/api/prestadores";
  protected static final String RUTA_CATEGORIAS = "/api/catalogos/categorias";

  protected Integer idSubcategoria(String nombre) {
    return jdbc.queryForObject(
        """
        SELECT s.id_subcategoria_servicio
        FROM subcategoria_servicio s
        JOIN categoria_servicio c ON c.id_categoria_servicio = s.id_categoria_servicio
        WHERE s.nombre = ? AND c.nombre = 'Hogar y mantenimiento'
        """,
        Integer.class,
        nombre);
  }

  protected Short idCategoria(String nombre) {
    return jdbc.queryForObject(
        "SELECT id_categoria_servicio FROM categoria_servicio WHERE nombre = ?",
        Short.class,
        nombre);
  }

  protected Map<String, Object> solicitudDeServicio(String nombre, Integer idSubcategoria) {
    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("nombre", nombre);
    cuerpo.put("descripcion", "Descripción de " + nombre + " para el hogar.");
    cuerpo.put("idSubcategoriaServicio", idSubcategoria);
    cuerpo.put("precioReferencia", null);
    return cuerpo;
  }

  protected Map<String, Object> solicitudDeServicioConPrecio(
      String nombre, Integer idSubcategoria, String precio) {
    Map<String, Object> cuerpo = solicitudDeServicio(nombre, idSubcategoria);
    cuerpo.put("precioReferencia", new java.math.BigDecimal(precio));
    return cuerpo;
  }

  protected HttpResponse<String> crearServicio(String nombre) {
    return crearServicio(navegador, nombre, idSubcategoria("Plomería"));
  }

  protected HttpResponse<String> crearServicio(
      NavegadorDePrueba desde, String nombre, Integer idSubcategoria) {
    HttpResponse<String> respuesta =
        desde.post(RUTA_SERVICIOS_PROPIOS, solicitudDeServicio(nombre, idSubcategoria));
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return respuesta;
  }

  protected long idDe(HttpResponse<String> respuesta) {
    return json(respuesta).get("idServicioPublicado").asLong();
  }

  protected HttpResponse<String> activar(long idServicio) {
    return navegador.put(
        RUTA_SERVICIOS_PROPIOS + "/" + idServicio + "/estado", Map.of("estado", "ACTIVO"));
  }

  protected HttpResponse<String> desactivar(long idServicio) {
    return navegador.put(
        RUTA_SERVICIOS_PROPIOS + "/" + idServicio + "/estado", Map.of("estado", "INACTIVO"));
  }

  protected String estadoEnBase(long idServicio) {
    return jdbc.queryForObject(
        "SELECT estado FROM servicio_publicado WHERE id_servicio_publicado = ?",
        String.class,
        idServicio);
  }

  protected void dejarDisponible(String disponibilidad) {
    assertThat(
            navegador
                .put(RUTA_DISPONIBILIDAD, Map.of("disponibilidad", disponibilidad))
                .statusCode())
        .isEqualTo(HttpStatus.OK.value());
  }
}
