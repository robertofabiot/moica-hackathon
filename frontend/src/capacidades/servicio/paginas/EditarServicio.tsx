import { Link, useParams } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import { Boton } from '../../../comun/componentes/ui';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import EstadoDelServicio from '../componentes/EstadoDelServicio';
import FormularioDeServicio from '../componentes/FormularioDeServicio';
import ImagenesDelServicio from '../componentes/ImagenesDelServicio';
import { useServicioPropio } from '../hooks/useServiciosPropios';
import { RUTA_SERVICIOS } from '../rutas';
import MarcoDeGestionDeServicios from './MarcoDeGestionDeServicios';
import propios from './servicios.module.css';

/** Edita un servicio propio: datos, estado e imágenes. */
export default function EditarServicio() {
  const { idServicio } = useParams();
  const identificador = Number(idServicio);
  const servicio = useServicioPropio(Number.isInteger(identificador) ? identificador : undefined);

  if (servicio.isPending) {
    return (
      <MarcoDeGestionDeServicios>
        <MigasDeEdicion />
        <p className={secciones.estado} role="status">
          Cargando el servicio…
        </p>
      </MarcoDeGestionDeServicios>
    );
  }

  if (servicio.isError || servicio.data === undefined) {
    return (
      <MarcoDeGestionDeServicios>
        <MigasDeEdicion />
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
        <Boton variante="secundario" to={RUTA_SERVICIOS}>
          Volver al listado
        </Boton>
      </MarcoDeGestionDeServicios>
    );
  }

  const datos = servicio.data;

  return (
    <MarcoDeGestionDeServicios>
      <MigasDeEdicion />
      <header className={propios.cabeceraDeEdicion}>
        <div className={propios.grupoDeTitulo}>
          <h1 className={propios.tituloDeEdicion}>{datos.nombre}</h1>
          <div className={propios.filaDePildoras}>
            <span className={propios.pildoraCategoria}>{datos.nombreCategoria}</span>
            <span className={propios.pildoraCategoria}>{datos.nombreSubcategoria}</span>
          </div>
        </div>
        <Boton variante="secundario" to={RUTA_SERVICIOS}>
          Volver al listado
        </Boton>
      </header>
      <FormularioDeServicio servicio={datos} />
      <EstadoDelServicio servicio={datos} />
      <ImagenesDelServicio servicio={datos} />
    </MarcoDeGestionDeServicios>
  );
}

function MigasDeEdicion() {
  return (
    <nav className={propios.migas} aria-label="Migas de pan">
      <ol className={propios.listaDeMigas}>
        <li>
          <Link className={propios.enlaceMiga} to={RUTA_SERVICIOS}>
            Mis servicios
          </Link>
        </li>
        <li className={propios.migaActual} aria-current="page">
          Editar servicio
        </li>
      </ol>
    </nav>
  );
}
