import { Link } from 'react-router';

import CambioDeClave from '../componentes/CambioDeClave';
import SegundoFactorDeLaCuenta from '../componentes/SegundoFactorDeLaCuenta';
import estilos from './seguridad.module.css';

/**
 * Seguridad de la cuenta: contraseña y segundo factor.
 *
 * Solo pinta y ordena. Cada sección se encarga de sus datos y de sus estados de carga y error, que
 * es lo que permite que un fallo al consultar el segundo factor no impida cambiar la contraseña.
 */
export default function SeguridadCuenta() {
  return (
    <main className={estilos.pantalla}>
      <div className={estilos.contenido}>
        <header className={estilos.encabezado}>
          <h1 className={estilos.titulo}>Seguridad de tu cuenta</h1>
          <p className={estilos.explicacion}>
            Desde aquí cambias tu contraseña y decides si Moica te pide un segundo factor al entrar.
          </p>
        </header>

        <CambioDeClave />
        <SegundoFactorDeLaCuenta />

        <p className={estilos.pie}>
          <Link to="/">Volver al inicio</Link>
        </p>
      </div>
    </main>
  );
}
