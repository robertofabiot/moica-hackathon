package com.moica.moderacion.dto;

import com.moica.servicio.dto.DatosDeImagenDeServicio;
import com.moica.solicitud.dto.DatosDeSolicitudServicio;
import java.util.List;

/**
 * El expediente completo de un caso, tal como lo estudia quien lo revisa.
 *
 * <p>Reúne en una sola respuesta lo que hoy existe vinculado al caso y ninguna cosa más: el caso
 * con su decisión vigente, la solicitud reportada con su historial de transiciones, las imágenes
 * del servicio contratado —la única evidencia material que Moica ya guarda sobre el trato— y las
 * versiones SCD2 del propio expediente.
 *
 * <p><b>Los mensajes no viajan aquí.</b> Tienen su propia ruta dentro del caso, porque leer una
 * conversación privada entre dos personas es un acto administrativo distinto de abrir el
 * expediente: separarlo deja rastro de qué se consultó y evita que el hilo se descargue cada vez
 * que alguien mira la ficha.
 *
 * <p>No lleva correos ni contactos de los participantes, ni documentos de verificación, que tienen
 * su propia superficie autorizada. El nombre de cada persona es el mismo que ya viaja en el detalle
 * de la solicitud.
 *
 * @param puedeResolver si la sesión que consulta es la responsable y, por tanto, puede iniciar la
 *     revisión o cerrar el caso; la interfaz lo usa para no ofrecer una acción que el backend
 *     rechazaría
 * @param imagenesDelServicio vacía si el servicio contratado no tiene imágenes
 */
public record DatosDeExpedienteDeCaso(
    ResumenDeCasoAdministrativo caso,
    String descripcion,
    String resolucionActual,
    DatosDeSolicitudServicio solicitud,
    List<DatosDeImagenDeServicio> imagenesDelServicio,
    List<DatosDeVersionDeCaso> historial,
    boolean puedeResolver) {

  public DatosDeExpedienteDeCaso {
    imagenesDelServicio = List.copyOf(imagenesDelServicio);
    historial = List.copyOf(historial);
  }
}
