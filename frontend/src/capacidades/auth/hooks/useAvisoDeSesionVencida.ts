import { useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useNavigate } from 'react-router';

import type { SesionActual } from '../tipos';
import { MOTIVO_SESION_VENCIDA, rutaDeInicioSesion } from '../rutas';
import { CLAVE_DE_SESION } from './useSesionActual';

/**
 * Avisa cuando la sesión vence sin que la persona haya hecho nada.
 *
 * La API dice hasta cuándo vale la sesión, así que la interfaz programa el aviso para ese momento
 * en lugar de esperar a que la siguiente acción falle con un 401 inexplicable. Al vencer se olvida
 * la sesión y se lleva a iniciar sesión con el motivo a la vista.
 *
 * Moica no renueva sesiones automáticamente (decisión D-SEC-01): al vencer, se vuelve a entrar.
 */
export function useAvisoDeSesionVencida(sesion: SesionActual | null | undefined): void {
  const cliente = useQueryClient();
  const navegar = useNavigate();

  useEffect(() => {
    if (!sesion) {
      return;
    }

    const restante = new Date(sesion.sesion.fechaExpiracion).getTime() - Date.now();

    const temporizador = setTimeout(
      () => {
        cliente.setQueryData(CLAVE_DE_SESION, null);
        navegar(rutaDeInicioSesion(MOTIVO_SESION_VENCIDA));
      },
      Math.max(restante, 0)
    );

    return () => clearTimeout(temporizador);
  }, [sesion, cliente, navegar]);
}
