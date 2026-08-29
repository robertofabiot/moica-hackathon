import { Link, useNavigate, useParams } from 'react-router';

import secciones from '../../../comun/estilos/secciones.module.css';
import FormularioDeSolicitud from '../componentes/FormularioDeSolicitud';
import { rutaDeSolicitud } from '../rutas';
import propios from './solicitudes.module.css';

/** Formulario para solicitar un servicio publicado. */
export default function NuevaSolicitud() {
  const { idServicio } = useParams();
  const identificador = Number(idServicio);
  const navegar = useNavigate();

  if (!Number.isInteger(identificador) || identificador <= 0) {
    return (
      <main className={propios.pantalla}>
        <div className={propios.contenido}>
          <p className={secciones.estado} role="alert">
            Esa dirección no corresponde a un servicio.
          </p>
          <p className={propios.pie}>
            <Link to="/explorar">Volver a explorar</Link>
          </p>
        </div>
      </main>
    );
  }

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>Solicitar este servicio</h1>
          <p className={secciones.explicacion}>
            Cuéntale al prestador qué necesitas, dónde y cuándo te vendría bien. Los contactos
            siguen ocultos hasta que acepte.
          </p>
        </header>
        <FormularioDeSolicitud
          idServicioPublicado={identificador}
          alCrear={(idSolicitud) => navegar(rutaDeSolicitud(idSolicitud))}
        />
        <p className={propios.pie}>
          <Link to={`/explorar/servicios/${identificador}`}>Volver al servicio</Link>
        </p>
      </div>
    </main>
  );
}
