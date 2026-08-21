import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';

import { iniciarSesion, registrarUsuario } from '../api';
import { MOTIVO_CUENTA_CREADA, rutaDeInicioSesion } from '../rutas';
import { CLAVE_DE_SESION } from './useSesionActual';

/**
 * Las operaciones que cambian el estado de acceso.
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
