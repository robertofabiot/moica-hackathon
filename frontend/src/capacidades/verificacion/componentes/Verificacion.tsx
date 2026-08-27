import { useState } from 'react';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { nombreDelEstado, nombreDelNivelSolicitado } from '../etiquetas';
import { useEstadoDeVerificacion } from '../hooks/useVerificacion';
import type { EstadoDeVerificacion, NivelSolicitado } from '../tipos';
import EnvioDeExpediente from './EnvioDeExpediente';
import HistorialDeVerificacion from './HistorialDeVerificacion';
import InsigniaDeVerificacion from './InsigniaDeVerificacion';
import propios from './verificacion.module.css';

/**
 * La verificación del perfil propio: qué nivel tiene, qué puede pedir y qué presentó antes.
 *
 * Es una sección del perfil, no una pantalla aparte: la verificación no es un trámite separado sino
 * lo que decide si ese perfil puede salir al público.
 *
 * Solo pinta y ordena. Los datos los trae su hook, el envío vive en {@link EnvioDeExpediente} y el
 * historial en {@link HistorialDeVerificacion}, cada uno con sus propios estados de carga y error.
 */
export default function Verificacion() {
  const estado = useEstadoDeVerificacion();
  const [enviando, setEnviando] = useState<NivelSolicitado | null>(null);

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-verificacion">
      <h2 className={secciones.tituloDeSeccion} id="titulo-verificacion">
        Verificación de tu perfil
      </h2>

      {estado.isPending && (
        <p className={secciones.estado} role="status">
          Cargando tu verificación…
        </p>
      )}

      {estado.isError && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {estado.error instanceof ErrorDeApi
            ? estado.error.message
            : 'No pudimos cargar tu verificación.'}{' '}
          <button
            className={estilos.enlaceDeTexto}
            type="button"
            onClick={() => void estado.refetch()}
          >
            Reintentar
          </button>
        </p>
      )}

      {estado.data !== undefined && (
        <>
          <p className={propios.encabezadoDelNivel}>
            <span>Nivel vigente:</span>
            <InsigniaDeVerificacion nivel={estado.data.nivelVerificacion} />
          </p>
          <p className={secciones.explicacion}>{estado.data.significado}</p>

          <p className={propios.explicacionDeLaInsignia}>
            Una insignia dice que Moica <strong>revisó la documentación que presentaste</strong> en
            un momento determinado. No garantiza la calidad futura de tu trabajo ni sustituye el
            criterio de quien te contrata: eso se sigue respaldando con tu portafolio y con las
            calificaciones de solicitudes completadas.
          </p>

          {enviando === null ? (
            <QueSePuedeHacer estado={estado.data} alElegirNivel={setEnviando} />
          ) : (
            <EnvioDeExpediente
              nivel={enviando}
              alTerminar={() => setEnviando(null)}
              alCancelar={() => setEnviando(null)}
            />
          )}

          <h3 className={propios.subtitulo}>Tus solicitudes</h3>
          <HistorialDeVerificacion />
        </>
      )}
    </section>
  );
}

/**
 * Qué ofrece la sección según el nivel vigente y lo que ya está en curso.
 *
 * Las dos banderas las decide el servidor y son las mismas que aplica al recibir el envío: aquí solo
 * sirven para no proponer algo que la API va a rechazar.
 */
function QueSePuedeHacer({
  estado,
  alElegirNivel,
}: {
  estado: EstadoDeVerificacion;
  alElegirNivel: (nivel: NivelSolicitado) => void;
}) {
  const abierta = estado.solicitudAbierta;

  return (
    <>
      {abierta !== null && (
        <p className={secciones.estado} role="status">
          {nombreDelNivelSolicitado(abierta.nivelSolicitado)}:{' '}
          <span className={secciones.etiquetaDeEstado}>
            {nombreDelEstado(abierta.estadoSolicitud)}
          </span>{' '}
          <span className={secciones.metadatoDelElemento}>
            Ya enviaste {abierta.documentos.length}{' '}
            {abierta.documentos.length === 1 ? 'documento' : 'documentos'}. Te avisaremos aquí
            cuando se resuelva.
          </span>
        </p>
      )}

      <div className={propios.acciones}>
        {estado.puedeSolicitarBasica && (
          <button className={estilos.boton} type="button" onClick={() => alElegirNivel('BASICA')}>
            Solicitar verificación básica
          </button>
        )}
        {estado.puedeSolicitarProfesional && (
          <button
            className={secciones.botonSecundario}
            type="button"
            onClick={() => alElegirNivel('PROFESIONAL')}
          >
            Solicitar verificación profesional
          </button>
        )}
      </div>

      {!estado.puedeSolicitarProfesional &&
        estado.nivelVerificacion === 'SIN_VERIFICAR' &&
        abierta === null && (
          <p className={secciones.explicacion}>
            La verificación profesional es opcional y llega después: primero necesitas la básica
            vigente.
          </p>
        )}
    </>
  );
}
