import { useState } from 'react';

import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import {
  useReaperturaDeCaso,
  useRegistroDeApelacion,
  useResolucionDeApelacion,
} from '../hooks/useRevisionDeCasos';
import type { EstadoDeApelacion, ExpedienteDeCaso } from '../tipos';
import propios from './acciones.module.css';
import AvisoDeAccion from './AvisoDeAccion';
import { errorMasReciente } from './errorMasReciente';

const EXPLICACION: Record<EstadoDeApelacion, string> = {
  SIN_APELACION: 'Nadie ha apelado esta decisión.',
  PENDIENTE: 'Hay una apelación registrada esperando decisión.',
  ACEPTADA: 'La apelación se aceptó. Puedes reabrir el expediente si procede volver a revisarlo.',
  RECHAZADA: 'La apelación se evaluó y la decisión se mantuvo.',
};

/**
 * Las apelaciones del caso, tal como las gestiona el área administrativa.
 *
 * **La apelación no se presenta aquí.** Llega por el canal externo de soporte que la aplicación
 * muestra junto al aviso de la medida, y lo que hay en esta pantalla es el registro de lo recibido.
 * No existe ningún formulario para la persona sancionada, ni lo habrá en el MVP.
 *
 * Aceptar y reabrir son dos decisiones distintas a propósito: a veces basta con aceptar la apelación
 * y revocar la medida, sin volver a investigar el expediente.
 */
export default function ApelacionDelCaso({ expediente }: { expediente: ExpedienteDeCaso }) {
  const { caso, apelacion, puedeResolver } = expediente;
  const idCaso = caso.idCasoModeracion;

  const registro = useRegistroDeApelacion(idCaso);
  const resolucion = useResolucionDeApelacion(idCaso);
  const reapertura = useReaperturaDeCaso(idCaso);

  const cerrado = caso.estadoActual === 'CERRADO';
  const puedeRegistrar = puedeResolver && cerrado && apelacion !== 'PENDIENTE';
  const puedeResolverla = puedeResolver && apelacion === 'PENDIENTE';
  const puedeReabrir = puedeResolver && cerrado && apelacion === 'ACEPTADA';

  return (
    <section className={secciones.seccion} aria-labelledby="apelacion">
      <h2 className={secciones.tituloDeSeccion} id="apelacion">
        Apelación
      </h2>

      <p className={secciones.explicacion}>
        {EXPLICACION[apelacion]} Las apelaciones llegan por el canal externo de soporte, no desde
        Moica: aquí solo se registra lo recibido.
      </p>

      {!cerrado && apelacion !== 'PENDIENTE' && (
        <p className={secciones.explicacion}>
          Solo se registra una apelación sobre un caso cerrado con su decisión vigente.
        </p>
      )}

      {puedeRegistrar && <FormularioDeRegistro registro={registro} />}
      {puedeResolverla && <FormularioDeResolucion resolucion={resolucion} />}
      {puedeReabrir && <FormularioDeReapertura reapertura={reapertura} />}

      <AvisoDeAccion error={errorMasReciente(registro, resolucion, reapertura)} />
    </section>
  );
}

type Registro = ReturnType<typeof useRegistroDeApelacion>;
type Resolucion = ReturnType<typeof useResolucionDeApelacion>;
type Reapertura = ReturnType<typeof useReaperturaDeCaso>;

function FormularioDeRegistro({ registro }: { registro: Registro }) {
  const [relato, setRelato] = useState('');
  const vacio = relato.trim() === '';

  return (
    <form
      className={propios.formularioDeCierre}
      onSubmit={(evento) => {
        evento.preventDefault();
        if (!vacio) {
          registro.mutate(relato.trim());
        }
      }}
    >
      <label className={estilos.etiqueta} htmlFor="relato-apelacion">
        Qué expuso la persona
      </label>
      <textarea
        className={estilos.campo}
        id="relato-apelacion"
        name="relato-apelacion"
        rows={4}
        maxLength={3000}
        value={relato}
        onChange={(evento) => setRelato(evento.target.value)}
        aria-describedby="ayuda-relato"
      />
      <p className={secciones.explicacion} id="ayuda-relato">
        Copia lo esencial de lo que llegó por soporte. Queda en el historial del caso.{' '}
        {relato.length} de 3000 caracteres.
      </p>
      <button
        className={secciones.botonSecundario}
        type="submit"
        disabled={vacio || registro.isPending}
      >
        {registro.isPending ? 'Registrando…' : 'Registrar la apelación'}
      </button>
    </form>
  );
}

/**
 * Aceptar o rechazar, con su explicación.
 *
 * Van en el mismo formulario porque la decisión y su motivo se auditan juntos, igual que el cierre
 * del caso: registrar un resultado y dejar la explicación para después es lo que hace ilegible un
 * expediente meses más tarde.
 */
function FormularioDeResolucion({ resolucion }: { resolucion: Resolucion }) {
  const [aceptada, setAceptada] = useState(true);
  const [texto, setTexto] = useState('');
  const vacio = texto.trim() === '';

  return (
    <form
      className={propios.formularioDeCierre}
      onSubmit={(evento) => {
        evento.preventDefault();
        if (!vacio) {
          resolucion.mutate({ aceptada, resolucion: texto.trim() });
        }
      }}
    >
      <fieldset className={propios.grupoDeResultado}>
        <legend className={estilos.etiqueta}>Decisión sobre la apelación</legend>
        <label className={propios.opcion}>
          <input
            type="radio"
            name="decision-apelacion"
            value="ACEPTADA"
            checked={aceptada}
            onChange={() => setAceptada(true)}
          />
          Aceptarla — habilita reabrir el expediente
        </label>
        <label className={propios.opcion}>
          <input
            type="radio"
            name="decision-apelacion"
            value="RECHAZADA"
            checked={!aceptada}
            onChange={() => setAceptada(false)}
          />
          Rechazarla — la decisión vigente se mantiene
        </label>
      </fieldset>

      <label className={estilos.etiqueta} htmlFor="resolucion-apelacion">
        Resolución
      </label>
      <textarea
        className={estilos.campo}
        id="resolucion-apelacion"
        name="resolucion-apelacion"
        rows={3}
        maxLength={3000}
        value={texto}
        onChange={(evento) => setTexto(evento.target.value)}
      />
      <p className={secciones.explicacion}>
        Aceptarla no levanta la medida ni reabre el caso por sí sola: las dos cosas son decisiones
        aparte.
      </p>
      <button
        className={secciones.botonSecundario}
        type="submit"
        disabled={vacio || resolucion.isPending}
      >
        {resolucion.isPending ? 'Registrando…' : 'Registrar la decisión'}
      </button>
    </form>
  );
}

function FormularioDeReapertura({ reapertura }: { reapertura: Reapertura }) {
  const [motivo, setMotivo] = useState('');
  const vacio = motivo.trim() === '';

  return (
    <form
      className={propios.formularioDeCierre}
      onSubmit={(evento) => {
        evento.preventDefault();
        if (!vacio) {
          reapertura.mutate(motivo.trim());
        }
      }}
    >
      <label className={estilos.etiqueta} htmlFor="motivo-reapertura">
        Motivo para reabrir el caso
      </label>
      <textarea
        className={estilos.campo}
        id="motivo-reapertura"
        name="motivo-reapertura"
        rows={3}
        maxLength={2000}
        value={motivo}
        onChange={(evento) => setMotivo(evento.target.value)}
      />
      <p className={secciones.explicacion}>
        El expediente vuelve a revisión y deja de tener una decisión vigente. La resolución anterior
        se conserva en el historial, y la medida sigue aplicada hasta que alguien la revoque.
      </p>
      <button
        className={secciones.botonSecundario}
        type="submit"
        disabled={vacio || reapertura.isPending}
      >
        {reapertura.isPending ? 'Reabriendo…' : 'Reabrir el caso'}
      </button>
    </form>
  );
}
