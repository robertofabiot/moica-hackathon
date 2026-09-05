import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';

/**
 * El error que devolvió la API, tal cual: es quien conoce el estado real del caso.
 *
 * Se coloca **fuera** de los bloques de acción de quien lo usa. Un conflicto cambia justo lo que
 * decide qué acciones caben: el refresco que sigue al fallo puede traer el caso cerrado, la medida
 * ya sustituida o el permiso retirado, y con ellos desaparecería el formulario y —si el aviso
 * viviera dentro— la única explicación de por qué la acción no salió.
 */
export default function AvisoDeAccion({ error }: { error: unknown }) {
  if (error === null || error === undefined) {
    return null;
  }

  return (
    <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
      {error instanceof ErrorDeApi ? error.message : 'No pudimos completar la acción.'}
    </p>
  );
}
