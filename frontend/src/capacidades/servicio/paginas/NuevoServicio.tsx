import { useNavigate } from 'react-router';

import { BarraLateral } from '../../../comun/componentes/ui';
import { RUTA_SEGURIDAD } from '../../auth';
import { RUTA_PRESTADOR } from '../../prestador';
import FormularioDeServicio from '../componentes/FormularioDeServicio';
import IndicadorDePasos from '../componentes/IndicadorDePasos';
import { rutaDeEdicionDeServicio } from '../rutas';
import propios from './servicios.module.css';

/** Crea un servicio inactivo y lleva a editarlo para agregar imágenes o activarlo. */
export default function NuevoServicio() {
  const navegar = useNavigate();

  return (
    <div className={propios.paginaAsistente}>
      <div className={propios.barraLateralDeAsistente}>
        <BarraLateral
          itemActivo="inicio"
          destinos={{
            inicio: '/',
            perfil: RUTA_PRESTADOR,
            configuracion: RUTA_SEGURIDAD,
          }}
        />
      </div>
      <main className={propios.principalAsistente}>
        <div className={propios.tarjetaAsistente}>
          <header className={propios.encabezadoAsistente}>
            <h1 className={propios.tituloAsistente}>Publicar un servicio</h1>
            <p className={propios.explicacionAsistente}>
              Queda inactivo hasta que lo actives. Si tu perfil aún no está verificado, puedes
              prepararlo ahora y activarlo después.
            </p>
          </header>
          <IndicadorDePasos pasoActual={1} />
          <FormularioDeServicio
            alCrear={(creado) => navegar(rutaDeEdicionDeServicio(creado.idServicioPublicado))}
          />
        </div>
      </main>
    </div>
  );
}
