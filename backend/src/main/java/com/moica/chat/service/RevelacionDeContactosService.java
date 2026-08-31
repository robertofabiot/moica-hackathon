package com.moica.chat.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.prestador.dto.ContactoRevelado;
import com.moica.prestador.service.MedioContactoService;
import com.moica.solicitud.dto.ParticipacionEnSolicitud;
import com.moica.solicitud.service.SolicitudServicioService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La revelación de los contactos externos del prestador al cliente que lo contrató.
 *
 * <p>Es una superficie propia y no un campo más del detalle de la solicitud: así los contactos no
 * viajan por descuido en las bandejas, en el descubrimiento público ni en el perfil, y la única
 * forma de obtenerlos es pedir expresamente este recurso y superar sus tres condiciones.
 *
 * <p>Esas condiciones son:
 *
 * <ul>
 *   <li>Quien pregunta participa en la solicitud. Si no, 404: un tercero no puede confirmar
 *       siquiera que exista.
 *   <li>Quien pregunta es el **cliente**. El prestador recibe también 404 —la revelación pertenece
 *       al cliente, y sus propios contactos los administra en {@code /api/prestador/contactos}—, de
 *       modo que este recurso responde 200 a una sola persona.
 *   <li>La solicitud llegó a estar aceptada. Aceptar es lo que revela los contactos, y cancelar o
 *       completar después no los vuelve a ocultar: el compromiso existió.
 * </ul>
 *
 * <p>Lo que se entrega son solo las entradas libres que el prestador configuró como {@code
 * MedioContactoPrestador}. Nunca el correo de la cuenta ni ningún dato tomado de la autenticación.
 */
@Service
public class RevelacionDeContactosService {

  private final SolicitudServicioService solicitudes;
  private final MedioContactoService contactos;

  public RevelacionDeContactosService(
      SolicitudServicioService solicitudes, MedioContactoService contactos) {
    this.solicitudes = solicitudes;
    this.contactos = contactos;
  }

  /**
   * Los contactos del prestador, en su orden de visualización, para el cliente participante.
   *
   * <p>Una lista vacía es una respuesta legítima: significa que el prestador no configuró ningún
   * medio de contacto, no que falte permiso.
   *
   * @throws ErrorDeAplicacion 404 si quien pregunta no es el cliente participante; 409 {@code
   *     CONTACTOS_NO_REVELADOS} si la solicitud nunca llegó a aceptarse
   */
  @Transactional(readOnly = true)
  public List<ContactoRevelado> revelar(UsuarioAutenticado sujeto, Long idSolicitudServicio) {
    ParticipacionEnSolicitud participacion =
        solicitudes.participacionDe(sujeto, idSolicitudServicio);

    if (!participacion.esCliente(sujeto.idUsuario())) {
      throw new ErrorDeAplicacion(
          HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "Esa solicitud no existe.");
    }
    if (!participacion.llegoAAceptada()) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "CONTACTOS_NO_REVELADOS",
          "Los contactos del prestador se revelan cuando acepta la solicitud.");
    }

    return contactos.revelarContactosDe(participacion.idPrestador());
  }
}
