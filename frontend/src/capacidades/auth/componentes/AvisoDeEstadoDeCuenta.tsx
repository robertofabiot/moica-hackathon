import { useSesionActual } from '../hooks/useSesionActual';
import type { EstadoCuenta } from '../tipos';
import estilos from './avisoDeCuenta.module.css';

/** Qué significa cada estado para quien lo tiene, en su propia voz. */
const EXPLICACION: Record<Exclude<EstadoCuenta, 'ACTIVA'>, string> = {
  RESTRINGIDA_TEMPORAL:
    'Tu cuenta está restringida temporalmente. Puedes navegar, consultar tu historial, cancelar' +
    ' compromisos que ya tenías y escribir a soporte, pero por ahora no puedes publicar servicios,' +
    ' contratar, aceptar solicitudes, enviar mensajes ni calificar.',
  SUSPENDIDA_TEMPORAL:
    'Tu cuenta está suspendida temporalmente. Mientras dure, no puedes usar las funciones de Moica.',
  SUSPENDIDA_PERMANENTE: 'Tu cuenta está suspendida de forma permanente y no se reactiva sola.',
};

/**
 * El aviso que ve quien arrastra una medida administrativa.
 *
 * Vive junto a las rutas y no dentro de una pantalla porque acompaña a la persona por toda la
 * aplicación: enterarse de por qué algo no funciona no debería depender de en qué página esté.
 *
 * Dice tres cosas y ninguna más: qué le pasa a la cuenta, hasta cuándo si es temporal, y a dónde
 * escribir. **No dice qué medida se aplicó, ni desde qué caso, ni quién la decidió**: eso es
 * información del expediente administrativo y esta persona no tiene por qué verla.
 *
 * En la práctica lo lee una cuenta restringida, que conserva su sesión. A una suspendida se le
 * explica lo mismo al intentar entrar: aplicar una suspensión revoca sus sesiones, así que ya no
 * llega hasta aquí.
 *
 * **La apelación no se presenta en Moica.** Por eso el aviso ofrece un canal externo y no un
 * formulario: quien lo atiende registra después lo recibido desde el área administrativa.
 */
export default function AvisoDeEstadoDeCuenta() {
  const sesion = useSesionActual();

  const aviso = sesion.data?.avisoDeCuenta;
  const estado = sesion.data?.usuario.estadoCuenta;

  if (aviso === null || aviso === undefined || estado === undefined || estado === 'ACTIVA') {
    return null;
  }

  return (
    <aside className={estilos.aviso} role="status" aria-labelledby="aviso-de-cuenta">
      <p className={estilos.titulo} id="aviso-de-cuenta">
        {EXPLICACION[estado]}
      </p>
      {aviso.fechaFin !== null && (
        <p className={estilos.detalle}>Termina el {fechaLegible(aviso.fechaFin)}.</p>
      )}
      <p className={estilos.detalle}>
        Si crees que es un error, escribe a{' '}
        <a className={estilos.canal} href={`mailto:${aviso.canalDeSoporte}`}>
          {aviso.canalDeSoporte}
        </a>
        . Una persona revisará lo que cuentes.
      </p>
    </aside>
  );
}

/** La fecha en la forma en que se lee, no en la que viaja. */
function fechaLegible(fecha: string): string {
  return new Date(fecha).toLocaleString('es-NI', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
