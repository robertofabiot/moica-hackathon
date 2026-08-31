import { useSesionActual } from '../../auth';
import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { useContactosRevelados } from '../hooks/useChat';
import { puedeVerContactos } from '../presentacion';
import type { DatosDeSolicitudServicio } from '../tipos';
import chat from './chat.module.css';

/**
 * Los contactos externos del prestador, revelados al cliente después de la aceptación.
 *
 * Solo se monta para el cliente participante de una solicitud que llegó a estar aceptada; para
 * cualquier otra persona ni siquiera se pide el recurso. Quien autoriza de verdad es el backend,
 * que responde 404 al prestador y a un tercero.
 *
 * Cada entrada se pinta como **texto**, nunca como enlace: es un campo libre que escribió otra
 * persona, y convertirlo en un enlace automático sería ofrecer un destino que Moica no revisó.
 */
export default function ContactosDelPrestador({
  solicitud,
}: {
  solicitud: DatosDeSolicitudServicio;
}) {
  const sesion = useSesionActual();
  const revelados = puedeVerContactos(solicitud, sesion.data?.usuario.idUsuario);
  const contactos = useContactosRevelados(solicitud.idSolicitudServicio, revelados);

  if (!revelados) {
    return null;
  }

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-contactos">
      <h2 className={secciones.tituloDeSeccion} id="titulo-contactos">
        Contactos de {solicitud.nombrePublicoPrestador}
      </h2>
      <p className={secciones.explicacion}>
        Se muestran porque el prestador aceptó tu solicitud. Son las entradas que publicó en su
        perfil; Moica no verifica a dónde llevan.
      </p>

      {contactos.isPending ? (
        <p className={secciones.estado} role="status">
          Cargando los contactos…
        </p>
      ) : null}

      {contactos.isError ? (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {contactos.error instanceof ErrorDeApi
            ? contactos.error.message
            : 'No pudimos cargar los contactos.'}{' '}
          <button
            className={estilos.enlaceDeTexto}
            type="button"
            onClick={() => void contactos.refetch()}
          >
            Reintentar
          </button>
        </p>
      ) : null}

      {contactos.data !== undefined && contactos.data.length === 0 ? (
        <p className={secciones.vacio}>
          Este prestador todavía no publicó ningún medio de contacto. Puedes coordinar el trabajo
          por los mensajes de esta solicitud.
        </p>
      ) : null}

      {contactos.data !== undefined && contactos.data.length > 0 ? (
        <ul className={chat.contactos} aria-label="Medios de contacto del prestador">
          {contactos.data.map((contacto) => (
            <li className={chat.contacto} key={contacto.idMedioContactoPrestador}>
              {contacto.contenido}
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}
