import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';

import {
  cambiarClave,
  confirmarActivacionDeSegundoFactor,
  desactivarSegundoFactor,
  iniciarActivacionDeSegundoFactor,
  obtenerSegundoFactor,
  verificarSegundoFactorDeLaSesion,
} from '../api';
import { MOTIVO_CREDENCIALES_CAMBIADAS, MOTIVO_SESION_VENCIDA, rutaDeInicioSesion } from '../rutas';
import { CLAVE_DE_SESION } from './useSesionActual';

/**
 * Las operaciones de la sección de seguridad de la cuenta.
 *
 * Dos de ellas —cambiar la contraseña y desactivar el segundo factor— revocan todas las sesiones
 * en el servidor. Aquí eso se traduce en lo mismo que ya hace el cierre de sesión: olvidar el
 * estado de acceso y volver al inicio de sesión explicando por qué.
 */

/** Clave con la que React Query guarda el estado del segundo factor. */
export const CLAVE_DE_SEGUNDO_FACTOR = ['auth', 'segundo-factor'] as const;

/** Estado del segundo factor de la cuenta. Nunca contiene el secreto. */
export function useSegundoFactor() {
  return useQuery({
    queryKey: CLAVE_DE_SEGUNDO_FACTOR,
    queryFn: obtenerSegundoFactor,
    retry: false,
  });
}

/** Cambia la contraseña y devuelve al inicio de sesión, porque ya no queda ninguna vigente. */
export function useCambioDeClave() {
  const olvidarAccesoYVolverAEntrar = useSalidaTrasCambioDeCredenciales();

  return useMutation({
    mutationFn: cambiarClave,
    onSuccess: olvidarAccesoYVolverAEntrar,
  });
}

/**
 * Empieza la activación del segundo factor.
 *
 * El resultado —con el secreto dentro— se queda en el estado de la mutación, no en la caché de
 * React Query: así desaparece al salir de la pantalla en lugar de sobrevivir en memoria.
 */
export function useActivacionDeSegundoFactor() {
  return useMutation({ mutationFn: iniciarActivacionDeSegundoFactor });
}

/** Confirma la activación con el primer código válido. */
export function useConfirmacionDeSegundoFactor() {
  const cliente = useQueryClient();

  return useMutation({
    mutationFn: confirmarActivacionDeSegundoFactor,
    onSuccess: (segundoFactor) => {
      cliente.setQueryData(CLAVE_DE_SEGUNDO_FACTOR, segundoFactor);
      // Activarlo verifica también esta sesión: hay que releerla para que la
      // interfaz deje de creer que está pendiente.
      void cliente.invalidateQueries({ queryKey: CLAVE_DE_SESION });
    },
  });
}

/** Desactiva el segundo factor; el backend lo trata como un cambio de credenciales. */
export function useDesactivacionDeSegundoFactor() {
  const olvidarAccesoYVolverAEntrar = useSalidaTrasCambioDeCredenciales();

  return useMutation({
    mutationFn: desactivarSegundoFactor,
    onSuccess: olvidarAccesoYVolverAEntrar,
  });
}

/** Completa la sesión provisional presentando el código. */
export function useVerificacionDeSesion() {
  const cliente = useQueryClient();
  const navegar = useNavigate();

  return useMutation({
    mutationFn: verificarSegundoFactorDeLaSesion,
    onSuccess: (sesion) => {
      cliente.setQueryData(CLAVE_DE_SESION, sesion);
      navegar('/');
    },
    onError: (error) => {
      // Si la sesión provisional muere mientras se verifica, no tiene sentido
      // insistir con el código: hay que volver a entrar.
      if (esSesionPerdida(error)) {
        // Primero se navega y después se olvida la sesión: al revés, el
        // envoltorio de ruta vería «sin sesión» todavía montado y redirigiría
        // por su cuenta al inicio de sesión, sin el motivo.
        navegar(rutaDeInicioSesion(MOTIVO_SESION_VENCIDA));
        cliente.setQueryData(CLAVE_DE_SESION, null);
      }
    },
  });
}

/**
 * Lo que ocurre después de cambiar unas credenciales: no queda ninguna sesión vigente, ni siquiera
 * la actual, así que se olvida todo el estado de acceso y se vuelve a la pantalla de entrada.
 */
function useSalidaTrasCambioDeCredenciales() {
  const cliente = useQueryClient();
  const navegar = useNavigate();

  return () => {
    // El orden importa: si primero se olvidara la sesión, el envoltorio de la
    // ruta protegida —todavía montado— redirigiría al inicio de sesión sin el
    // motivo y la explicación se perdería.
    navegar(rutaDeInicioSesion(MOTIVO_CREDENCIALES_CAMBIADAS));
    cliente.setQueryData(CLAVE_DE_SESION, null);
    cliente.removeQueries({ queryKey: CLAVE_DE_SEGUNDO_FACTOR });
  };
}

function esSesionPerdida(error: unknown): boolean {
  return error instanceof Error && 'estado' in error && error.estado === 401;
}
