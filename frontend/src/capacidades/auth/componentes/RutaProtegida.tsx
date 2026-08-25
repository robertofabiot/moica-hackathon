import { useState, type ReactNode } from 'react';
import { Navigate } from 'react-router';

import { useSesionActual } from '../hooks/useSesionActual';
import { RUTA_VERIFICACION_SEGUNDO_FACTOR, rutaDeInicioSesion } from '../rutas';
import estilos from '../paginas/formulario.module.css';

/**
 * Deja pasar solo a quien tiene una sesión completa.
 *
 * Esto **no** es un control de seguridad: es navegación. Quien pida una ruta protegida sin sesión
 * recibe 401 del backend, y quien la pida con una sesión provisional recibe 403. Lo que hace este
 * componente es evitar que la persona vea una pantalla vacía llena de errores y llevarla al paso
 * que le corresponde.
 */
export default function RutaProtegida({ children }: { children: ReactNode }) {
  const sesion = useSesionActual();
  const desaparecio = useSesionDesaparecida(sesion.data);

  if (sesion.isPending) {
    return <Aviso texto="Comprobando tu sesión…" />;
  }

  if (!sesion.data) {
    return desaparecio ? <Aviso texto="Cerrando tu sesión…" /> : <IrAIniciarSesion />;
  }

  if (sesion.data.sesion.pendienteDeSegundoFactor) {
    return <Navigate to={RUTA_VERIFICACION_SEGUNDO_FACTOR} replace />;
  }

  return <>{children}</>;
}

/**
 * Deja pasar solo a una sesión provisional.
 *
 * La pantalla de verificación no tiene sentido sin ella: sin sesión hay que iniciarla, y con la
 * sesión ya completa no hay nada que verificar.
 */
export function RutaDeVerificacion({ children }: { children: ReactNode }) {
  const sesion = useSesionActual();
  const desaparecio = useSesionDesaparecida(sesion.data);

  if (sesion.isPending) {
    return <Aviso texto="Comprobando tu sesión…" />;
  }

  if (!sesion.data) {
    return desaparecio ? <Aviso texto="Cerrando tu sesión…" /> : <IrAIniciarSesion />;
  }

  if (!sesion.data.sesion.pendienteDeSegundoFactor) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

/**
 * Distingue «llegaste aquí sin sesión» de «tu sesión terminó mientras estabas aquí».
 *
 * La diferencia importa porque los dos casos los resuelve alguien distinto. El primero es de este
 * componente: nadie más sabe que la persona pidió una ruta que no le corresponde. El segundo lo
 * resuelve quien acabó con la sesión —cerrar sesión, cambiar la contraseña o desactivar el segundo
 * factor—, que además sabe **por qué** terminó y navega al inicio de sesión con esa explicación. Si
 * este componente redirigiera también en ese caso, su navegación llegaría después y la explicación
 * se perdería por el camino.
 *
 * Que ese segundo caso termine siempre no depende de que alguna pantalla se acuerde de resolverlo:
 * `useVigilanciaDeSesion`, montada en `App` durante toda la navegación, lleva a iniciar sesión ante
 * cualquier sesión que desaparezca sin que nadie más lo haya hecho. El aviso de aquí es una
 * transición, no un estado en el que se pueda quedar.
 */
function useSesionDesaparecida(sesion: unknown): boolean {
  const [huboSesion, setHuboSesion] = useState(false);

  // Ajuste de estado durante el render, el patrón que React documenta para
  // «recordar algo que ya se vio»: se ejecuta una sola vez, en cuanto aparece la
  // primera sesión, y no provoca un ciclo porque la condición deja de cumplirse.
  if (sesion && !huboSesion) {
    setHuboSesion(true);
  }

  return huboSesion && !sesion;
}

function IrAIniciarSesion() {
  return <Navigate to={rutaDeInicioSesion()} replace />;
}

function Aviso({ texto }: { texto: string }) {
  return (
    <main className={estilos.pantalla}>
      <p className={estilos.explicacion} role="status">
        {texto}
      </p>
    </main>
  );
}
