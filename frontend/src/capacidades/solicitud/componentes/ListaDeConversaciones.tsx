import { useState } from 'react';

import { ErrorDeApi } from '../../../comun/api';
import { Entrada, IconoLupa } from '../../../comun/componentes/ui';
import estilosDeFormulario from '../../../comun/estilos/formulario.module.css';
import {
  filtrarConversaciones,
  inicialesDeNombre,
  instanteDeLista,
  nombreDeContraparte,
  nombreDelEstado,
} from '../presentacion';
import type { ResumenDeSolicitudServicio } from '../tipos';
import ItemConversacion from './ItemConversacion';
import estilos from './ListaDeConversaciones.module.css';

export type UltimoMensajeDeBandeja = {
  contenido: string;
  fechaEnvio: string;
};

type PropiedadesDeListaDeConversaciones = {
  conversaciones: ResumenDeSolicitudServicio[];
  idUsuario: number | undefined;
  idSeleccionado: number | undefined;
  ultimosMensajes?: Readonly<Record<number, UltimoMensajeDeBandeja>>;
  cargando: boolean;
  error: Error | null;
  alReintentar: () => void;
  alSeleccionar: (idSolicitud: number) => void;
};

/**
 * Panel izquierdo de la mensajería: título, búsqueda y la lista desplazable.
 */
export default function ListaDeConversaciones({
  conversaciones,
  idUsuario,
  idSeleccionado,
  ultimosMensajes,
  cargando,
  error,
  alReintentar,
  alSeleccionar,
}: PropiedadesDeListaDeConversaciones) {
  const [consulta, setConsulta] = useState('');
  const visibles = filtrarConversaciones(conversaciones, consulta, idUsuario);
  const buscando = consulta.trim() !== '';

  return (
    <section className={estilos.bandeja} aria-labelledby="titulo-mensajes">
      <header className={estilos.encabezado}>
        <h1 className={estilos.titulo} id="titulo-mensajes">
          Mensajes
        </h1>
        <Entrada
          type="search"
          icono={<IconoLupa />}
          placeholder="Buscar conversaciones"
          aria-label="Buscar conversaciones por nombre o servicio"
          value={consulta}
          onChange={(evento) => setConsulta(evento.target.value)}
        />
      </header>

      {cargando ? (
        <p className={estilos.estado} role="status">
          Cargando conversaciones…
        </p>
      ) : null}

      {error !== null ? (
        <p
          className={`${estilosDeFormulario.aviso} ${estilosDeFormulario.avisoDeError} ${estilos.aviso}`}
          role="alert"
        >
          {error instanceof ErrorDeApi ? error.message : 'No pudimos cargar las conversaciones.'}{' '}
          <button
            className={estilosDeFormulario.enlaceDeTexto}
            type="button"
            onClick={alReintentar}
          >
            Reintentar
          </button>
        </p>
      ) : null}

      {!cargando && conversaciones.length === 0 && error === null ? (
        <p className={estilos.estado} role="status">
          Todavía no tienes conversaciones. Aparecerán aquí cuando una solicitud sea aceptada.
        </p>
      ) : null}

      {!cargando && conversaciones.length > 0 && visibles.length === 0 && buscando ? (
        <p className={estilos.estado} role="status">
          Ninguna conversación coincide con la búsqueda.
        </p>
      ) : null}

      {visibles.length > 0 ? (
        <ul className={estilos.lista}>
          {visibles.map((item) => {
            const nombre = nombreDeContraparte(item, idUsuario);
            const ultimo = ultimosMensajes?.[item.idSolicitudServicio];
            const fechaDeFila = ultimo?.fechaEnvio ?? item.fechaCreacion;
            return (
              <li key={item.idSolicitudServicio}>
                <ItemConversacion
                  nombre={nombre}
                  servicio={item.nombreServicio}
                  extracto={ultimo?.contenido ?? nombreDelEstado(item.estadoActual)}
                  instante={instanteDeLista(fechaDeFila)}
                  fechaIso={fechaDeFila}
                  iniciales={inicialesDeNombre(nombre)}
                  seleccionado={item.idSolicitudServicio === idSeleccionado}
                  alSeleccionar={() => alSeleccionar(item.idSolicitudServicio)}
                />
              </li>
            );
          })}
        </ul>
      ) : null}
    </section>
  );
}
