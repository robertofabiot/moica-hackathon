import { useState } from 'react';

import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import {
  useAdministradores,
  useAsignacionDeCaso,
  useCierreDeCaso,
  useInicioDeRevision,
} from '../hooks/useRevisionDeCasos';
import type { ExpedienteDeCaso, ResultadoDeCaso } from '../tipos';
import propios from './acciones.module.css';
import AvisoDeAccion from './AvisoDeAccion';
import { errorMasReciente } from './errorMasReciente';

/**
 * Las decisiones que caben sobre un caso, según su estado y quién lo lleva.
 *
 * Ocultar un botón mejora la experiencia pero no es un control de seguridad: la autorización la
 * aplica el backend en cada petición, y quien fuerce una acción que no le toca recibe 403 o 409.
 * Por eso el mensaje de error se muestra tal cual lo envía la API, que es quien sabe el estado real.
 *
 * El aviso vive **fuera** de los bloques de acción, y por eso la mutación de cierre se declara aquí
 * y no dentro de su formulario. Un conflicto cambia justamente lo que decide qué acciones caben: el
 * refresco que sigue al fallo puede traer el caso cerrado o sin permiso para resolver, y con él
 * desaparecerían el botón, el formulario y —si el aviso viviera dentro— la única explicación de por
 * qué la acción no salió. Quien recibe un 409 se quedaría mirando una pantalla que cambió sola.
 */
export default function AccionesDelCaso({ expediente }: { expediente: ExpedienteDeCaso }) {
  const { caso, puedeResolver } = expediente;
  const idCaso = caso.idCasoModeracion;

  const administradores = useAdministradores();
  const asignacion = useAsignacionDeCaso(idCaso);
  const revision = useInicioDeRevision(idCaso);
  const cierre = useCierreDeCaso(idCaso);

  const [responsableElegido, setResponsableElegido] = useState('');

  const cerrado = caso.estadoActual === 'CERRADO';
  const puedeIniciarRevision =
    puedeResolver && (caso.estadoActual === 'ABIERTO' || caso.estadoActual === 'REABIERTO');
  const puedeCerrar = puedeResolver && caso.estadoActual === 'EN_REVISION';

  return (
    <section className={secciones.seccion} aria-labelledby="acciones">
      <h2 className={secciones.tituloDeSeccion} id="acciones">
        Revisión
      </h2>

      <p className={secciones.explicacion}>
        {caso.nombreAdministradorResponsable === null
          ? 'Todavía nadie responde por este caso.'
          : `Responsable: ${caso.nombreAdministradorResponsable}.`}{' '}
        {!puedeResolver &&
          !cerrado &&
          'Solo quien lo tiene asignado puede iniciar la revisión y cerrarlo.'}
      </p>

      {!cerrado && (
        <form
          className={propios.formularioDeAsignacion}
          onSubmit={(evento) => {
            evento.preventDefault();
            if (responsableElegido !== '') {
              asignacion.mutate(Number(responsableElegido));
            }
          }}
        >
          <label className={estilos.etiqueta} htmlFor="responsable">
            {caso.idAdministradorResponsable === null
              ? 'Asignar responsable'
              : 'Reasignar a otra persona'}
          </label>
          <div className={propios.filaDeAsignacion}>
            <select
              className={estilos.campo}
              id="responsable"
              value={responsableElegido}
              onChange={(evento) => setResponsableElegido(evento.target.value)}
              disabled={administradores.isPending || asignacion.isPending}
            >
              <option value="">Elige una persona administradora</option>
              {administradores.data?.map((persona) => (
                <option key={persona.idAdministrador} value={persona.idAdministrador}>
                  {persona.nombreCompleto}
                </option>
              ))}
            </select>
            <button
              className={secciones.botonSecundario}
              type="submit"
              disabled={responsableElegido === '' || asignacion.isPending}
            >
              {asignacion.isPending ? 'Asignando…' : 'Asignar'}
            </button>
          </div>
        </form>
      )}

      {puedeIniciarRevision && (
        <div className={propios.accion}>
          <button
            className={secciones.botonSecundario}
            type="button"
            onClick={() => revision.mutate()}
            disabled={revision.isPending}
          >
            {revision.isPending ? 'Iniciando…' : 'Iniciar la revisión'}
          </button>
        </div>
      )}

      {puedeCerrar && <FormularioDeCierre cierre={cierre} />}

      <AvisoDeAccion error={errorMasReciente(asignacion, revision, cierre)} />
    </section>
  );
}

/**
 * El cierre: resultado y resolución, juntos.
 *
 * Van en el mismo formulario porque el backend los exige a la vez —un caso cerrado sin decisión no
 * diría nada— y porque separarlos invitaría a registrar un resultado y dejar la explicación para
 * después, que es justo lo que hace inauditable un expediente meses más tarde.
 *
 * La mutación llega por props: si se creara aquí, un 403 que retire el permiso de resolver
 * desmontaría este formulario y se llevaría por delante el error que lo explica.
 */
function FormularioDeCierre({ cierre }: { cierre: MutacionDeCierre }) {
  const [resultado, setResultado] = useState<ResultadoDeCaso>('PROCEDENTE');
  const [resolucion, setResolucion] = useState('');

  const vacia = resolucion.trim() === '';

  return (
    <form
      className={propios.formularioDeCierre}
      onSubmit={(evento) => {
        evento.preventDefault();
        if (!vacia) {
          cierre.mutate({ resultado, resolucion: resolucion.trim() });
        }
      }}
    >
      <fieldset className={propios.grupoDeResultado}>
        <legend className={estilos.etiqueta}>Resultado de la investigación</legend>
        <label className={propios.opcion}>
          <input
            type="radio"
            name="resultado"
            value="PROCEDENTE"
            checked={resultado === 'PROCEDENTE'}
            onChange={() => setResultado('PROCEDENTE')}
          />
          Procedente — amerita una decisión administrativa
        </label>
        <label className={propios.opcion}>
          <input
            type="radio"
            name="resultado"
            value="DESESTIMADO"
            checked={resultado === 'DESESTIMADO'}
            onChange={() => setResultado('DESESTIMADO')}
          />
          Desestimado — no amerita una medida
        </label>
      </fieldset>

      <label className={estilos.etiqueta} htmlFor="resolucion">
        Resolución
      </label>
      <textarea
        className={estilos.campo}
        id="resolucion"
        name="resolucion"
        rows={4}
        maxLength={3000}
        value={resolucion}
        onChange={(evento) => setResolucion(evento.target.value)}
        aria-describedby="ayuda-resolucion"
      />
      <p className={secciones.explicacion} id="ayuda-resolucion">
        Queda en el historial del caso. Explica qué se revisó y por qué se decidió así.{' '}
        {resolucion.length} de 3000 caracteres.
      </p>

      <p className={secciones.explicacion}>
        Cerrar registra la decisión. No aplica ninguna medida ni cambia el estado de la cuenta
        reportada.
      </p>

      <button
        className={secciones.botonSecundario}
        type="submit"
        disabled={vacia || cierre.isPending}
      >
        {cierre.isPending ? 'Cerrando…' : 'Cerrar el caso'}
      </button>
    </form>
  );
}

type MutacionDeCierre = ReturnType<typeof useCierreDeCaso>;
