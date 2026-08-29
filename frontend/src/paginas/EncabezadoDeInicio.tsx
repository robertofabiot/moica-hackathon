import { useState } from 'react';
import { Link, useNavigate } from 'react-router';

import logoHorizontal from '../assets/logos/moica-horizontal.png';
import { RUTA_ADMIN } from '../capacidades/admin';
import {
  RUTA_INICIO_SESION,
  RUTA_REGISTRO,
  RUTA_SEGURIDAD,
  RUTA_VERIFICACION_SEGUNDO_FACTOR,
  useCierreSesion,
  useSesionActual,
} from '../capacidades/auth';
import { RUTA_EXPLORAR } from '../capacidades/busqueda';
import { RUTA_PRESTADOR } from '../capacidades/prestador';
import { RUTA_SERVICIOS } from '../capacidades/servicio';
import { ErrorDeApi } from '../comun/api';
import { Boton } from '../comun/componentes/ui';
import estilos from './EncabezadoDeInicio.module.css';

const ENLACES_DE_NAVEGACION = [
  { etiqueta: 'Explorar', destino: RUTA_EXPLORAR },
  { etiqueta: 'Cómo funciona', destino: '#como-funciona' },
  { etiqueta: 'Para empresas', destino: '#para-empresas' },
  { etiqueta: 'Sobre Moica', destino: '#sobre-moica' },
] as const;

/**
 * Navegación superior de la aterrizaje.
 *
 * Sin sesión muestra entrar y registrarse. Con sesión, el encabezado solo
 * saluda por el primer nombre; el resto de la cuenta queda en un menú.
 */
export function EncabezadoDeInicio() {
  const sesion = useSesionActual();
  const cierre = useCierreSesion();
  const avisoDeCierre = mensajeDeCierreFallido(cierre.error);
  const pendienteDeSegundoFactor = sesion.data?.sesion.pendienteDeSegundoFactor === true;

  return (
    <>
      <header className={estilos.encabezado}>
        <Link className={estilos.marca} to="/">
          <img className={estilos.logotipo} src={logoHorizontal} alt="Logotipo de Moica" />
        </Link>

        <nav className={estilos.navegacion} aria-label="Navegación principal">
          <ul className={estilos.listaNav}>
            {ENLACES_DE_NAVEGACION.map((enlace) => (
              <li key={enlace.etiqueta}>
                {enlace.destino.startsWith('#') ? (
                  <a className={estilos.enlaceNav} href={enlace.destino}>
                    {enlace.etiqueta}
                  </a>
                ) : (
                  <Link className={estilos.enlaceNav} to={enlace.destino}>
                    {enlace.etiqueta}
                  </Link>
                )}
              </li>
            ))}
          </ul>
        </nav>

        <div className={estilos.acciones}>
          {sesion.isPending ? (
            <p className={estilos.estado}>Comprobando tu sesión…</p>
          ) : sesion.data ? (
            <AccionesConSesion
              nombreCompleto={sesion.data.usuario.nombreCompleto}
              esAdministrador={sesion.data.usuario.esAdministrador}
              pendienteDeSegundoFactor={pendienteDeSegundoFactor}
              cerrando={cierre.isPending}
              alCerrar={() => cierre.solicitarCierre()}
            />
          ) : (
            <AccionesSinSesion />
          )}
        </div>
      </header>

      {pendienteDeSegundoFactor && (
        <p className={estilos.aviso} role="status">
          Falta verificar tu segundo factor. Hasta entonces, tu sesión solo sirve para eso o para
          salir.
        </p>
      )}
      {avisoDeCierre !== null && (
        <p className={estilos.avisoDeError} role="alert">
          {avisoDeCierre}
        </p>
      )}
    </>
  );
}

function AccionesSinSesion() {
  const navegar = useNavigate();

  return (
    <>
      <Boton variante="secundario" onClick={() => navegar(RUTA_INICIO_SESION)}>
        Iniciar sesión
      </Boton>
      <Boton onClick={() => navegar(RUTA_REGISTRO)}>Regístrate</Boton>
    </>
  );
}

function AccionesConSesion({
  nombreCompleto,
  esAdministrador,
  pendienteDeSegundoFactor,
  cerrando,
  alCerrar,
}: {
  nombreCompleto: string;
  esAdministrador: boolean;
  pendienteDeSegundoFactor: boolean;
  cerrando: boolean;
  alCerrar: () => void;
}) {
  const [menuAbierto, setMenuAbierto] = useState(false);
  const primerNombre = primerNombreDe(nombreCompleto);

  return (
    <div className={estilos.menuDeSesion}>
      <button
        type="button"
        className={estilos.saludo}
        aria-expanded={menuAbierto}
        aria-haspopup="true"
        onClick={() => setMenuAbierto((abierto) => !abierto)}
      >
        Hola, {primerNombre}
      </button>
      {menuAbierto ? (
        <ul className={estilos.panelDeSesion}>
          {pendienteDeSegundoFactor ? (
            <li>
              <Link className={estilos.opcionDeSesion} to={RUTA_VERIFICACION_SEGUNDO_FACTOR}>
                Verificar segundo factor
              </Link>
            </li>
          ) : (
            <>
              <li>
                <Link className={estilos.opcionDeSesion} to={RUTA_PRESTADOR}>
                  Mi perfil de prestador
                </Link>
              </li>
              <li>
                <Link className={estilos.opcionDeSesion} to={RUTA_SERVICIOS}>
                  Mis servicios
                </Link>
              </li>
              <li>
                <Link className={estilos.opcionDeSesion} to={RUTA_SEGURIDAD}>
                  Seguridad de la cuenta
                </Link>
              </li>
              {esAdministrador ? (
                <li>
                  <Link className={estilos.opcionDeSesion} to={RUTA_ADMIN}>
                    Área administrativa
                  </Link>
                </li>
              ) : null}
            </>
          )}
          <li>
            <button
              type="button"
              className={estilos.opcionDeSesion}
              onClick={alCerrar}
              disabled={cerrando}
            >
              {cerrando ? 'Cerrando sesión…' : 'Cerrar sesión'}
            </button>
          </li>
        </ul>
      ) : null}
    </div>
  );
}

function primerNombreDe(nombreCompleto: string): string {
  return nombreCompleto.trim().split(/\s+/)[0] || nombreCompleto;
}

function mensajeDeCierreFallido(error: unknown): string | null {
  if (error instanceof ErrorDeApi && error.estado === 401) {
    return null;
  }
  return error instanceof Error ? error.message : null;
}
