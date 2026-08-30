import { Link } from 'react-router';

import { rutaDeInicioSesion, useSesionActual } from '../../auth';
import estilos from '../../../comun/estilos/formulario.module.css';
import { cuentaEstaActiva } from '../presentacion';
import { rutaDeNuevaSolicitud } from '../rutas';
import propios from '../paginas/solicitudes.module.css';

/**
 * Invita a solicitar un servicio desde su detalle público, o explica por qué no se puede.
 */
export default function AccionDeSolicitud({
  idServicio,
  idPrestador,
  admiteContratacion,
}: {
  idServicio: number;
  idPrestador: number;
  admiteContratacion: boolean;
}) {
  const sesion = useSesionActual();
  const datos = sesion.data;
  const esPropio =
    datos != null &&
    !datos.sesion.pendienteDeSegundoFactor &&
    datos.usuario.idUsuario === idPrestador;

  if (!admiteContratacion) {
    return (
      <p className={estilos.aviso} role="status">
        Este prestador no está disponible para contratar ahora.
      </p>
    );
  }

  if (sesion.isPending) {
    return (
      <p className={estilos.aviso} role="status">
        Comprobando tu sesión…
      </p>
    );
  }

  if (!sesion.data) {
    return (
      <div className={propios.confirmacion}>
        <p className={estilos.aviso} role="status">
          Inicia sesión para enviar una solicitud. Los contactos siguen ocultos hasta que el
          prestador acepte.
        </p>
        <Link className={`${estilos.boton} ${propios.enlaceBoton}`} to={rutaDeInicioSesion()}>
          Iniciar sesión para solicitar
        </Link>
      </div>
    );
  }

  if (sesion.data.sesion.pendienteDeSegundoFactor) {
    return (
      <p className={estilos.aviso} role="status">
        Termina de verificar tu segundo factor para poder solicitar este servicio.
      </p>
    );
  }

  if (esPropio) {
    return (
      <p className={estilos.aviso} role="status">
        No puedes solicitar un servicio publicado por tu propia cuenta.
      </p>
    );
  }

  if (!cuentaEstaActiva(sesion.data.usuario.estadoCuenta)) {
    return (
      <p className={estilos.aviso} role="status">
        Tu cuenta está restringida y por ahora no puede enviar solicitudes.
      </p>
    );
  }

  return (
    <div className={propios.confirmacion}>
      <p className={estilos.aviso} role="status">
        Este prestador está disponible. Al aceptar se habilitarán el chat y los contactos; hoy solo
        queda el registro de la solicitud.
      </p>
      <Link
        className={`${estilos.boton} ${propios.enlaceBoton}`}
        to={rutaDeNuevaSolicitud(idServicio)}
      >
        Solicitar este servicio
      </Link>
    </div>
  );
}
