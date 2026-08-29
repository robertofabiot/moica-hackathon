import { InsigniaDeVerificacion } from '../../verificacion';
import secciones from '../../../comun/estilos/secciones.module.css';
import type { PrestadorPublico } from '../tipos';

/**
 * Insignia pública con la explicación de qué confirma y qué no garantiza.
 *
 * El texto de advertencia es el mismo en todos los niveles: la revisión documental no garantiza
 * la calidad futura del trabajo.
 */
export default function InsigniaResponsable({ prestador }: { prestador: PrestadorPublico }) {
  return (
    <div>
      <p>
        <InsigniaDeVerificacion nivel={prestador.nivelVerificacion} />
      </p>
      <p className={secciones.explicacion}>{prestador.significadoVerificacion}</p>
      <p className={secciones.explicacion}>{prestador.advertenciaDeInsignia}</p>
    </div>
  );
}
