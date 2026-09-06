package com.moica.calificacion.service;

import com.moica.calificacion.dto.AgregadoDeCalificaciones;
import com.moica.calificacion.dto.ReputacionPorRol;
import com.moica.calificacion.entity.RolCalificado;
import com.moica.calificacion.repository.CalificacionUsuarioRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La reputación que se deriva de las calificaciones, separada por rol.
 *
 * <p>Es deliberadamente el servicio de solo lectura de la capacidad y no depende de ninguna otra:
 * lo único que necesita para calcular un promedio son las filas de {@code calificacion_usuario}.
 * Gracias a eso el descubrimiento público puede pedirle la reputación de un prestador sin que se
 * forme un ciclo entre capacidades —registrar una calificación sí necesita preguntar por la
 * solicitud, y eso vive aparte, en {@link CalificacionDeSolicitudService}—.
 *
 * <p>No hay tabla de reputación ni valor almacenado: cada consulta recorre las calificaciones. Una
 * cifra guardada podría quedar desfasada respecto de las filas que la originan, y aquí no puede.
 *
 * <p>Nunca entrega calificaciones individuales, comentarios ni identidades de quienes calificaron:
 * hacia fuera solo sale el agregado.
 */
@Service
public class ReputacionService {

  private final CalificacionUsuarioRepository calificaciones;

  public ReputacionService(CalificacionUsuarioRepository calificaciones) {
    this.calificaciones = calificaciones;
  }

  /**
   * La reputación de una persona en un rol.
   *
   * <p>Quien no ha recibido calificaciones no obtiene un error ni un cero: obtiene el agregado
   * vacío, con {@code promedio} nulo. No calificar y no ser calificado no penalizan.
   */
  @Transactional(readOnly = true)
  public ReputacionPorRol reputacionDe(Long idUsuario, RolCalificado rol) {
    return reputacionesDe(List.of(idUsuario), rol).get(idUsuario);
  }

  /**
   * La reputación como prestador de varias personas a la vez.
   *
   * <p>Lo consume el descubrimiento público, que pinta muchas tarjetas de golpe: resolver la
   * reputación tarjeta por tarjeta añadiría una consulta por tarjeta. Aquí se resuelven todas en
   * una sola consulta agrupada.
   *
   * @return un agregado por cada identificador pedido, vacío para quien todavía no tiene
   *     calificaciones
   */
  @Transactional(readOnly = true)
  public Map<Long, ReputacionPorRol> reputacionesDePrestadores(Collection<Long> idsPrestador) {
    return reputacionesDe(idsPrestador, RolCalificado.PRESTADOR);
  }

  private Map<Long, ReputacionPorRol> reputacionesDe(
      Collection<Long> idsUsuario, RolCalificado rol) {

    Set<Long> pedidos = Set.copyOf(idsUsuario);
    if (pedidos.isEmpty()) {
      return Map.of();
    }

    Map<Long, AgregadoDeCalificaciones> agregados =
        calificaciones.agregarPorCalificado(rol, pedidos).stream()
            .collect(Collectors.toMap(AgregadoDeCalificaciones::idCalificado, Function.identity()));

    // La consulta agrupada solo devuelve a quien tiene alguna calificación; el
    // resto completa el mapa con el agregado vacío, de modo que quien pregunta
    // siempre recibe una respuesta por cada identificador que pidió.
    Map<Long, ReputacionPorRol> reputaciones = new HashMap<>();
    for (Long idUsuario : pedidos) {
      AgregadoDeCalificaciones agregado = agregados.get(idUsuario);
      reputaciones.put(
          idUsuario,
          agregado == null
              ? ReputacionPorRol.sinCalificaciones(rol)
              : ReputacionPorRol.de(rol, agregado));
    }
    return reputaciones;
  }
}
