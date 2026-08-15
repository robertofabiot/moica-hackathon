import { Route, Routes } from 'react-router';

import Inicio from './paginas/Inicio';
import RutaNoEncontrada from './paginas/RutaNoEncontrada';

/**
 * Mapa de rutas de Moica.
 *
 * P1 solo define la ruta inicial y el manejo de rutas inexistentes. Cada
 * incremento agregara las rutas de su propia capacidad.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path="*" element={<RutaNoEncontrada />} />
    </Routes>
  );
}
