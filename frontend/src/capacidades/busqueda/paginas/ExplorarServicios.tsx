import { useState } from 'react';
import { Link } from 'react-router';

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

/**
 * Exploración pública de servicios. No exige sesión.
 *
 * Solo el backend decide qué aparece: servicios activos de cuentas operativas, prestadores
 * disponibles y perfiles con al menos verificación básica.
 */
export default function ExplorarServicios() {
  const [borrador, setBorrador] = useState<FiltrosDeBusqueda>(FILTROS_VACIOS);
  const [aplicados, setAplicados] = useState<FiltrosDeBusqueda>(FILTROS_VACIOS);
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
          onAplicar={() => setAplicados(borrador)}
          onLimpiar={() => {
            setBorrador(FILTROS_VACIOS);
            setAplicados(FILTROS_VACIOS);
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
