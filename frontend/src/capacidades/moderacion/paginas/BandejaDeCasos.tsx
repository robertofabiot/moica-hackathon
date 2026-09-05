import { useState } from 'react';
import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { fechaLegible, nombreDelEstado, nombreDelResultado } from '../etiquetas';
import { useBandejaDeCasos } from '../hooks/useRevisionDeCasos';
import { rutaDeExpediente } from '../rutas';
import type { EstadoDeCaso, FiltroDeBandeja } from '../tipos';
import propios from './casos.module.css';

/**
 * La bandeja de casos de moderación del área administrativa.
 *
 * Por omisión muestra lo que espera decisión, que es el trabajo pendiente. Los cerrados se piden a
 * propósito: sirven para consultar una decisión anterior, no para trabajarla.
 *
 * La pantalla ordena y elige; quién puede hacer qué lo decide el backend en cada petición. Llegar
 * hasta aquí sin rol administrativo o sin el segundo factor verificado ya devuelve 403.
 */
export default function BandejaDeCasos() {
  const [filtro, setFiltro] = useState<FiltroDeBandeja>({
    estados: ['ABIERTO', 'EN_REVISION', 'REABIERTO'],
    soloMios: false,
  });

  const bandeja = useBandejaDeCasos(filtro);

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>Casos de moderación</h1>
          <p className={secciones.explicacion}>
            Cada caso lo revisa y lo resuelve una persona. Moica no sanciona por su cuenta ni elige
            medidas a partir de la reincidencia.
          </p>
        </header>

        <Filtros filtro={filtro} alCambiar={setFiltro} />

        {bandeja.isPending && (
          <p className={secciones.estado} role="status">
            Cargando la bandeja…
          </p>
        )}

        {bandeja.isError && (
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {bandeja.error instanceof ErrorDeApi
              ? bandeja.error.message
              : 'No pudimos cargar la bandeja de casos.'}{' '}
            <button
              className={estilos.enlaceDeTexto}
              type="button"
              onClick={() => void bandeja.refetch()}
            >
              Reintentar
            </button>
          </p>
        )}

        {bandeja.data !== undefined &&
          (bandeja.data.length === 0 ? (
            <p className={secciones.vacio}>No hay casos con estos filtros.</p>
          ) : (
            <div className={propios.tablaDesplazable}>
              <table className={propios.tabla}>
                <caption className={secciones.explicacion}>
                  Casos de moderación, del más antiguo al más reciente.
                </caption>
                <thead>
                  <tr>
                    <th scope="col">Caso</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Responsable</th>
                  </tr>
                </thead>
                <tbody>
                  {bandeja.data.map((caso) => (
                    <tr key={caso.idCasoModeracion}>
                      {/*
                        El motivo, las personas y la fecha acompañan al enlace en
                        lugar de ocupar columnas propias: con seis columnas la
                        tabla no cabe en un teléfono y las palabras se parten a
                        la mitad. Con tres se lee entera en 375 px.
                      */}
                      <th scope="row" className={propios.celdaDelCaso}>
                        <Link
                          className={propios.enlaceDelCaso}
                          to={rutaDeExpediente(caso.idCasoModeracion)}
                        >
                          {caso.motivo}
                        </Link>
                        <span className={propios.detalleDeLaFila}>
                          {caso.nombreReportante} reportó a {caso.nombreReportado} · Abierto el{' '}
                          {fechaLegible(caso.fechaApertura)}
                        </span>
                      </th>
                      <td>
                        {nombreDelEstado(caso.estadoActual)}
                        {caso.resultadoActual !== null && (
                          <span className={propios.detalleDeLaFila}>
                            {nombreDelResultado(caso.resultadoActual)}
                          </span>
                        )}
                      </td>
                      <td>
                        {caso.nombreAdministradorResponsable ?? (
                          <span className={propios.sinAsignar}>Sin asignar</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}

        <p className={propios.pie}>
          <Link to="/admin">Volver al área administrativa</Link>
        </p>
      </div>
    </main>
  );
}

const ESTADOS_PENDIENTES: EstadoDeCaso[] = ['ABIERTO', 'EN_REVISION', 'REABIERTO'];
const ESTADOS_CERRADOS: EstadoDeCaso[] = ['CERRADO'];

/**
 * Los dos ejes por los que se acota la bandeja: en qué etapa está el caso y de quién es.
 *
 * Son botones y no un formulario porque no hay nada que enviar: cambiar el filtro es cambiar lo que
 * se está mirando. `aria-pressed` dice cuál está activo a quien no ve el estilo.
 */
function Filtros({
  filtro,
  alCambiar,
}: {
  filtro: FiltroDeBandeja;
  alCambiar: (siguiente: FiltroDeBandeja) => void;
}) {
  const mostrandoPendientes = filtro.estados.length > 1;

  return (
    <div className={propios.filtros} role="group" aria-label="Filtros de la bandeja">
      <button
        className={secciones.botonSecundario}
        type="button"
        aria-pressed={mostrandoPendientes}
        onClick={() => alCambiar({ ...filtro, estados: ESTADOS_PENDIENTES })}
      >
        Esperando decisión
      </button>
      <button
        className={secciones.botonSecundario}
        type="button"
        aria-pressed={!mostrandoPendientes}
        onClick={() => alCambiar({ ...filtro, estados: ESTADOS_CERRADOS })}
      >
        Cerrados
      </button>
      <button
        className={secciones.botonSecundario}
        type="button"
        aria-pressed={filtro.soloMios}
        onClick={() => alCambiar({ ...filtro, soloMios: !filtro.soloMios })}
      >
        Solo los míos
      </button>
    </div>
  );
}
