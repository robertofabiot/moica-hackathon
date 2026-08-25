import { Link } from 'react-router';

import { RUTA_ADMIN } from '../capacidades/admin';
import {
  ErrorDeApi,
  RUTA_INICIO_SESION,
  RUTA_REGISTRO,
  RUTA_SEGURIDAD,
  RUTA_VERIFICACION_SEGUNDO_FACTOR,
  useCierreSesion,
  useSesionActual,
} from '../capacidades/auth';
import estilos from './Inicio.module.css';

/**
 * Pantalla de inicio de Moica.
 *
 * Presenta la marca y muestra el estado de acceso: quién ha iniciado sesión, qué le falta para
 * usarla y a dónde puede ir. Las pantallas de contenido llegan con sus propios incrementos.
 */
export default function Inicio() {
  const sesion = useSesionActual();
  const cierre = useCierreSesion();
  const avisoDeCierre = mensajeDeCierreFallido(cierre.error);

  const pendienteDeSegundoFactor = sesion.data?.sesion.pendienteDeSegundoFactor === true;

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
        <>
          <div className={estilos.acceso}>
            <p className={estilos.estado}>
              Sesión iniciada como <strong>{sesion.data.usuario.nombreCompleto}</strong>
            </p>
            {pendienteDeSegundoFactor ? (
              <Link className={estilos.boton} to={RUTA_VERIFICACION_SEGUNDO_FACTOR}>
                Verificar segundo factor
              </Link>
            ) : (
              <>
                <Link className={estilos.boton} to={RUTA_SEGURIDAD}>
                  Seguridad de la cuenta
                </Link>
                {sesion.data.usuario.esAdministrador && (
                  <Link className={estilos.boton} to={RUTA_ADMIN}>
                    Área administrativa
                  </Link>
                )}
              </>
            )}
            <button
              className={estilos.boton}
              type="button"
              onClick={() => cierre.solicitarCierre()}
              disabled={cierre.isPending}
            >
              {cierre.isPending ? 'Cerrando sesión…' : 'Cerrar sesión'}
            </button>
          </div>
          {pendienteDeSegundoFactor && (
            <p className={estilos.aviso} role="status">
              Falta verificar tu segundo factor. Hasta entonces, tu sesión solo sirve para eso o
              para salir.
            </p>
          )}
          {avisoDeCierre !== null && (
            <p className={estilos.avisoDeError} role="alert">
              {avisoDeCierre}
            </p>
          )}
        </>
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

/**
 * El 401 ya se traduce en navegación a «sesión vencida»; no se pinta aquí para
 * no mostrar un error de cierre sobre una sesión que acaba de olvidarse.
 */
function mensajeDeCierreFallido(error: unknown): string | null {
  if (error instanceof ErrorDeApi && error.estado === 401) {
    return null;
  }
  return error instanceof Error ? error.message : null;
}
