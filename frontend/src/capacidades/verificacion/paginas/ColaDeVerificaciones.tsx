import { useState } from 'react';
import { Link } from 'react-router';

import { useSesionActual } from '../../auth';
import { ErrorDeApi } from '../../../comun/api';
import { Boton } from '../../../comun/componentes/ui';
import ExpedienteEnRevision from '../componentes/ExpedienteEnRevision';
import { fechaLegible, nombreDelEstado, nombreDelNivelSolicitado } from '../etiquetas';
import { useColaDeVerificaciones } from '../hooks/useRevisionDeVerificaciones';
import type { EstadoDeSolicitud, FiltroDeCola, NivelSolicitado } from '../tipos';
import propios from './revision.module.css';

/**
 * La cola de verificaciones documentales del área administrativa.
 *
 * Por omisión muestra lo que espera decisión, que es el trabajo pendiente. Los estados resueltos se
 * piden a propósito: de las aprobadas es de donde se revoca, y las rechazadas y revocadas sirven
 * para seguir la traza de un caso.
 *
 * La pantalla ordena y elige; quién puede hacer qué lo decide el backend en cada petición. Llegar
 * hasta aquí sin rol administrativo o sin el segundo factor verificado ya devuelve 403.
 */
export default function ColaDeVerificaciones() {
  const sesion = useSesionActual();
  const [filtro, setFiltro] = useState<FiltroDeCola>({
    estados: ['PENDIENTE', 'EN_REVISION'],
    nivel: null,
  });
  const [seleccionada, setSeleccionada] = useState<number | null>(null);

  const cola = useColaDeVerificaciones(filtro);
  const expediente =
    cola.data?.find((fila) => fila.idSolicitudVerificacion === seleccionada) ?? null;

  const cambiarFiltro = (siguiente: FiltroDeCola) => {
    setFiltro(siguiente);
    setSeleccionada(null);
  };

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>Verificaciones documentales</h1>
          <p className={propios.explicacion}>
            Toda solicitud la resuelve una persona. Moica no aprueba, rechaza ni revoca nada por su
            cuenta.
          </p>
        </header>

        <Filtros filtro={filtro} alCambiar={cambiarFiltro} />

        {cola.isPending && (
          <p className={propios.estado} role="status">
            Cargando la cola…
          </p>
        )}

        {cola.isError && (
          <p className={propios.avisoDeError} role="alert">
            {cola.error instanceof ErrorDeApi
              ? cola.error.message
              : 'No pudimos cargar la cola de verificaciones.'}{' '}
            <button
              className={propios.enlaceDeTexto}
              type="button"
              onClick={() => void cola.refetch()}
            >
              Reintentar
            </button>
          </p>
        )}

        {cola.data !== undefined &&
          (cola.data.length === 0 ? (
            <p className={propios.vacio}>No hay solicitudes con estos filtros.</p>
          ) : (
            <div className={propios.tablaDesplazable}>
              <table className={propios.tabla}>
                <caption className={propios.explicacion}>
                  Solicitudes de verificación, de la más antigua a la más reciente.
                </caption>
                <thead>
                  <tr>
                    <th scope="col">Prestador</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Revisión</th>
                  </tr>
                </thead>
                <tbody>
                  {cola.data.map((fila) => (
                    <tr
                      key={fila.idSolicitudVerificacion}
                      className={
                        seleccionada === fila.idSolicitudVerificacion
                          ? propios.filaActiva
                          : undefined
                      }
                    >
                      <th scope="row" className={propios.celdaDePrestador}>
                        <span className={propios.nombrePublico}>
                          {fila.prestador.nombrePublico}
                        </span>
                        <span className={propios.detalleDeLaFila}>
                          <span className={propios.pildoraDeNivel}>
                            {nombreDelNivelSolicitado(fila.nivelSolicitado)}
                          </span>
                          Enviada el {fechaLegible(fila.fechaSolicitud)}
                        </span>
                      </th>
                      <td>
                        <span
                          className={`${propios.pildoraDeEstado} ${claseDePildora(fila.estadoSolicitud)}`}
                        >
                          {nombreDelEstado(fila.estadoSolicitud)}
                        </span>
                      </td>
                      <td>
                        <Boton
                          className={propios.botonDeFila}
                          variante="secundario"
                          type="button"
                          onClick={() =>
                            setSeleccionada(
                              seleccionada === fila.idSolicitudVerificacion
                                ? null
                                : fila.idSolicitudVerificacion
                            )
                          }
                          aria-expanded={seleccionada === fila.idSolicitudVerificacion}
                          aria-label={
                            seleccionada === fila.idSolicitudVerificacion
                              ? `Cerrar el expediente de ${fila.prestador.nombrePublico}`
                              : `Abrir el expediente de ${fila.prestador.nombrePublico}`
                          }
                        >
                          {seleccionada === fila.idSolicitudVerificacion ? 'Cerrar' : 'Abrir'}
                        </Boton>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}

        {expediente !== null && (
          <ExpedienteEnRevision
            expediente={expediente}
            idAdministrador={sesion.data?.usuario.idUsuario ?? null}
            alResolver={() => void cola.refetch()}
          />
        )}

        <p className={propios.pie}>
          <Link className={propios.enlaceDePie} to="/admin">
            Volver al área administrativa
          </Link>
        </p>
      </div>
    </main>
  );
}

/** Los dos filtros de la cola, cada uno con su etiqueta y utilizable con el teclado. */
function Filtros({
  filtro,
  alCambiar,
}: {
  filtro: FiltroDeCola;
  alCambiar: (filtro: FiltroDeCola) => void;
}) {
  return (
    <div className={propios.filtros}>
      <div className={propios.filtro}>
        <label className={propios.etiqueta} htmlFor="filtro-estado">
          Estado
        </label>
        <select
          className={propios.selector}
          id="filtro-estado"
          value={filtro.estados.join(',')}
          onChange={(evento) =>
            alCambiar({
              ...filtro,
              estados: evento.target.value.split(',') as EstadoDeSolicitud[],
            })
          }
        >
          <option value="PENDIENTE,EN_REVISION">Esperando decisión</option>
          <option value="APROBADA">Aprobadas</option>
          <option value="RECHAZADA">Rechazadas</option>
          <option value="REVOCADA">Revocadas</option>
        </select>
      </div>

      <div className={propios.filtro}>
        <label className={propios.etiqueta} htmlFor="filtro-nivel">
          Nivel
        </label>
        <select
          className={propios.selector}
          id="filtro-nivel"
          value={filtro.nivel ?? ''}
          onChange={(evento) =>
            alCambiar({
              ...filtro,
              nivel: evento.target.value === '' ? null : (evento.target.value as NivelSolicitado),
            })
          }
        >
          <option value="">Todos los niveles</option>
          <option value="BASICA">Verificación básica</option>
          <option value="PROFESIONAL">Verificación profesional</option>
        </select>
      </div>
    </div>
  );
}

function claseDePildora(estado: EstadoDeSolicitud): string | undefined {
  switch (estado) {
    case 'PENDIENTE':
      return propios.pildoraPendiente;
    case 'EN_REVISION':
      return propios.pildoraEnRevision;
    case 'APROBADA':
      return propios.pildoraAprobada;
    case 'RECHAZADA':
    case 'REVOCADA':
      return propios.pildoraRechazada;
    default:
      return undefined;
  }
}
