import { useId, useState } from 'react';
import type { z } from 'zod';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import {
  nombreDelNivelSolicitado,
  nombreDelTipoDeDocumento,
  tamanoLegible,
  TIPOS_DE_DOCUMENTO,
} from '../etiquetas';
import { esquemaDeArchivo, queLeFaltaAlExpediente } from '../esquemas';
import { useEnvioDeExpediente } from '../hooks/useVerificacion';
import type { DocumentoElegido, NivelSolicitado, TipoDeDocumento } from '../tipos';
import propios from './verificacion.module.css';

/**
 * Armar y enviar un expediente: elegir archivos, decir qué es cada uno y confirmar.
 *
 * El envío es una sola petición con todo dentro, así que aquí no se sube nada hasta confirmar: lo
 * que se ve antes es una lista local que se puede corregir entera. Quitar un archivo antes de
 * enviar no deja rastro en ninguna parte porque nunca salió del navegador.
 *
 * El paso de confirmación no es un adorno: enviar un expediente publica documentos personales hacia
 * una revisión que después no se puede editar, y conviene decirlo antes y no después.
 */
export default function EnvioDeExpediente({
  nivel,
  alTerminar,
  alCancelar,
}: {
  nivel: NivelSolicitado;
  alTerminar: () => void;
  alCancelar: () => void;
}) {
  const envio = useEnvioDeExpediente();
  const identificador = useId();
  const [elegidos, setElegidos] = useState<DocumentoElegido[]>([]);
  const [rechazados, setRechazados] = useState<string[]>([]);
  const [confirmando, setConfirmando] = useState(false);

  const tipoPorOmision: TipoDeDocumento = nivel === 'BASICA' ? 'IDENTIDAD' : 'CERTIFICACION';
  const loQueFalta = queLeFaltaAlExpediente(
    nivel,
    elegidos.map((elegido) => elegido.tipoDocumento)
  );

  const elegir = (evento: React.ChangeEvent<HTMLInputElement>) => {
    const admitidos: DocumentoElegido[] = [];
    const problemas: string[] = [];

    for (const archivo of Array.from(evento.target.files ?? [])) {
      const revision = esquemaDeArchivo.safeParse({
        tipoMime: archivo.type,
        tamanoBytes: archivo.size,
      });

      if (revision.success) {
        admitidos.push({
          id: `${archivo.name}-${archivo.size}-${admitidos.length}-${Date.now()}`,
          archivo,
          tipoDocumento: tipoPorOmision,
        });
      } else {
        problemas.push(`${archivo.name}: ${primerProblema(revision.error)}`);
      }
    }

    setElegidos((actuales) => [...actuales, ...admitidos]);
    setRechazados(problemas);
    setConfirmando(false);
    // El campo se limpia siempre: si no, volver a elegir el mismo archivo tras
    // quitarlo no dispararía otro `change`.
    evento.target.value = '';
  };

  const quitar = (id: string) => {
    setElegidos((actuales) => actuales.filter((elegido) => elegido.id !== id));
    setConfirmando(false);
  };

  const cambiarTipo = (id: string, tipoDocumento: TipoDeDocumento) => {
    setElegidos((actuales) =>
      actuales.map((elegido) => (elegido.id === id ? { ...elegido, tipoDocumento } : elegido))
    );
  };

  const enviar = () => {
    envio.mutate({ nivel, documentos: elegidos }, { onSuccess: alTerminar });
  };

  return (
    <div className={propios.envio}>
      <h3 className={propios.subtitulo}>
        Enviar tu {nombreDelNivelSolicitado(nivel).toLowerCase()}
      </h3>

      <p className={propios.avisoDePrivacidad} role="note">
        <strong>Tus documentos no serán públicos.</strong> Se guardan en un almacenamiento privado y
        solo puede abrirlos una persona administradora de Moica para revisar tu solicitud. No
        aparecen en tu perfil, ni en el descubrimiento, ni se comparten con clientes.
      </p>

      {envio.error !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeDe(envio.error)}
        </p>
      )}

      {rechazados.length > 0 && (
        <ul className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {rechazados.map((problema) => (
            <li key={problema}>{problema}</li>
          ))}
        </ul>
      )}

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor={`${identificador}-archivos`}>
          Elige tus documentos
        </label>
        <input
          id={`${identificador}-archivos`}
          type="file"
          multiple
          accept="image/jpeg,image/png,application/pdf"
          onChange={elegir}
          disabled={envio.isPending}
          aria-describedby={`${identificador}-pista`}
        />
        <p className={estilos.pista} id={`${identificador}-pista`}>
          JPEG, PNG o PDF, hasta 5 MB cada uno. Puedes elegir varios a la vez.
        </p>
      </div>

      {elegidos.length === 0 ? (
        <p className={secciones.vacio}>Todavía no has elegido ningún documento.</p>
      ) : (
        <ul className={secciones.lista}>
          {elegidos.map((elegido) => (
            <li className={secciones.elemento} key={elegido.id}>
              <p className={secciones.contenidoDelElemento}>{elegido.archivo.name}</p>
              <p className={secciones.metadatoDelElemento}>{tamanoLegible(elegido.archivo.size)}</p>
              <div className={estilos.campo}>
                <label className={estilos.etiqueta} htmlFor={`${identificador}-${elegido.id}`}>
                  Qué es este documento
                </label>
                <select
                  className={estilos.entrada}
                  id={`${identificador}-${elegido.id}`}
                  value={elegido.tipoDocumento}
                  onChange={(evento) =>
                    cambiarTipo(elegido.id, evento.target.value as TipoDeDocumento)
                  }
                  disabled={envio.isPending}
                >
                  {TIPOS_DE_DOCUMENTO.map((tipo) => (
                    <option key={tipo} value={tipo}>
                      {nombreDelTipoDeDocumento(tipo)}
                    </option>
                  ))}
                </select>
              </div>
              <div className={secciones.accionesDelElemento}>
                <button
                  className={secciones.botonPequeno}
                  type="button"
                  onClick={() => quitar(elegido.id)}
                  disabled={envio.isPending}
                >
                  Quitar {elegido.archivo.name}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {loQueFalta !== null && elegidos.length > 0 && (
        <p className={estilos.error} role="status">
          {loQueFalta}
        </p>
      )}

      {confirmando ? (
        <div className={propios.confirmacion} role="group" aria-label="Confirmar el envío">
          <p>
            Vas a enviar {elegidos.length} {elegidos.length === 1 ? 'documento' : 'documentos'} para
            tu {nombreDelNivelSolicitado(nivel).toLowerCase()}.{' '}
            <strong>Después de enviarlo no podrás editarlo ni sustituirlo:</strong> si algo cambia,
            tendrás que presentar una solicitud nueva.
          </p>
          <div className={secciones.accionesDelElemento}>
            <button
              className={estilos.boton}
              type="button"
              onClick={enviar}
              disabled={envio.isPending}
            >
              {envio.isPending ? 'Enviando el expediente…' : 'Confirmar y enviar'}
            </button>
            <button
              className={secciones.botonSecundario}
              type="button"
              onClick={() => setConfirmando(false)}
              disabled={envio.isPending}
            >
              Seguir editando
            </button>
          </div>
        </div>
      ) : (
        <div className={secciones.accionesDelElemento}>
          <button
            className={estilos.boton}
            type="button"
            onClick={() => setConfirmando(true)}
            disabled={loQueFalta !== null || envio.isPending}
          >
            Revisar y enviar
          </button>
          <button
            className={secciones.botonSecundario}
            type="button"
            onClick={alCancelar}
            disabled={envio.isPending}
          >
            Cancelar
          </button>
        </div>
      )}
    </div>
  );
}

/** El primer motivo por el que un archivo no se admitió, con un respaldo si Zod no diera ninguno. */
function primerProblema(error: z.ZodError): string {
  return error.issues[0]?.message ?? 'El archivo no se admite.';
}

function mensajeDe(error: unknown): string {
  if (error instanceof ErrorDeApi) {
    return error.message;
  }
  return error instanceof Error
    ? error.message
    : 'No pudimos enviar tu expediente. Inténtalo otra vez.';
}
