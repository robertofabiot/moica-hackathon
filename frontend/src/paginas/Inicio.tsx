import { Link } from 'react-router';

import {
  RUTA_INICIO_SESION,
  RUTA_REGISTRO,
  useAvisoDeSesionVencida,
  useCierreSesion,
  useSesionActual,
} from '../capacidades/auth';
import estilos from './Inicio.module.css';

/**
 * Pantalla de inicio de Moica.
 *
 * Presenta la marca y muestra el estado de acceso: quién ha iniciado sesión o cómo hacerlo. Las
 * pantallas de contenido llegan con sus propios incrementos.
 */
export default function Inicio() {
  const sesion = useSesionActual();
  const cierre = useCierreSesion();

  useAvisoDeSesionVencida(sesion.data);

  return (
    <main className={estilos.contenedor}>
      <img
        className={estilos.logotipo}
        src="/icono-192.png"
        alt="Logotipo de Moica"
        width={96}
        height={96}
      />
      <h1 className={estilos.titulo}>Moica</h1>
      <p className={estilos.lema}>La confianza se construye entre todos</p>

      {sesion.isPending ? (
        <p className={estilos.estado}>Comprobando tu sesión…</p>
      ) : sesion.data ? (
        <div className={estilos.acceso}>
          <p className={estilos.estado}>
            Sesión iniciada como <strong>{sesion.data.usuario.nombreCompleto}</strong>
          </p>
          <button
            className={estilos.boton}
            type="button"
            onClick={() => cierre.mutate()}
            disabled={cierre.isPending}
          >
            {cierre.isPending ? 'Cerrando sesión…' : 'Cerrar sesión'}
          </button>
        </div>
      ) : (
        <div className={estilos.acceso}>
          <Link className={estilos.boton} to={RUTA_INICIO_SESION}>
            Iniciar sesión
          </Link>
          <Link className={estilos.enlace} to={RUTA_REGISTRO}>
            Crear cuenta
          </Link>
        </div>
      )}
    </main>
  );
}
