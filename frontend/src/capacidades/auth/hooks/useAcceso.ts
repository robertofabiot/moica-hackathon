import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback, useRef, useState } from 'react';
import { useNavigate } from 'react-router';

import {
  cerrarSesion,
  ErrorDeApi,
  iniciarSesion,
  MENSAJE_SIN_RESPUESTA,
  registrarUsuario,
} from '../api';
import { olvidarDatosPrivados, olvidarSesion } from '../cacheDeSesion';
import {
  MOTIVO_CUENTA_CREADA,
  MOTIVO_SESION_VENCIDA,
  RUTA_VERIFICACION_SEGUNDO_FACTOR,
  rutaDeInicioSesion,
} from '../rutas';
import { CLAVE_DE_SESION } from './useSesionActual';

/**
 * Las tres operaciones que cambian el estado de acceso: registrarse, entrar y salir.
 *
 * Cada una deja la caché de la sesión coherente con lo que acaba de pasar y lleva a la pantalla que
 * corresponde, para que las páginas solo tengan que pintar.
 */

const CODIGO_OPERACION_OBSOLETA = 'OPERACION_OBSOLETA';

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

/**
 * Inicia sesión y deja la sesión recién abierta en la caché, sin volver a pedirla.
 *
 * Cuando la cuenta usa segundo factor, la sesión nace provisional y lo dice en la respuesta: en ese
 * caso el siguiente paso no es el inicio, sino la pantalla de verificación.
 */
export function useInicioSesion() {
  const cliente = useQueryClient();
  const navegar = useNavigate();

  return useMutation({
    mutationFn: iniciarSesion,
    onSuccess: (sesion) => {
      // Quien entra no hereda nada: la salida anterior ya debió limpiarlo, pero
      // entre dos cuentas no se recarga la aplicación y esta es la última
      // oportunidad de comprobarlo.
      olvidarDatosPrivados(cliente);
      cliente.setQueryData(CLAVE_DE_SESION, sesion);
      navegar(sesion.sesion.pendienteDeSegundoFactor ? RUTA_VERIFICACION_SEGUNDO_FACTOR : '/');
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
  const [errorSinConexion, setErrorSinConexion] = useState<ErrorDeApi | null>(null);
  const generacionRef = useRef(0);

  const mutacion = useMutation({
    mutationFn: async () => {
      const generacion = generacionRef.current;
      await cerrarSesion();
      if (generacion !== generacionRef.current) {
        throw new ErrorDeApi(0, CODIGO_OPERACION_OBSOLETA, MENSAJE_SIN_RESPUESTA);
      }
    },
    onSuccess: () => {
      setErrorSinConexion(null);
      olvidarSesion(cliente);
      navegar(rutaDeInicioSesion());
    },
    onError: (error) => {
      if (error instanceof ErrorDeApi && error.codigo === CODIGO_OPERACION_OBSOLETA) {
        return;
      }
      if (error instanceof ErrorDeApi && error.estado === 401) {
        setErrorSinConexion(null);
        olvidarSesion(cliente);
        navegar(rutaDeInicioSesion(MOTIVO_SESION_VENCIDA));
      }
    },
  });

  const solicitarCierre = useCallback(() => {
    if (typeof navigator !== 'undefined' && navigator.onLine === false) {
      setErrorSinConexion(new ErrorDeApi(0, 'SIN_RESPUESTA', MENSAJE_SIN_RESPUESTA));
      return;
    }
    setErrorSinConexion(null);
    generacionRef.current += 1;
    mutacion.mutate();
  }, [mutacion]);

  return {
    solicitarCierre,
    isPending: mutacion.isPending,
    error: errorSinConexion ?? mutacion.error,
  };
}
