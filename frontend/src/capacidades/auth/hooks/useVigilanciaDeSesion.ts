import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router';

import { motivoDeLaSalida, olvidarDatosPrivados } from '../cacheDeSesion';
import { rutaDeInicioSesion } from '../rutas';
import { CLAVE_DE_SESION, useSesionActual } from './useSesionActual';

/**
 * Vigila la sesión mientras Moica esté abierta, en la pantalla que sea.
 *
 * Hace dos cosas. La primera es programar el fin de la sesión: la API dice hasta cuándo vale, así
 * que la interfaz lo espera en lugar de dejar que la siguiente acción falle con un 401
 * inexplicable. Moica no renueva sesiones automáticamente (decisión D-SEC-01): al vencer se vuelve
 * a entrar.
 *
 * La segunda es llevar a iniciar sesión en cuanto la sesión deja de existir, sea por lo que sea:
 * venció, la cerró la persona, cambió sus credenciales, la revocaron desde otro dispositivo o una
 * consulta de fondo recibió un 401. **Es el único sitio de la aplicación que navega por ese
 * motivo.** Quien termina una sesión a propósito solo anota su explicación con `olvidarSesion`; si
 * además navegara, las dos navegaciones competirían y el motivo visible dependería de cuál llegara
 * antes.
 *
 * Se usa desde `App`, que permanece montada durante toda la navegación. Antes vivía en la pantalla
 * de inicio, y allí el temporizador se cancelaba al abrir `/seguridad` o `/admin`: la sesión vencía
 * sin que nadie se enterara. Estando aquí, ninguna ruta puede quedarse indefinidamente en «Cerrando
 * tu sesión…» esperando a que alguien resuelva la salida.
 */
export function useVigilanciaDeSesion(): void {
  const cliente = useQueryClient();
  const navegar = useNavigate();
  const sesion = useSesionActual().data;
  const huboSesion = useRef(false);

  useEffect(() => {
    if (!sesion) {
      return;
    }
    huboSesion.current = true;

    const restante = new Date(sesion.sesion.fechaExpiracion).getTime() - Date.now();
    const temporizador = setTimeout(
      () => cliente.setQueryData(CLAVE_DE_SESION, null),
      Math.max(restante, 0)
    );

    return () => clearTimeout(temporizador);
  }, [sesion, cliente]);

  useEffect(() => {
    // `undefined` es «todavía se está consultando»; solo `null` significa que ya
    // no hay sesión. Y sin una sesión anterior no hay salida que explicar: quien
    // llega sin haber entrado lo resuelve el envoltorio de la ruta.
    if (sesion !== null || !huboSesion.current) {
      return;
    }
    huboSesion.current = false;

    const motivo = motivoDeLaSalida(cliente);
    navegar(rutaDeInicioSesion(motivo ?? undefined));

    // Quien anotó el motivo ya limpió, pero una sesión que se apaga sola —el
    // temporizador o un 401 de fondo— no pasa por ahí.
    olvidarDatosPrivados(cliente);
  }, [sesion, cliente, navegar]);
}
