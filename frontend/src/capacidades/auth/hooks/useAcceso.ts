import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';

import { cerrarSesion, ErrorDeApi, iniciarSesion, registrarUsuario } from '../api';
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
 * Se olvida la sesión pase lo que pase: si el backend responde 401 es porque ya no valía, y en ese
 * caso se avisa de que venció en lugar de dejar a la persona sin explicación.
 */
export function useCierreSesion() {
  const cliente = useQueryClient();
  const navegar = useNavigate();

  return useMutation({
    mutationFn: cerrarSesion,
    onSettled: (_resultado, error) => {
      cliente.setQueryData(CLAVE_DE_SESION, null);

      const habiaVencido = error instanceof ErrorDeApi && error.estado === 401;
      navegar(rutaDeInicioSesion(habiaVencido ? MOTIVO_SESION_VENCIDA : undefined));
    },
  });
}
