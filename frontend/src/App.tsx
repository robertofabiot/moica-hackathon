import { Route, Routes } from 'react-router';

import { PanelAdministrativo, RUTA_ADMIN, RutaAdministrativa } from './capacidades/admin';
import {
  InicioSesion,
  Registro,
  RUTA_INICIO_SESION,
  RUTA_REGISTRO,
  RUTA_SEGURIDAD,
  RUTA_VERIFICACION_SEGUNDO_FACTOR,
  RutaDeVerificacion,
  RutaProtegida,
  SeguridadCuenta,
  useVigilanciaDeSesion,
  VerificacionSegundoFactor,
} from './capacidades/auth';
import { PerfilPrestador, RUTA_PRESTADOR } from './capacidades/prestador';
import Inicio from './paginas/Inicio';
import RutaNoEncontrada from './paginas/RutaNoEncontrada';

/**
 * Mapa de rutas de Moica.
 *
 * Cada incremento agrega las rutas de su propia capacidad. P2 añadió las de acceso, P3 las de
 * seguridad de la cuenta y el área administrativa, y P4 la del perfil de prestador.
 *
 * Los envoltorios de ruta deciden a qué pantalla llevar a cada persona, no si puede hacer algo:
 * quien pida una ruta protegida de la API sin permiso recibe 401 o 403 del backend aunque llegue a
 * pintar la pantalla.
 *
 * Lo que si vive aqui y no en una pantalla concreta es la vigilancia de la sesion: es lo unico que
 * debe seguir funcionando al cambiar de ruta.
 */
export default function App() {
  useVigilanciaDeSesion();

  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path={RUTA_REGISTRO} element={<Registro />} />
      <Route path={RUTA_INICIO_SESION} element={<InicioSesion />} />
      <Route
        path={RUTA_VERIFICACION_SEGUNDO_FACTOR}
        element={
          <RutaDeVerificacion>
            <VerificacionSegundoFactor />
          </RutaDeVerificacion>
        }
      />
      <Route
        path={RUTA_SEGURIDAD}
        element={
          <RutaProtegida>
            <SeguridadCuenta />
          </RutaProtegida>
        }
      />
      <Route
        path={RUTA_PRESTADOR}
        element={
          <RutaProtegida>
            <PerfilPrestador />
          </RutaProtegida>
        }
      />
      <Route
        path={RUTA_ADMIN}
        element={
          <RutaAdministrativa>
            <PanelAdministrativo />
          </RutaAdministrativa>
        }
      />
      <Route path="*" element={<RutaNoEncontrada />} />
    </Routes>
  );
}
