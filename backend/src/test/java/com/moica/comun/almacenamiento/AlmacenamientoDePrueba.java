package com.moica.comun.almacenamiento;

import com.moica.comun.error.ErrorDeAplicacion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * Doble en memoria del almacén de imágenes para las pruebas de integración.
 *
 * <p>Se comporta como el almacén real visto desde la aplicación: entrega URL públicas bajo una base
 * fija, borra de forma idempotente y falla con el mismo error uniforme. Lo que añade es
 * observabilidad —qué claves se guardaron y se eliminaron, y qué contiene cada objeto— y dos
 * palancas de fallo: simular un proveedor caído y devolver una URL que la base de datos no puede
 * persistir, que es la manera realista de provocar la compensación de un objeto recién subido.
 *
 * <p>No sustituye a una comprobación contra un bucket R2 real: esa queda documentada como
 * procedimiento manual en {@code Docs/Dev/Almacenamiento.md} mientras el entorno no tenga
 * credenciales.
 */
public class AlmacenamientoDePrueba implements AlmacenamientoDeImagenesPublicas {

  /** Base propia e inconfundible: ninguna prueba depende de las variables MOICA_R2_*. */
  public static final String URL_BASE = "https://imagenes.moica.test";

  private final Map<String, ObjetoGuardado> objetos = new LinkedHashMap<>();
  private final List<String> clavesGuardadas = new ArrayList<>();
  private final List<String> clavesEliminadas = new ArrayList<>();

  private boolean simularNoDisponible;
  private boolean simularUrlInutilizable;

  /** El contenido y el tipo con los que se guardó un objeto. */
  public record ObjetoGuardado(byte[] contenido, String tipoMime) {
    public ObjetoGuardado {
      contenido = contenido.clone();
    }

    @Override
    public byte[] contenido() {
      return contenido.clone();
    }
  }

  @Override
  public synchronized String guardar(String clave, byte[] contenido, String tipoMime) {
    if (simularNoDisponible) {
      throw almacenamientoNoDisponible();
    }
    objetos.put(clave, new ObjetoGuardado(contenido, tipoMime));
    clavesGuardadas.add(clave);

    String url = URL_BASE + "/" + clave;
    if (simularUrlInutilizable) {
      // Más larga que el VARCHAR(500) de las columnas de URL: la subida
      // «funciona» y es la persistencia posterior la que falla, exactamente el
      // escenario que obliga a compensar.
      return url + "x".repeat(600);
    }
    return url;
  }

  @Override
  public synchronized void eliminar(String clave) {
    if (simularNoDisponible) {
      throw almacenamientoNoDisponible();
    }
    objetos.remove(clave);
    clavesEliminadas.add(clave);
  }

  @Override
  public synchronized Optional<String> claveDe(String urlPublica) {
    String prefijo = URL_BASE + "/";
    if (urlPublica == null || !urlPublica.startsWith(prefijo)) {
      return Optional.empty();
    }
    return Optional.of(urlPublica.substring(prefijo.length()));
  }

  /** Vuelve al estado inicial. Cada prueba lo llama antes de empezar. */
  public synchronized void reiniciar() {
    objetos.clear();
    clavesGuardadas.clear();
    clavesEliminadas.clear();
    simularNoDisponible = false;
    simularUrlInutilizable = false;
  }

  /** A partir de ahora, guardar y eliminar fallan como si el proveedor no respondiera. */
  public synchronized void simularNoDisponible() {
    this.simularNoDisponible = true;
  }

  /** A partir de ahora, guardar devuelve una URL que la base de datos rechazará. */
  public synchronized void simularUrlInutilizable() {
    this.simularUrlInutilizable = true;
  }

  public synchronized boolean contiene(String clave) {
    return objetos.containsKey(clave);
  }

  public synchronized Optional<ObjetoGuardado> objeto(String clave) {
    return Optional.ofNullable(objetos.get(clave));
  }

  public synchronized int cantidadDeObjetos() {
    return objetos.size();
  }

  public synchronized List<String> clavesGuardadas() {
    return List.copyOf(clavesGuardadas);
  }

  public synchronized List<String> clavesEliminadas() {
    return List.copyOf(clavesEliminadas);
  }

  private static ErrorDeAplicacion almacenamientoNoDisponible() {
    return new ErrorDeAplicacion(
        HttpStatus.SERVICE_UNAVAILABLE,
        "ALMACENAMIENTO_NO_DISPONIBLE",
        "No pudimos procesar la imagen en este momento. Inténtalo de nuevo en unos minutos.");
  }
}
