import { nombreDelEstado } from '../presentacion';
import type { EstadoSolicitud } from '../tipos';
import propios from '../paginas/solicitudes.module.css';

/**
 * Píldora semántica del estado de una solicitud. El color no es la única
 * señal: el texto del estado queda siempre visible.
 */
export default function PildoraDeEstado({
  estado,
  grande = false,
}: {
  estado: EstadoSolicitud;
  grande?: boolean;
}) {
  return (
    <span
      className={unirClases(
        propios.pildoraDeEstado,
        tono(estado),
        grande ? propios.pildoraGrande : undefined
      )}
    >
      {nombreDelEstado(estado)}
    </span>
  );
}

function tono(estado: EstadoSolicitud): string | undefined {
  if (estado === 'PENDIENTE') {
    return propios.pildoraPendiente;
  }
  if (estado === 'ACEPTADA') {
    return propios.pildoraAceptada;
  }
  if (estado === 'COMPLETADA') {
    return propios.pildoraCompletada;
  }
  return propios.pildoraCerrada;
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
