import { useCallback, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router';

import { BarraLateral, IconoMensaje } from '../../../comun/componentes/ui';
import { RUTA_SEGURIDAD, useSesionActual } from '../../auth';
import { RUTA_PRESTADOR } from '../../prestador';
import ConversacionActiva from '../componentes/ConversacionActiva';
import ListaDeConversaciones, {
  type UltimoMensajeDeBandeja,
} from '../componentes/ListaDeConversaciones';
import { useSolicitudesEnviadas, useSolicitudesRecibidas } from '../hooks/useSolicitudes';
import { conversacionesDeBandeja } from '../presentacion';
import { RUTA_MENSAJES } from '../rutas';
import type { MensajeSolicitud } from '../tipos';
import estilos from './mensajes.module.css';

const DESTINOS_DE_BARRA = {
  inicio: '/panel',
  mensajes: RUTA_MENSAJES,
  perfil: RUTA_PRESTADOR,
  configuracion: RUTA_SEGURIDAD,
};

/**
 * Pantalla dedicada de mensajería: bandeja a la izquierda y hilo a la derecha.
 *
 * Las conversaciones salen de las solicitudes enviadas y recibidas que ya admiten chat
 * (aceptadas o cerradas en solo lectura). El hilo se pide con el mismo short polling
 * del detalle de la solicitud.
 */
export default function Mensajes() {
  const sesion = useSesionActual();
  const idUsuario = sesion.data?.usuario.idUsuario;
  const enviadas = useSolicitudesEnviadas();
  const recibidas = useSolicitudesRecibidas();
  const [parametros, setParametros] = useSearchParams();
  const [ultimosMensajes, setUltimosMensajes] = useState<
    Readonly<Record<number, UltimoMensajeDeBandeja>>
  >({});

  const conversaciones = useMemo(
    () => conversacionesDeBandeja(enviadas.data, recibidas.data),
    [enviadas.data, recibidas.data]
  );

  const idSeleccionado = idSeleccionadoDeParametros(parametros);
  const haySeleccion = idSeleccionado !== undefined;
  const resumenSeleccionado = conversaciones.find(
    (item) => item.idSolicitudServicio === idSeleccionado
  );

  const cargando = enviadas.isPending || recibidas.isPending;
  const error = errorDeBandeja(enviadas.error, recibidas.error);

  const recordarUltimo = useCallback((idSolicitud: number, mensaje: MensajeSolicitud) => {
    setUltimosMensajes((previos) => {
      const actual = previos[idSolicitud];
      if (
        actual?.contenido === mensaje.contenido &&
        actual.fechaEnvio === mensaje.fechaEnvio
      ) {
        return previos;
      }
      return {
        ...previos,
        [idSolicitud]: { contenido: mensaje.contenido, fechaEnvio: mensaje.fechaEnvio },
      };
    });
  }, []);

  function seleccionar(idSolicitud: number) {
    setParametros({ solicitud: String(idSolicitud) });
  }

  function volverALaBandeja() {
    setParametros({});
  }

  function reintentar() {
    void enviadas.refetch();
    void recibidas.refetch();
  }

  return (
    <div className={estilos.pagina}>
      <div className={estilos.barraLateral}>
        <BarraLateral itemActivo="mensajes" destinos={DESTINOS_DE_BARRA} />
      </div>
      <main
        className={unirClases(estilos.principal, haySeleccion ? estilos.conSeleccion : undefined)}
      >
        <div className={estilos.columnaBandeja}>
          <ListaDeConversaciones
            conversaciones={conversaciones}
            idUsuario={idUsuario}
            idSeleccionado={idSeleccionado}
            ultimosMensajes={ultimosMensajes}
            cargando={cargando}
            error={error}
            alReintentar={reintentar}
            alSeleccionar={seleccionar}
          />
        </div>
        <div className={estilos.columnaConversacion}>
          {idSeleccionado !== undefined ? (
            <ConversacionActiva
              key={idSeleccionado}
              idSolicitud={idSeleccionado}
              resumen={resumenSeleccionado}
              idUsuario={idUsuario}
              alVolver={volverALaBandeja}
              alUltimoMensaje={recordarUltimo}
            />
          ) : (
            <div className={estilos.vacio}>
              <span className={estilos.iconoVacio}>
                <IconoMensaje />
              </span>
              <p className={estilos.textoVacio}>
                Selecciona una conversación para ver los mensajes
              </p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

export function idSeleccionadoDeParametros(parametros: URLSearchParams): number | undefined {
  const crudo = parametros.get('solicitud');
  if (crudo === null || crudo === '') {
    return undefined;
  }
  const id = Number(crudo);
  return Number.isInteger(id) && id > 0 ? id : undefined;
}

function errorDeBandeja(enviadas: Error | null, recibidas: Error | null): Error | null {
  return enviadas ?? recibidas;
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
