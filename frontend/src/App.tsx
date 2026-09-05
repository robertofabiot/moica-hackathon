import { Navigate, Route, Routes } from 'react-router';

import { PanelAdministrativo, RUTA_ADMIN, RutaAdministrativa } from './capacidades/admin';
import {
  InicioSesion,
  Registro,
  RUTA_CONFIGURACION,
  RUTA_INICIO_SESION,
  RUTA_REGISTRO,
  RUTA_SEGURIDAD,
  RUTA_VERIFICACION_SEGUNDO_FACTOR,
  AvisoDeEstadoDeCuenta,
  RutaDeVerificacion,
  RutaProtegida,
  SeguridadCuenta,
  useVigilanciaDeSesion,
  VerificacionSegundoFactor,
} from './capacidades/auth';
import {
  DetalleDeServicio,
  ExplorarServicios,
  PrestadorPublico,
  RUTA_DETALLE_SERVICIO,
  RUTA_EXPLORAR,
  RUTA_PRESTADOR_PUBLICO,
} from './capacidades/busqueda';
import {
  BandejaDeCasos,
  CatalogoDeMedidas,
  ExpedienteDeCaso,
  RUTA_ADMIN_CASOS,
  RUTA_ADMIN_EXPEDIENTE,
  RUTA_ADMIN_MEDIDAS,
} from './capacidades/moderacion';
import { PanelUsuario, RUTA_PANEL } from './capacidades/panel';
import { PerfilPrestador, RUTA_PRESTADOR } from './capacidades/prestador';
import {
  EditarServicio,
  NuevoServicio,
  RUTA_EDITAR_SERVICIO,
  RUTA_NUEVO_SERVICIO,
  RUTA_SERVICIOS,
  ServiciosPropios,
} from './capacidades/servicio';
import {
  DetalleDeSolicitud,
  Mensajes,
  MisSolicitudes,
  NuevaSolicitud,
  RUTA_DETALLE_SOLICITUD,
  RUTA_MENSAJES,
  RUTA_NUEVA_SOLICITUD,
  RUTA_SOLICITUDES,
} from './capacidades/solicitud';
import { ColaDeVerificaciones, RUTA_ADMIN_VERIFICACIONES } from './capacidades/verificacion';
import Inicio from './paginas/Inicio';
import RutaNoEncontrada from './paginas/RutaNoEncontrada';

/**
 * Mapa de rutas de Moica.
 *
 * Cada incremento agrega las rutas de su propia capacidad. P2 añadió las de acceso, P3 las de
 * seguridad de la cuenta y el área administrativa, P4 la del perfil de prestador, P4V la cola
 * administrativa de verificaciones, P5 el descubrimiento público y la gestión de servicios, P6
 * el ciclo de solicitudes, el panel de actividad en `/panel` y P10A la revisión administrativa de
 * casos de moderación.
 *
 * Los envoltorios de ruta deciden a qué pantalla llevar a cada persona, no si puede hacer algo:
 * quien pida una ruta protegida de la API sin permiso recibe 401 o 403 del backend aunque llegue a
 * pintar la pantalla.
 *
 * Lo que si vive aqui y no en una pantalla concreta es lo transversal: la vigilancia de la sesion y
 * el aviso de una cuenta restringida o suspendida. Los dos deben seguir funcionando al cambiar de
 * ruta, porque enterarse de por que algo no funciona no deberia depender de en que pantalla se este.
 */
export default function App() {
  useVigilanciaDeSesion();

  return (
    <>
      <AvisoDeEstadoDeCuenta />
      <Routes>
        <Route path="/" element={<Inicio />} />
        <Route path={RUTA_EXPLORAR} element={<ExplorarServicios />} />
        <Route path={RUTA_DETALLE_SERVICIO} element={<DetalleDeServicio />} />
        <Route path={RUTA_PRESTADOR_PUBLICO} element={<PrestadorPublico />} />
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
        <Route path={RUTA_CONFIGURACION} element={<Navigate to={RUTA_SEGURIDAD} replace />} />
        <Route
          path={RUTA_PANEL}
          element={
            <RutaProtegida>
              <PanelUsuario />
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
          path={RUTA_SERVICIOS}
          element={
            <RutaProtegida>
              <ServiciosPropios />
            </RutaProtegida>
          }
        />
        <Route
          path={RUTA_NUEVO_SERVICIO}
          element={
            <RutaProtegida>
              <NuevoServicio />
            </RutaProtegida>
          }
        />
        <Route
          path={RUTA_EDITAR_SERVICIO}
          element={
            <RutaProtegida>
              <EditarServicio />
            </RutaProtegida>
          }
        />
        <Route
          path={RUTA_SOLICITUDES}
          element={
            <RutaProtegida>
              <MisSolicitudes />
            </RutaProtegida>
          }
        />
        <Route
          path={RUTA_DETALLE_SOLICITUD}
          element={
            <RutaProtegida>
              <DetalleDeSolicitud />
            </RutaProtegida>
          }
        />
        <Route
          path={RUTA_NUEVA_SOLICITUD}
          element={
            <RutaProtegida>
              <NuevaSolicitud />
            </RutaProtegida>
          }
        />
        <Route
          path={RUTA_MENSAJES}
          element={
            <RutaProtegida>
              <Mensajes />
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
        <Route
          path={RUTA_ADMIN_VERIFICACIONES}
          element={
            <RutaAdministrativa>
              <ColaDeVerificaciones />
            </RutaAdministrativa>
          }
        />
        <Route
          path={RUTA_ADMIN_CASOS}
          element={
            <RutaAdministrativa>
              <BandejaDeCasos />
            </RutaAdministrativa>
          }
        />
        <Route
          path={RUTA_ADMIN_EXPEDIENTE}
          element={
            <RutaAdministrativa>
              <ExpedienteDeCaso />
            </RutaAdministrativa>
          }
        />
        <Route
          path={RUTA_ADMIN_MEDIDAS}
          element={
            <RutaAdministrativa>
              <CatalogoDeMedidas />
            </RutaAdministrativa>
          }
        />
        <Route path="*" element={<RutaNoEncontrada />} />
      </Routes>
    </>
  );
}
