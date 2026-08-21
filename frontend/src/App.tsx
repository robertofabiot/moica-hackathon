import { Route, Routes } from 'react-router';

import { InicioSesion, Registro, RUTA_INICIO_SESION, RUTA_REGISTRO } from './capacidades/auth';
import Inicio from './paginas/Inicio';
import RutaNoEncontrada from './paginas/RutaNoEncontrada';

/**
 * Mapa de rutas de Moica.
 *
 * Cada incremento agrega las rutas de su propia capacidad. P2 añade las de acceso.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path={RUTA_REGISTRO} element={<Registro />} />
      <Route path={RUTA_INICIO_SESION} element={<InicioSesion />} />
      <Route path="*" element={<RutaNoEncontrada />} />
    </Routes>
  );
}
