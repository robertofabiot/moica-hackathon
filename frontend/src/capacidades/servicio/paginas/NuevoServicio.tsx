import { Link, useNavigate } from 'react-router';

import secciones from '../../../comun/estilos/secciones.module.css';
import FormularioDeServicio from '../componentes/FormularioDeServicio';
import { RUTA_SERVICIOS, rutaDeEdicionDeServicio } from '../rutas';
import propios from './servicios.module.css';

/** Crea un servicio inactivo y lleva a editarlo para agregar imágenes o activarlo. */
export default function NuevoServicio() {
  const navegar = useNavigate();

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>Publicar un servicio</h1>
          <p className={secciones.explicacion}>
            Queda inactivo hasta que lo actives. Si tu perfil aún no está verificado, puedes
            prepararlo ahora y activarlo después.
          </p>
        </header>
        <FormularioDeServicio
          alCrear={(creado) => navegar(rutaDeEdicionDeServicio(creado.idServicioPublicado))}
        />
        <p className={propios.pie}>
          <Link to={RUTA_SERVICIOS}>Volver a tus servicios</Link>
        </p>
      </div>
    </main>
  );
}
