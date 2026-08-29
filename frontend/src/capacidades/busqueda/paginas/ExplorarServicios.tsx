import { useState } from 'react';
import { Link, useSearchParams } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import FiltrosPublicos from '../componentes/FiltrosPublicos';
import TarjetaDeServicio from '../componentes/TarjetaDeServicio';
import { useServiciosPublicos } from '../hooks/useBusquedaPublica';
import type { FiltrosDeBusqueda } from '../tipos';
import propios from './explorar.module.css';
import lista from '../componentes/tarjeta.module.css';

const FILTROS_VACIOS: FiltrosDeBusqueda = {
  texto: '',
  idCategoria: '',
  idSubcategoria: '',
  idMunicipio: '',
};

function filtrosDesdeParametros(params: URLSearchParams): FiltrosDeBusqueda {
  return {
    texto: params.get('texto') ?? '',
    idCategoria: params.get('idCategoria') ?? '',
    idSubcategoria: params.get('idSubcategoria') ?? '',
    idMunicipio: params.get('idMunicipio') ?? '',
  };
}

function parametrosDesdeFiltros(filtros: FiltrosDeBusqueda): Record<string, string> {
  const parametros: Record<string, string> = {};
  const texto = filtros.texto.trim();
  if (texto !== '') {
    parametros.texto = texto;
  }
  if (filtros.idCategoria !== '') {
    parametros.idCategoria = filtros.idCategoria;
  }
  if (filtros.idSubcategoria !== '') {
    parametros.idSubcategoria = filtros.idSubcategoria;
  }
  if (filtros.idMunicipio !== '') {
    parametros.idMunicipio = filtros.idMunicipio;
  }
  return parametros;
}

/**
 * Exploración pública de servicios. No exige sesión.
 *
 * Solo el backend decide qué aparece: servicios activos de cuentas operativas, prestadores
 * disponibles y perfiles con al menos verificación básica. Los filtros iniciales salen de la
 * URL para que el hero de la portada pueda abrir esta pantalla con un texto.
 */
export default function ExplorarServicios() {
  const [parametros, setParametros] = useSearchParams();
  const aplicados = filtrosDesdeParametros(parametros);
  const [borrador, setBorrador] = useState<FiltrosDeBusqueda>(aplicados);
  const resultados = useServiciosPublicos(aplicados);

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>Explorar servicios</h1>
          <p className={secciones.explicacion}>
            Puedes mirar sin registrarte. Contratar llega más adelante: aquí solo descubres quién
            ofrece qué en Managua.
          </p>
        </header>

        <FiltrosPublicos
          filtros={borrador}
          onCambiar={setBorrador}
          onAplicar={() => setParametros(parametrosDesdeFiltros(borrador), { replace: true })}
          onLimpiar={() => {
            setBorrador(FILTROS_VACIOS);
            setParametros({}, { replace: true });
          }}
        />

        {resultados.isPending && (
          <p className={secciones.estado} role="status">
            Buscando servicios…
          </p>
        )}

        {resultados.isError && (
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {resultados.error instanceof ErrorDeApi
              ? resultados.error.message
              : 'No pudimos cargar los servicios.'}{' '}
            <button
              className={estilos.enlaceDeTexto}
              type="button"
              onClick={() => void resultados.refetch()}
            >
              Reintentar
            </button>
          </p>
        )}

        {resultados.data !== undefined && resultados.data.length === 0 && (
          <p className={secciones.vacio}>
            No hay servicios que coincidan con esos filtros. Prueba otra combinación.
          </p>
        )}

        {resultados.data !== undefined && resultados.data.length > 0 && (
          <ul className={lista.lista}>
            {resultados.data.map((servicio) => (
              <TarjetaDeServicio key={servicio.idServicioPublicado} servicio={servicio} />
            ))}
          </ul>
        )}

        <p className={propios.pie}>
          <Link to="/">Volver al inicio</Link>
        </p>
      </div>
    </main>
  );
}
