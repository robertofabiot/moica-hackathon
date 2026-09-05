import { useState } from 'react';
import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import AvisoDeAccion from '../componentes/AvisoDeAccion';
import { errorMasReciente } from '../componentes/errorMasReciente';
import { nombreDelEstadoDeCuenta } from '../etiquetas';
import {
  useCatalogoDeMedidas,
  useCreacionDeMedida,
  useEdicionDeMedida,
  useHabilitacionDeMedida,
} from '../hooks/useRevisionDeCasos';
import { RUTA_ADMIN_CASOS } from '../rutas';
import type { EstadoDeCuenta, MedidaAdministrativa } from '../tipos';
import propios from './casos.module.css';

/** Los estados que una medida puede imponer, con el plazo que cada uno implica. */
const ESTADOS_POSIBLES: { valor: string; etiqueta: string; temporal: boolean }[] = [
  { valor: '', etiqueta: 'Ninguno — solo queda registrada', temporal: false },
  { valor: 'RESTRINGIDA_TEMPORAL', etiqueta: 'Restringida temporalmente', temporal: true },
  { valor: 'SUSPENDIDA_TEMPORAL', etiqueta: 'Suspendida temporalmente', temporal: true },
  { valor: 'SUSPENDIDA_PERMANENTE', etiqueta: 'Suspendida permanentemente', temporal: false },
];

/**
 * El catálogo de medidas administrativas.
 *
 * Describe qué sanciones existen; no decide ninguna. El nivel de severidad ordena la lista para
 * quien elige y nada más: Moica no recomienda medidas ni escala sanciones por reincidencia.
 *
 * **No hay forma de eliminar una medida, y no es un olvido.** Una citada por un caso o por el
 * historial es la evidencia de una decisión. Lo que aquí se llama «dejar de ofrecerla» es
 * deshabilitarla: sigue describiendo lo que ya pasó y no vuelve a aplicarse.
 */
export default function CatalogoDeMedidas() {
  const catalogo = useCatalogoDeMedidas();
  const creacion = useCreacionDeMedida();
  const edicion = useEdicionDeMedida();
  const habilitacion = useHabilitacionDeMedida();

  const [editando, setEditando] = useState<number | null>(null);

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <p className={propios.migaDePan}>
          <Link to={RUTA_ADMIN_CASOS}>Casos de moderación</Link>
        </p>

        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>Catálogo de medidas</h1>
          <p className={secciones.explicacion}>
            Son las sanciones que una persona administradora puede elegir al resolver un caso. La
            severidad ordena la lista; no activa ninguna regla ni decide nada por sí sola.
          </p>
        </header>

        {catalogo.isPending && (
          <p className={secciones.estado} role="status">
            Cargando el catálogo…
          </p>
        )}

        {catalogo.isError && (
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {catalogo.error instanceof ErrorDeApi
              ? catalogo.error.message
              : 'No pudimos cargar el catálogo.'}{' '}
            <button
              className={estilos.enlaceDeTexto}
              type="button"
              onClick={() => void catalogo.refetch()}
            >
              Reintentar
            </button>
          </p>
        )}

        {catalogo.data !== undefined && (
          <section className={secciones.seccion} aria-labelledby="medidas">
            <h2 className={secciones.tituloDeSeccion} id="medidas">
              Medidas registradas
            </h2>

            {catalogo.data.length === 0 ? (
              <p className={secciones.vacio}>
                Todavía no hay ninguna medida. Crea la primera con el formulario de abajo: sin
                catálogo no se puede sancionar ningún caso.
              </p>
            ) : (
              <ul className={secciones.lista}>
                {catalogo.data.map((medida) => (
                  <li key={medida.idMedidaAdministrativa} className={secciones.elemento}>
                    {editando === medida.idMedidaAdministrativa ? (
                      <FormularioDeMedida
                        medida={medida}
                        enCurso={edicion.isPending}
                        alGuardar={(datos) =>
                          edicion.mutate(
                            { idMedida: medida.idMedidaAdministrativa, medida: datos },
                            { onSuccess: () => setEditando(null) }
                          )
                        }
                        alCancelar={() => setEditando(null)}
                      />
                    ) : (
                      <FichaDeMedida
                        medida={medida}
                        enCurso={habilitacion.isPending}
                        alEditar={() => setEditando(medida.idMedidaAdministrativa)}
                        alCambiarHabilitacion={() =>
                          habilitacion.mutate({
                            idMedida: medida.idMedidaAdministrativa,
                            habilitada: !medida.habilitada,
                          })
                        }
                      />
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
        )}

        <section className={secciones.seccion} aria-labelledby="nueva">
          <h2 className={secciones.tituloDeSeccion} id="nueva">
            Añadir una medida
          </h2>
          <FormularioDeMedida
            medida={null}
            enCurso={creacion.isPending}
            alGuardar={(datos, codigo) => creacion.mutate({ ...datos, codigo: codigo ?? '' })}
            alCancelar={null}
          />
        </section>

        <AvisoDeAccion error={errorMasReciente(creacion, edicion, habilitacion)} />
      </div>
    </main>
  );
}

/** Una medida ya registrada, con lo que se puede hacer con ella. */
function FichaDeMedida({
  medida,
  enCurso,
  alEditar,
  alCambiarHabilitacion,
}: {
  medida: MedidaAdministrativa;
  enCurso: boolean;
  alEditar: () => void;
  alCambiarHabilitacion: () => void;
}) {
  return (
    <>
      <div className={secciones.contenidoDelElemento}>
        <p className={secciones.tituloDelElemento}>
          {medida.nombre}{' '}
          <span className={secciones.etiquetaDeEstado}>
            {medida.habilitada ? 'Habilitada' : 'Deshabilitada'}
          </span>
        </p>
        <p className={secciones.metadatoDelElemento}>
          {medida.codigo} · Severidad {medida.nivelSeveridad} ·{' '}
          {medida.estadoCuentaResultante === null
            ? 'no cambia el acceso'
            : `deja la cuenta ${nombreDelEstadoDeCuenta(
                medida.estadoCuentaResultante
              ).toLowerCase()}`}
          {medida.requiereFechaFin ? ' · exige fecha de finalización' : ''}
        </p>
        {medida.descripcion !== null && (
          <p className={secciones.metadatoDelElemento}>{medida.descripcion}</p>
        )}
      </div>
      <div className={secciones.accionesDelElemento}>
        <button
          className={secciones.botonPequeno}
          type="button"
          onClick={alEditar}
          disabled={enCurso}
        >
          Editar
        </button>
        <button
          className={secciones.botonPequeno}
          type="button"
          onClick={alCambiarHabilitacion}
          disabled={enCurso}
        >
          {medida.habilitada ? 'Dejar de ofrecerla' : 'Volver a ofrecerla'}
        </button>
      </div>
    </>
  );
}

interface DatosDeMedida {
  nombre: string;
  descripcion: string | null;
  nivelSeveridad: number;
  estadoCuentaResultante: EstadoDeCuenta | null;
  requiereFechaFin: boolean;
}

/**
 * El formulario que sirve para crear y para editar.
 *
 * El código solo aparece al crear: identifica la medida ante las decisiones que ya la citaron, y
 * cambiarlo dejaría un historial hablando de algo que no existe.
 *
 * El plazo no se pregunta: lo decide el estado resultante. Los dos estados temporales terminan en
 * una fecha, así que la medida tiene que pedirla; los otros dos no terminan solos, así que pedirla
 * sería prometer una reactivación que nunca llegaría. Derivarlo aquí evita ofrecer una combinación
 * que el backend rechazaría con `MEDIDA_INCOHERENTE`.
 */
function FormularioDeMedida({
  medida,
  enCurso,
  alGuardar,
  alCancelar,
}: {
  medida: MedidaAdministrativa | null;
  enCurso: boolean;
  alGuardar: (datos: DatosDeMedida, codigo?: string) => void;
  alCancelar: (() => void) | null;
}) {
  const esNueva = medida === null;
  const idCampo = esNueva ? 'nueva' : String(medida.idMedidaAdministrativa);

  const [codigo, setCodigo] = useState('');
  const [nombre, setNombre] = useState(medida?.nombre ?? '');
  const [descripcion, setDescripcion] = useState(medida?.descripcion ?? '');
  const [severidad, setSeveridad] = useState(String(medida?.nivelSeveridad ?? 1));
  const [estado, setEstado] = useState(medida?.estadoCuentaResultante ?? '');

  const temporal = ESTADOS_POSIBLES.find((posible) => posible.valor === estado)?.temporal ?? false;
  const incompleto = nombre.trim() === '' || (esNueva && codigo.trim() === '');

  return (
    <form
      className={estilos.formulario}
      onSubmit={(evento) => {
        evento.preventDefault();
        if (incompleto) {
          return;
        }
        alGuardar(
          {
            nombre: nombre.trim(),
            descripcion: descripcion.trim() === '' ? null : descripcion.trim(),
            nivelSeveridad: Number(severidad),
            estadoCuentaResultante: estado === '' ? null : (estado as EstadoDeCuenta),
            requiereFechaFin: temporal,
          },
          esNueva ? codigo.trim().toUpperCase() : undefined
        );
        if (esNueva) {
          setCodigo('');
          setNombre('');
          setDescripcion('');
          setSeveridad('1');
          setEstado('');
        }
      }}
    >
      {esNueva && (
        <>
          <label className={estilos.etiqueta} htmlFor={`codigo-${idCampo}`}>
            Código
          </label>
          <input
            className={estilos.campo}
            id={`codigo-${idCampo}`}
            name="codigo"
            type="text"
            maxLength={50}
            value={codigo}
            onChange={(evento) => setCodigo(evento.target.value)}
            aria-describedby={`ayuda-codigo-${idCampo}`}
          />
          <p className={secciones.explicacion} id={`ayuda-codigo-${idCampo}`}>
            Mayúsculas, dígitos y guion bajo. No se puede cambiar después: es lo que identifica la
            medida en las decisiones ya tomadas.
          </p>
        </>
      )}

      <label className={estilos.etiqueta} htmlFor={`nombre-${idCampo}`}>
        Nombre
      </label>
      <input
        className={estilos.campo}
        id={`nombre-${idCampo}`}
        name="nombre"
        type="text"
        maxLength={100}
        value={nombre}
        onChange={(evento) => setNombre(evento.target.value)}
      />

      <label className={estilos.etiqueta} htmlFor={`descripcion-${idCampo}`}>
        Descripción
      </label>
      <textarea
        className={estilos.campo}
        id={`descripcion-${idCampo}`}
        name="descripcion"
        rows={2}
        maxLength={2000}
        value={descripcion}
        onChange={(evento) => setDescripcion(evento.target.value)}
      />

      <label className={estilos.etiqueta} htmlFor={`severidad-${idCampo}`}>
        Nivel de severidad
      </label>
      <input
        className={estilos.campo}
        id={`severidad-${idCampo}`}
        name="nivelSeveridad"
        type="number"
        min={1}
        max={100}
        value={severidad}
        onChange={(evento) => setSeveridad(evento.target.value)}
        aria-describedby={`ayuda-severidad-${idCampo}`}
      />
      <p className={secciones.explicacion} id={`ayuda-severidad-${idCampo}`}>
        Solo ordena la lista para quien elige. No activa ninguna regla automática.
      </p>

      <label className={estilos.etiqueta} htmlFor={`estado-${idCampo}`}>
        Estado en el que deja la cuenta
      </label>
      <select
        className={estilos.campo}
        id={`estado-${idCampo}`}
        name="estadoCuentaResultante"
        value={estado}
        onChange={(evento) => setEstado(evento.target.value as EstadoDeCuenta | '')}
        aria-describedby={`ayuda-estado-${idCampo}`}
      >
        {ESTADOS_POSIBLES.map((posible) => (
          <option key={posible.valor} value={posible.valor}>
            {posible.etiqueta}
          </option>
        ))}
      </select>
      <p className={secciones.explicacion} id={`ayuda-estado-${idCampo}`}>
        {temporal
          ? 'Al aplicarla habrá que indicar cuándo termina, y terminará sola en esa fecha.'
          : 'No termina sola: solo se levanta revocándola.'}
      </p>

      <div className={secciones.accionesDelElemento}>
        <button
          className={secciones.botonSecundario}
          type="submit"
          disabled={incompleto || enCurso}
        >
          {enCurso ? 'Guardando…' : esNueva ? 'Añadir la medida' : 'Guardar los cambios'}
        </button>
        {alCancelar !== null && (
          <button
            className={secciones.botonPequeno}
            type="button"
            onClick={alCancelar}
            disabled={enCurso}
          >
            Cancelar
          </button>
        )}
      </div>
    </form>
  );
}
