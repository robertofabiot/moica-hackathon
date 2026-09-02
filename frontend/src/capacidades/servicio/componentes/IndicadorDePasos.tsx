import estilos from '../paginas/servicios.module.css';

const PASOS_DE_PUBLICACION = [
  { numero: 1, etiqueta: 'Información' },
  { numero: 2, etiqueta: 'Detalles' },
  { numero: 3, etiqueta: 'Precio' },
  { numero: 4, etiqueta: 'Publicar' },
] as const;

export type PasoDePublicacion = (typeof PASOS_DE_PUBLICACION)[number]['numero'];

type EstadoDePaso = 'activo' | 'completado' | 'futuro';

/**
 * Indicador horizontal de los cuatro pasos del asistente de publicación.
 *
 * El paso activo lleva el acento naranja; los ya recorridos, el verde secundario
 * y una marca de listo. No navega por sí mismo: el formulario es quien avanza.
 */
export default function IndicadorDePasos({ pasoActual }: { pasoActual: PasoDePublicacion }) {
  return (
    <ol className={estilos.pasos} aria-label="Pasos de publicación">
      {PASOS_DE_PUBLICACION.map((paso) => {
        const estado = estadoDelPaso(paso.numero, pasoActual);
        return (
          <li
            key={paso.numero}
            className={unirClases(estilos.paso, claseDePaso(estado))}
            aria-current={estado === 'activo' ? 'step' : undefined}
          >
            <span className={unirClases(estilos.circulo, claseDeCirculo(estado))}>
              {estado === 'completado' ? <MarcaDeListo /> : paso.numero}
            </span>
            <span className={unirClases(estilos.etiquetaDePaso, claseDeEtiqueta(estado))}>
              {paso.etiqueta}
            </span>
          </li>
        );
      })}
    </ol>
  );
}

function estadoDelPaso(numero: PasoDePublicacion, actual: PasoDePublicacion): EstadoDePaso {
  if (numero === actual) {
    return 'activo';
  }
  if (numero < actual) {
    return 'completado';
  }
  return 'futuro';
}

function claseDePaso(estado: EstadoDePaso): string | undefined {
  if (estado === 'activo') {
    return estilos.pasoActivo;
  }
  if (estado === 'completado') {
    return estilos.pasoCompletado;
  }
  return estilos.pasoFuturo;
}

function claseDeCirculo(estado: EstadoDePaso): string | undefined {
  if (estado === 'activo') {
    return estilos.circuloActivo;
  }
  if (estado === 'completado') {
    return estilos.circuloCompletado;
  }
  return estilos.circuloFuturo;
}

function claseDeEtiqueta(estado: EstadoDePaso): string | undefined {
  if (estado === 'activo') {
    return estilos.etiquetaDePasoActiva;
  }
  if (estado === 'completado') {
    return estilos.etiquetaDePasoCompletada;
  }
  return undefined;
}

function MarcaDeListo() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={3}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M20 6 9 17l-5-5" />
    </svg>
  );
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
