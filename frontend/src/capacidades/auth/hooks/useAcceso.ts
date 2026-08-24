import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';

import {
  cerrarSesion,
  ErrorDeApi,
  iniciarSesion,
  MENSAJE_SIN_RESPUESTA,
  registrarUsuario,
} from '../api';
import { MOTIVO_CUENTA_CREADA, MOTIVO_SESION_VENCIDA, rutaDeInicioSesion } from '../rutas';
import { CLAVE_DE_SESION } from './useSesionActual';

/**
 * Las tres operaciones que cambian el estado de acceso: registrarse, entrar y salir.
 *
 * Cada una deja la caché de la sesión coherente con lo que acaba de pasar y lleva a la pantalla que
 * corresponde, para que las páginas solo tengan que pintar.
 */

/**
 * Crea la cuenta y lleva a iniciar sesión.
 *
 * El registro no autentica: crear la cuenta y entrar son dos pasos distintos.
 */
export function useRegistro() {
  const navegar = useNavigate();

  return useMutation({
    mutationFn: registrarUsuario,
    onSuccess: () => navegar(rutaDeInicioSesion(MOTIVO_CUENTA_CREADA)),
  });
}

/** Inicia sesión y deja la sesión recién abierta en la caché, sin volver a pedirla. */
export function useInicioSesion() {
  const cliente = useQueryClient();
  const navegar = useNavigate();

  return useMutation({
    mutationFn: iniciarSesion,
    onSuccess: (sesion) => {
      cliente.setQueryData(CLAVE_DE_SESION, sesion);
      navegar('/');
    },
  });
}

/**
 * Cierra la sesión.
 *
 * Solo se olvida el estado local cuando el servidor confirmó el cierre (204) o
 * cuando ya no hay sesión que revocar (401). Un fallo de red, un 403 o un 500
 * no revocan la fila: si se limpiara aquí, la persona creería que salió y el
 * servidor seguiría con la sesión vigente.
 */
export function useCierreSesion() {
  const cliente = useQueryClient();
  const navegar = useNavigate();

  return useMutation({
    mutationFn: async () => {
      // Offline de DevTools deja `fetch` colgado; no hay que llegar a disparar la petición.
      if (typeof navigator !== 'undefined' && navigator.onLine === false) {
        throw new ErrorDeApi(0, 'SIN_RESPUESTA', MENSAJE_SIN_RESPUESTA);
      }
      return cerrarSesion();
    },
    onSuccess: () => {
      cliente.setQueryData(CLAVE_DE_SESION, null);
      navegar(rutaDeInicioSesion());
    },
    onError: (error) => {
      if (error instanceof ErrorDeApi && error.estado === 401) {
        cliente.setQueryData(CLAVE_DE_SESION, null);
        navegar(rutaDeInicioSesion(MOTIVO_SESION_VENCIDA));
      }
    },
  });
}
