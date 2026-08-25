package com.moica;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

/**
 * Un navegador de mentira para las pruebas de integración.
 *
 * <p>Hace lo mismo que haría el navegador de una persona: guarda las cookies que recibe, las
 * devuelve en la petición siguiente y repite el token CSRF en la cabecera {@code X-XSRF-TOKEN} de
 * toda operación mutable. Sin esto, cada prueba tendría que reconstruir a mano ese trámite y
 * dejaría de parecerse a lo que ocurre de verdad.
 *
 * <p>El almacén de cookies es el del JDK, así que una cookie que llega con {@code Max-Age=0}
 * desaparece igual que en un navegador: es lo que permite comprobar que cerrar sesión la borra.
 *
 * <p>Los métodos {@code sinTokenCsrf} existen para lo contrario: comprobar que sin ese trámite la
 * operación no se completa.
 */
public final class NavegadorDePrueba {

  private static final String COOKIE_CSRF = "XSRF-TOKEN";
  private static final String CABECERA_CSRF = "X-XSRF-TOKEN";

  private final String base;
  private final ObjectWriter escritor;
  private final CookieManager galletas = new CookieManager();
  private final HttpClient cliente;

  public NavegadorDePrueba(int puerto, ObjectMapper mapeador) {
    this.base = "http://localhost:" + puerto;
    this.escritor = mapeador.writer();
    this.cliente = HttpClient.newBuilder().cookieHandler(galletas).build();
  }

  public HttpResponse<String> get(String ruta) {
    return enviar(constructor(ruta).GET(), true);
  }

  public HttpResponse<String> post(String ruta, Object cuerpo) {
    return enviar(constructor(ruta).POST(comoJson(cuerpo)), true);
  }

  public HttpResponse<String> postSinTokenCsrf(String ruta, Object cuerpo) {
    return enviar(constructor(ruta).POST(comoJson(cuerpo)), false);
  }

  public HttpResponse<String> put(String ruta, Object cuerpo) {
    return enviar(constructor(ruta).PUT(comoJson(cuerpo)), true);
  }

  public HttpResponse<String> putSinTokenCsrf(String ruta, Object cuerpo) {
    return enviar(constructor(ruta).PUT(comoJson(cuerpo)), false);
  }

  public HttpResponse<String> delete(String ruta) {
    return enviar(constructor(ruta).DELETE(), true);
  }

  public HttpResponse<String> deleteSinTokenCsrf(String ruta) {
    return enviar(constructor(ruta).DELETE(), false);
  }

  /** Valor de una cookie guardada, si el servidor la envió y sigue vigente. */
  public Optional<String> cookie(String nombre) {
    return galletas.getCookieStore().getCookies().stream()
        .filter(cookie -> cookie.getName().equals(nombre))
        .map(HttpCookie::getValue)
        .findFirst();
  }

  /** Sustituye una cookie, para poder llegar con un token que no emitió esta sesión. */
  public void ponerCookie(String nombre, String valor) {
    URI origen = URI.create(base);
    List<HttpCookie> guardadas = List.copyOf(galletas.getCookieStore().getCookies());
    guardadas.stream()
        .filter(cookie -> cookie.getName().equals(nombre))
        .forEach(cookie -> galletas.getCookieStore().remove(origen, cookie));

    HttpCookie nueva = new HttpCookie(nombre, valor);
    nueva.setPath("/");
    nueva.setDomain("localhost");
    galletas.getCookieStore().add(origen, nueva);
  }

  /** Olvida las cookies, como quien cierra el navegador sin cerrar sesión. */
  public void olvidarCookies() {
    galletas.getCookieStore().removeAll();
  }

  private HttpRequest.Builder constructor(String ruta) {
    return HttpRequest.newBuilder(URI.create(base + ruta))
        .header("Content-Type", "application/json");
  }

  private HttpRequest.BodyPublisher comoJson(Object cuerpo) {
    if (cuerpo instanceof String texto) {
      // Permite enviar a propósito algo que no es JSON válido.
      return HttpRequest.BodyPublishers.ofString(texto);
    }
    return HttpRequest.BodyPublishers.ofString(escritor.writeValueAsString(cuerpo));
  }

  private HttpResponse<String> enviar(HttpRequest.Builder constructor, boolean conTokenCsrf) {
    if (conTokenCsrf) {
      cookie(COOKIE_CSRF).ifPresent(token -> constructor.header(CABECERA_CSRF, token));
    }
    try {
      return cliente.send(constructor.build(), HttpResponse.BodyHandlers.ofString());
    } catch (IOException fallo) {
      throw new IllegalStateException("La petición de prueba no llegó al servidor", fallo);
    } catch (InterruptedException interrupcion) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("La petición de prueba quedó interrumpida", interrupcion);
    }
  }
}
