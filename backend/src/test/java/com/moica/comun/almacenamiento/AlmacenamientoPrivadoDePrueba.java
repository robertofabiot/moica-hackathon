package com.moica.comun.almacenamiento;

import com.moica.comun.error.ErrorDeAplicacion;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * Doble en memoria del almacén privado de expedientes para las pruebas de integración.
 *
 * <p>Se comporta como el almacén real visto desde la aplicación: guarda bajo una clave opaca, borra
 * de forma idempotente, entrega un acceso de lectura que caduca y falla con el mismo error
 * uniforme. Lo que añade es observabilidad —qué claves se guardaron y se eliminaron y qué contiene
 * cada objeto— y dos palancas de fallo: simular un proveedor caído y hacer fallar la carga a partir
 * de un archivo concreto, que es la manera realista de provocar la compensación a mitad de un
 * expediente.
 *
 * <p>El acceso temporal se construye con la duración **configurada**, tomada de {@link
 * PropiedadesDeDocumentos}, para que una prueba pueda comprobar que el enlace sirve antes de vencer
 * y deja de servir después. Que la firma real de R2 lleve esa misma duración se comprueba aparte,
 * en {@code AlmacenamientoPrivadoR2Test}, que sí firma de verdad.
 *
 * <p>No sustituye a una comprobación contra un bucket R2 real: esa queda documentada como
 * procedimiento manual en {@code Docs/Dev/Almacenamiento.md}.
 */
public class AlmacenamientoPrivadoDePrueba implements AlmacenamientoDeDocumentosPrivados {

  /** Base propia e inconfundible: ninguna prueba depende de las variables MOICA_R2_PRIVADO_*. */
  public static final String URL_BASE = "https://privado.moica.test";

  private static final int SIN_FALLO = -1;

  private final PropiedadesDeDocumentos propiedades;
  private final Clock reloj;

  private final Map<String, ObjetoPrivado> objetos = new LinkedHashMap<>();
  private final List<String> clavesGuardadas = new ArrayList<>();
  private final List<String> clavesEliminadas = new ArrayList<>();

  private boolean simularNoDisponible;
  private int fallarDesdeLaPosicion = SIN_FALLO;

  public AlmacenamientoPrivadoDePrueba(PropiedadesDeDocumentos propiedades, Clock reloj) {
    this.propiedades = propiedades;
    this.reloj = reloj;
  }

  /** El contenido y el tipo con los que se guardó un documento. */
  public record ObjetoPrivado(byte[] contenido, String tipoMime) {
    public ObjetoPrivado {
      contenido = contenido.clone();
    }

    @Override
    public byte[] contenido() {
      return contenido.clone();
    }
  }

  @Override
  public synchronized void guardar(String clave, byte[] contenido, String tipoMime) {
    if (simularNoDisponible
        || (fallarDesdeLaPosicion != SIN_FALLO
            && clavesGuardadas.size() >= fallarDesdeLaPosicion)) {
      throw almacenamientoNoDisponible();
    }
    objetos.put(clave, new ObjetoPrivado(contenido, tipoMime));
    clavesGuardadas.add(clave);
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
  public synchronized URI accesoTemporalDeLectura(String clave) {
    if (simularNoDisponible) {
      throw almacenamientoNoDisponible();
    }
    Instant vence = Instant.now(reloj).plus(propiedades.duracionUrlTemporal());
    return URI.create(URL_BASE + "/" + clave + "?expira=" + vence.getEpochSecond());
  }

  /** Si un acceso ya entregado seguiría sirviendo en ese instante. */
  public boolean sigueVigente(URI accesoTemporal, Instant instante) {
    String consulta = accesoTemporal.getQuery();
    long vence = Long.parseLong(consulta.substring(consulta.indexOf('=') + 1));
    return instante.getEpochSecond() < vence;
  }

  /** Vuelve al estado inicial. Cada prueba lo llama antes de empezar. */
  public synchronized void reiniciar() {
    objetos.clear();
    clavesGuardadas.clear();
    clavesEliminadas.clear();
    simularNoDisponible = false;
    fallarDesdeLaPosicion = SIN_FALLO;
  }

  /** A partir de ahora, todas las operaciones fallan como si el proveedor no respondiera. */
  public synchronized void simularNoDisponible() {
    this.simularNoDisponible = true;
  }

  /**
   * Hace fallar la carga a partir del archivo indicado, contando desde cero.
   *
   * <p>Con {@code 1}, el primer documento de un expediente se guarda y el segundo falla: es el
   * escenario que obliga a retirar lo ya subido sin dejar nada apuntado en la base.
   */
  public synchronized void fallarAlGuardarDesdeLaPosicion(int posicion) {
    this.fallarDesdeLaPosicion = posicion;
  }

  public synchronized boolean contiene(String clave) {
    return objetos.containsKey(clave);
  }

  public synchronized Optional<ObjetoPrivado> objeto(String clave) {
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
        "No pudimos procesar los documentos en este momento. Inténtalo de nuevo en unos minutos.");
  }
}
