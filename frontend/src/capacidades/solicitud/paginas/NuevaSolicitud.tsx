import { Link, useNavigate, useParams } from 'react-router';

import { useSesionActual } from '../../auth';
import { IconoChevronIzquierda } from '../../../comun/componentes/ui';
import FormularioDeSolicitud from '../componentes/FormularioDeSolicitud';
import { cuentaEstaActiva } from '../presentacion';
import { rutaDeSolicitud } from '../rutas';
import MarcoDeSolicitudes from './MarcoDeSolicitudes';
import propios from './solicitudes.module.css';

/** Formulario para solicitar un servicio publicado. */
export default function NuevaSolicitud() {
  const { idServicio } = useParams();
  const identificador = Number(idServicio);
  const navegar = useNavigate();
  const sesion = useSesionActual();
  const identificadorValido = Number.isInteger(identificador) && identificador > 0;

  if (!identificadorValido) {
    return (
      <MarcoDeSolicitudes>
        <p className={propios.vacio} role="alert">
          Esa dirección no corresponde a un servicio.
        </p>
        <p className={propios.pie}>
          <Link className={propios.enlaceVolver} to="/explorar">
            <IconoChevronIzquierda />
            Volver a explorar
          </Link>
        </p>
      </MarcoDeSolicitudes>
    );
  }

  if (sesion.isPending) {
    return (
      <MarcoDeSolicitudes>
        <p className={propios.vacio} role="status">
          Comprobando tu sesión…
        </p>
      </MarcoDeSolicitudes>
    );
  }

  if (!cuentaEstaActiva(sesion.data?.usuario.estadoCuenta)) {
    return (
      <MarcoDeSolicitudes>
        <p className={propios.vacio} role="status">
          Tu cuenta está restringida y por ahora no puede enviar solicitudes.
        </p>
        <p className={propios.pie}>
          <Link className={propios.enlaceVolver} to={`/explorar/servicios/${identificador}`}>
            <IconoChevronIzquierda />
            Volver al servicio
          </Link>
        </p>
      </MarcoDeSolicitudes>
    );
  }

  return (
    <MarcoDeSolicitudes>
      <article className={propios.tarjetaFormulario}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>Solicitar este servicio</h1>
          <p className={propios.explicacion}>
            Cuéntale al prestador qué necesitas, dónde y cuándo te vendría bien. Los contactos
            siguen ocultos hasta que acepte.
          </p>
        </header>
        <FormularioDeSolicitud
          idServicioPublicado={identificador}
          alCrear={(idSolicitud) => navegar(rutaDeSolicitud(idSolicitud))}
        />
        <p className={propios.pie}>
          <Link className={propios.enlaceVolver} to={`/explorar/servicios/${identificador}`}>
            <IconoChevronIzquierda />
            Volver al servicio
          </Link>
        </p>
      </article>
    </MarcoDeSolicitudes>
  );
}
