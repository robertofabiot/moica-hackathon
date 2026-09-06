import { nombreDeLaInsignia } from '../etiquetas';
import type { NivelVerificacion } from '../tipos';
import estilos from './verificacion.module.css';

/**
 * La insignia del nivel de verificación de un perfil.
 *
 * Es lo único público del flujo: documentos, observaciones y claves de almacenamiento no salen
 * nunca. Aquí solo se usa en el perfil propio, porque las superficies públicas llegan con P5; se
 * escribe como componente reutilizable para que entonces diga exactamente lo mismo.
 *
 * No se distingue por color solamente: cada nivel lleva su texto y su símbolo, de modo que quien no
 * perciba la diferencia de tono siga leyendo cuál es.
 */
export default function InsigniaDeVerificacion({ nivel }: { nivel: NivelVerificacion }) {
  return (
    <span className={`${estilos.insignia} ${claseDelNivel(nivel)}`}>
      <span aria-hidden="true">{simboloDelNivel(nivel)}</span> {nombreDeLaInsignia(nivel)}
    </span>
  );
}

// Las clases de un CSS Module se leen por índice, así que el tipo admite
// `undefined`; el literal de plantilla del componente lo tolera sin ruido.
function claseDelNivel(nivel: NivelVerificacion): string | undefined {
  switch (nivel) {
    case 'PROFESIONAL_VERIFICADO':
      return estilos.insigniaProfesional;
    case 'VERIFICADO_BASICO':
      return estilos.insigniaBasica;
    default:
      return estilos.insigniaSinVerificar;
  }
}

function simboloDelNivel(nivel: NivelVerificacion): string {
  switch (nivel) {
    case 'PROFESIONAL_VERIFICADO':
      return '★';
    case 'VERIFICADO_BASICO':
      return '✓';
    default:
      return '•';
  }
}
