import { useState } from 'react';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { fechaLegible } from '../etiquetas';
import { useMensajesDelCaso } from '../hooks/useRevisionDeCasos';
import propios from './acciones.module.css';

/**
 * El hilo de la solicitud reportada, dentro del contexto del caso.
 *
 * Se abre a petición y no al cargar el expediente. Es una conversación privada entre dos personas:
 * que haya que pulsar para leerla deja claro que se está entrando en ella, y evita descargarla
 * cuando quien revisa solo venía a mirar el estado del caso.
 *
 * Solo se lee. El área administrativa no participa en la conversación y no existe ninguna ruta para
 * escribir en ella.
 */
export default function MensajesDelCaso({ idCaso }: { idCaso: number }) {
  const [abierto, setAbierto] = useState(false);
  const mensajes = useMensajesDelCaso(idCaso, abierto);

  return (
    <section className={secciones.seccion} aria-labelledby="mensajes">
      <h2 className={secciones.tituloDeSeccion} id="mensajes">
        Mensajes de la solicitud
      </h2>
      <p className={secciones.explicacion}>
        Es la conversación privada entre los participantes. Se consulta solo dentro de este caso.
      </p>

      <button
        className={secciones.botonSecundario}
        type="button"
        aria-expanded={abierto}
        onClick={() => setAbierto(!abierto)}
      >
        {abierto ? 'Ocultar los mensajes' : 'Ver los mensajes'}
      </button>

      {abierto && (
        <>
          {mensajes.isPending && (
            <p className={secciones.estado} role="status">
              Cargando los mensajes…
            </p>
          )}

          {mensajes.isError && (
            <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
              {mensajes.error instanceof ErrorDeApi
                ? mensajes.error.message
                : 'No pudimos cargar los mensajes.'}
            </p>
          )}

          {mensajes.data !== undefined &&
            (mensajes.data.length === 0 ? (
              <p className={secciones.vacio}>Los participantes no se escribieron.</p>
            ) : (
              <ol className={propios.hilo}>
                {mensajes.data.map((mensaje) => (
                  <li key={mensaje.idMensajeSolicitud} className={propios.mensaje}>
                    <p className={propios.remitente}>
                      {mensaje.nombreRemitente}
                      <span className={propios.instante}>{fechaLegible(mensaje.fechaEnvio)}</span>
                    </p>
                    <p className={propios.contenido}>{mensaje.contenido}</p>
                  </li>
                ))}
              </ol>
            ))}
        </>
      )}
    </section>
  );
}
