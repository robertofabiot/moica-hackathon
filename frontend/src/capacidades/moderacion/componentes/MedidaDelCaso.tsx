import { useState } from 'react';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { fechaLegible, nombreDelEstadoDeCuenta } from '../etiquetas';
import {
  useAplicacionDeMedida,
  useCatalogoDeMedidas,
  useRevocacionDeMedida,
} from '../hooks/useRevisionDeCasos';
import type { ExpedienteDeCaso, MedidaAdministrativa } from '../tipos';
import propios from './acciones.module.css';
import AvisoDeAccion from './AvisoDeAccion';
import { errorMasReciente } from './errorMasReciente';

/**
 * La medida administrativa del caso: cuál está vigente y qué se puede hacer con ella.
 *
 * La sanción **la elige siempre una persona**: esta pantalla muestra el catálogo y espera. No
 * propone ninguna, no la ordena por lo grave que fue el caso y no marca ninguna por omisión.
 *
 * El aviso de error vive fuera de los bloques de acción, igual que en la revisión: un 409 cambia
 * justo lo que decide qué acciones caben, y si el aviso viviera dentro del formulario, el refresco
 * que sigue al fallo se llevaría por delante la única explicación de por qué la acción no salió.
 */
export default function MedidaDelCaso({ expediente }: { expediente: ExpedienteDeCaso }) {
  const { caso, medidaVigente, puedeResolver } = expediente;
  const idCaso = caso.idCasoModeracion;

  const catalogo = useCatalogoDeMedidas();
  const aplicacion = useAplicacionDeMedida(idCaso);
  const revocacion = useRevocacionDeMedida(idCaso);

  /**
   * Si el servidor ya rechazó una aplicación por existir otra medida vigente.
   *
   * Cubre la carrera: la pantalla se cargó sin medida y alguien sancionó a esa persona mientras
   * tanto. Aunque `medidaVigente` llegara vacío, a partir del 409 hay que confirmar igual.
   */
  const conflictoDeReemplazo =
    aplicacion.error instanceof ErrorDeApi &&
    aplicacion.error.codigo === 'MEDIDA_VIGENTE_EXISTENTE';

  const exigeConfirmacion = medidaVigente !== null || conflictoDeReemplazo;
  const laSostieneOtroCaso = medidaVigente !== null && !medidaVigente.esDeEsteCaso;

  const puedeAplicar =
    puedeResolver && caso.estadoActual === 'CERRADO' && caso.resultadoActual === 'PROCEDENTE';
  const puedeRevocar = puedeResolver && medidaVigente !== null && medidaVigente.esDeEsteCaso;

  return (
    <section className={secciones.seccion} aria-labelledby="medida">
      <h2 className={secciones.tituloDeSeccion} id="medida">
        Medida administrativa
      </h2>

      <p className={secciones.explicacion}>
        Cuenta reportada: {nombreDelEstadoDeCuenta(expediente.estadoCuentaReportada).toLowerCase()}.{' '}
        {medidaVigente === null
          ? 'No tiene ninguna medida vigente.'
          : `Sostiene «${medidaVigente.nombre}»${
              medidaVigente.fechaFinMedida === null
                ? ' sin fecha de finalización'
                : ` hasta el ${fechaLegible(medidaVigente.fechaFinMedida)}`
            }.`}
      </p>

      {laSostieneOtroCaso && medidaVigente !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="status">
          Esa medida la impuso el caso {medidaVigente.idCasoModeracion}, no este. Cada cuenta
          sostiene una sola a la vez: aplicar otra aquí la sustituirá.
        </p>
      )}

      {!puedeAplicar && !puedeRevocar && (
        <p className={secciones.explicacion}>
          {caso.estadoActual === 'CERRADO' && caso.resultadoActual === 'DESESTIMADO'
            ? 'El caso se desestimó, así que no procede ninguna medida.'
            : 'Una medida solo se aplica desde un caso cerrado como procedente, y solo por quien lo tiene asignado.'}
        </p>
      )}

      {puedeAplicar && (
        <FormularioDeMedida
          medidas={catalogo.data ?? []}
          cargandoCatalogo={catalogo.isPending}
          exigeConfirmacion={exigeConfirmacion}
          nombreVigente={medidaVigente?.nombre ?? null}
          aplicacion={aplicacion}
        />
      )}

      {puedeRevocar && <FormularioDeRevocacion revocacion={revocacion} />}

      <AvisoDeAccion error={errorMasReciente(aplicacion, revocacion)} />
    </section>
  );
}

type Aplicacion = ReturnType<typeof useAplicacionDeMedida>;
type Revocacion = ReturnType<typeof useRevocacionDeMedida>;

/**
 * El formulario con el que una persona elige la medida.
 *
 * Solo ofrece las habilitadas. Una deshabilitada puede seguir apareciendo en el historial —nunca se
 * borra— pero no vuelve a aplicarse, y si alguien la retirara entre que se pintó esto y se envió, el
 * backend responde 409 y el aviso lo explica.
 */
function FormularioDeMedida({
  medidas,
  cargandoCatalogo,
  exigeConfirmacion,
  nombreVigente,
  aplicacion,
}: {
  medidas: MedidaAdministrativa[];
  cargandoCatalogo: boolean;
  exigeConfirmacion: boolean;
  nombreVigente: string | null;
  aplicacion: Aplicacion;
}) {
  const [elegida, setElegida] = useState('');
  const [fechaFin, setFechaFin] = useState('');
  const [justificacion, setJustificacion] = useState('');
  const [confirmado, setConfirmado] = useState(false);

  const aplicables = medidas.filter((medida) => medida.habilitada);
  const medida = aplicables.find(
    (candidata) => String(candidata.idMedidaAdministrativa) === elegida
  );

  const faltaPlazo = medida?.requiereFechaFin === true && fechaFin === '';
  const faltaConfirmar = exigeConfirmacion && !confirmado;
  const incompleto =
    medida === undefined || justificacion.trim() === '' || faltaPlazo || faltaConfirmar;

  return (
    <form
      className={propios.formularioDeCierre}
      onSubmit={(evento) => {
        evento.preventDefault();
        if (medida === undefined || incompleto) {
          return;
        }
        aplicacion.mutate({
          idMedidaAdministrativa: medida.idMedidaAdministrativa,
          // El campo del navegador da una hora local sin zona; la API la espera con la suya.
          fechaFinMedida: medida.requiereFechaFin ? new Date(fechaFin).toISOString() : null,
          justificacion: justificacion.trim(),
          confirmaReemplazo: exigeConfirmacion,
        });
      }}
    >
      <label className={estilos.etiqueta} htmlFor="medida-elegida">
        Medida que vas a aplicar
      </label>
      <select
        className={estilos.campo}
        id="medida-elegida"
        value={elegida}
        onChange={(evento) => {
          setElegida(evento.target.value);
          setFechaFin('');
        }}
        disabled={cargandoCatalogo || aplicacion.isPending}
      >
        <option value="">Elige una medida del catálogo</option>
        {aplicables.map((candidata) => (
          <option key={candidata.idMedidaAdministrativa} value={candidata.idMedidaAdministrativa}>
            {candidata.nombre}
          </option>
        ))}
      </select>

      {!cargandoCatalogo && aplicables.length === 0 && (
        <p className={secciones.explicacion}>
          El catálogo no tiene ninguna medida habilitada. Añade una antes de sancionar.
        </p>
      )}

      {medida !== undefined && (
        <p className={secciones.explicacion}>
          {medida.estadoCuentaResultante === null
            ? 'Queda registrada en el expediente y no cambia el acceso de la cuenta.'
            : `Dejará la cuenta ${nombreDelEstadoDeCuenta(
                medida.estadoCuentaResultante
              ).toLowerCase()}.`}{' '}
          {medida.descripcion}
        </p>
      )}

      {medida?.requiereFechaFin === true && (
        <>
          <label className={estilos.etiqueta} htmlFor="fin-de-la-medida">
            Hasta cuándo
          </label>
          <input
            className={estilos.campo}
            id="fin-de-la-medida"
            name="fin-de-la-medida"
            type="datetime-local"
            value={fechaFin}
            onChange={(evento) => setFechaFin(evento.target.value)}
          />
          <p className={secciones.explicacion}>
            Al llegar esa fecha, la medida termina sola y la cuenta vuelve a estar activa.
          </p>
        </>
      )}

      <label className={estilos.etiqueta} htmlFor="justificacion">
        Por qué la aplicas
      </label>
      <textarea
        className={estilos.campo}
        id="justificacion"
        name="justificacion"
        rows={3}
        maxLength={2000}
        value={justificacion}
        onChange={(evento) => setJustificacion(evento.target.value)}
        aria-describedby="ayuda-justificacion"
      />
      <p className={secciones.explicacion} id="ayuda-justificacion">
        Queda en el historial del caso. {justificacion.length} de 2000 caracteres.
      </p>

      {exigeConfirmacion && (
        <label className={propios.opcion}>
          <input
            type="checkbox"
            name="confirma-reemplazo"
            checked={confirmado}
            onChange={(evento) => setConfirmado(evento.target.checked)}
          />
          Entiendo que esto revocará
          {nombreVigente === null ? ' la medida vigente' : ` «${nombreVigente}»`} y la sustituirá
          por la que elegí.
        </label>
      )}

      <button
        className={secciones.botonSecundario}
        type="submit"
        disabled={incompleto || aplicacion.isPending}
      >
        {aplicacion.isPending
          ? 'Aplicando…'
          : exigeConfirmacion
            ? 'Sustituir la medida vigente'
            : 'Aplicar la medida'}
      </button>
    </form>
  );
}

/** Revocar exige motivo por la misma razón que resolver: una decisión sin explicación no se audita. */
function FormularioDeRevocacion({ revocacion }: { revocacion: Revocacion }) {
  const [motivo, setMotivo] = useState('');
  const vacio = motivo.trim() === '';

  return (
    <form
      className={propios.formularioDeCierre}
      onSubmit={(evento) => {
        evento.preventDefault();
        if (!vacio) {
          revocacion.mutate(motivo.trim());
        }
      }}
    >
      <label className={estilos.etiqueta} htmlFor="motivo-revocacion">
        Motivo para revocar la medida
      </label>
      <textarea
        className={estilos.campo}
        id="motivo-revocacion"
        name="motivo-revocacion"
        rows={3}
        maxLength={2000}
        value={motivo}
        onChange={(evento) => setMotivo(evento.target.value)}
      />
      <p className={secciones.explicacion}>
        Al revocarla, la cuenta vuelve a estar activa de inmediato.
      </p>
      <button
        className={secciones.botonSecundario}
        type="submit"
        disabled={vacio || revocacion.isPending}
      >
        {revocacion.isPending ? 'Revocando…' : 'Revocar la medida'}
      </button>
    </form>
  );
}
