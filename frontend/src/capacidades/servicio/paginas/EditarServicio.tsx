import { Link, useParams } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import EstadoDelServicio from '../componentes/EstadoDelServicio';
import FormularioDeServicio from '../componentes/FormularioDeServicio';
import ImagenesDelServicio from '../componentes/ImagenesDelServicio';
import { useServicioPropio } from '../hooks/useServiciosPropios';
import { RUTA_SERVICIOS } from '../rutas';
import propios from './servicios.module.css';

/** Edita un servicio propio: datos, estado e imágenes. */
export default function EditarServicio() {
  const { idServicio } = useParams();
  const identificador = Number(idServicio);
  const servicio = useServicioPropio(Number.isInteger(identificador) ? identificador : undefined);

  if (servicio.isPending) {
    return (
      <main className={propios.pantalla}>
        <p className={secciones.estado} role="status">
          Cargando el servicio…
        </p>
      </main>
    );
  }

  if (servicio.isError || servicio.data === undefined) {
    return (
      <main className={propios.pantalla}>
        <div className={propios.contenido}>
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {servicio.error instanceof ErrorDeApi
              ? servicio.error.message
              : 'No encontramos ese servicio.'}{' '}
            <button
              className={estilos.enlaceDeTexto}
              type="button"
              onClick={() => void servicio.refetch()}
            >
              Reintentar
            </button>
          </p>
          <p className={propios.pie}>
            <Link to={RUTA_SERVICIOS}>Volver a tus servicios</Link>
          </p>
        </div>
      </main>
    );
  }

  const datos = servicio.data;

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>{datos.nombre}</h1>
          <p className={secciones.explicacion}>
            {datos.nombreCategoria} · {datos.nombreSubcategoria}
          </p>
        </header>
        <FormularioDeServicio servicio={datos} />
        <EstadoDelServicio servicio={datos} />
        <ImagenesDelServicio servicio={datos} />
        <p className={propios.pie}>
          <Link to={RUTA_SERVICIOS}>Volver a tus servicios</Link>
        </p>
      </div>
    </main>
  );
}
