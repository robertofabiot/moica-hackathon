import { NavLink, useMatch } from 'react-router';

import {
  IconoDeBarraLateral,
  type IdentificadorDeItemDeBarraLateral,
} from './iconosDeBarraLateral';
import estilos from './BarraLateral.module.css';

export type { IdentificadorDeItemDeBarraLateral };

const ITEMS: Array<{
  id: IdentificadorDeItemDeBarraLateral;
  etiqueta: string;
}> = [
  { id: 'notificaciones', etiqueta: 'Notificaciones' },
  { id: 'inicio', etiqueta: 'Inicio' },
  { id: 'calendario', etiqueta: 'Calendario' },
  { id: 'mensajes', etiqueta: 'Mensajes' },
  { id: 'perfil', etiqueta: 'Perfil' },
  { id: 'pagos', etiqueta: 'Pagos' },
  { id: 'configuracion', etiqueta: 'Configuración' },
];

type PropiedadesDeBarraLateral = {
  itemActivo?: IdentificadorDeItemDeBarraLateral;
  tieneMensajesSinLeer?: boolean;
  destinos?: Partial<Record<IdentificadorDeItemDeBarraLateral, string>>;
};

/**
 * Barra lateral de iconos, fija a la izquierda.
 *
 * `comun` no conoce las rutas de cada capacidad: el padre pasa `destinos`.
 * Un ítem sin destino se pinta como botón para no inventar pantallas que
 * todavía no existen (chat, calendario, pagos).
 */
export function BarraLateral({
  itemActivo,
  tieneMensajesSinLeer = false,
  destinos,
}: PropiedadesDeBarraLateral) {
  return (
    <nav className={estilos.barra} aria-label="Navegación principal">
      <ul className={estilos.lista}>
        {ITEMS.map((item) => (
          <ItemDeBarra
            key={item.id}
            id={item.id}
            etiqueta={item.etiqueta}
            destino={destinos?.[item.id]}
            marcado={itemActivo === item.id}
            tieneAviso={item.id === 'mensajes' && tieneMensajesSinLeer}
          />
        ))}
      </ul>
    </nav>
  );
}

function ItemDeBarra({
  id,
  etiqueta,
  destino,
  marcado,
  tieneAviso,
}: {
  id: IdentificadorDeItemDeBarraLateral;
  etiqueta: string;
  destino: string | undefined;
  marcado: boolean;
  tieneAviso: boolean;
}) {
  if (destino === undefined) {
    return <ItemBoton id={id} etiqueta={etiqueta} marcado={marcado} tieneAviso={tieneAviso} />;
  }

  return (
    <ItemEnlace
      id={id}
      etiqueta={etiqueta}
      destino={destino}
      marcado={marcado}
      tieneAviso={tieneAviso}
    />
  );
}

function ItemBoton({
  id,
  etiqueta,
  marcado,
  tieneAviso,
}: {
  id: IdentificadorDeItemDeBarraLateral;
  etiqueta: string;
  marcado: boolean;
  tieneAviso: boolean;
}) {
  return (
    <li className={unirClases(estilos.item, marcado ? estilos.itemActivo : undefined)}>
      <button
        type="button"
        className={unirClases(estilos.control, marcado ? estilos.controlActivo : undefined)}
        aria-label={nombreAccesible(etiqueta, tieneAviso)}
        aria-current={marcado ? 'page' : undefined}
      >
        <ContenidoDeItem id={id} tieneAviso={tieneAviso} />
      </button>
    </li>
  );
}

function ItemEnlace({
  id,
  etiqueta,
  destino,
  marcado,
  tieneAviso,
}: {
  id: IdentificadorDeItemDeBarraLateral;
  etiqueta: string;
  destino: string;
  marcado: boolean;
  tieneAviso: boolean;
}) {
  const coincidencia = useMatch({ path: destino, end: true });
  const activo = marcado || coincidencia !== null;

  return (
    <li className={unirClases(estilos.item, activo ? estilos.itemActivo : undefined)}>
      <NavLink
        to={destino}
        end
        aria-label={nombreAccesible(etiqueta, tieneAviso)}
        className={unirClases(estilos.control, activo ? estilos.controlActivo : undefined)}
      >
        <ContenidoDeItem id={id} tieneAviso={tieneAviso} />
      </NavLink>
    </li>
  );
}

function ContenidoDeItem({
  id,
  tieneAviso,
}: {
  id: IdentificadorDeItemDeBarraLateral;
  tieneAviso: boolean;
}) {
  return (
    <>
      <span className={estilos.icono}>
        <IconoDeBarraLateral identificador={id} />
      </span>
      {tieneAviso && <span className={estilos.aviso} aria-hidden="true" />}
    </>
  );
}

function nombreAccesible(etiqueta: string, tieneAviso: boolean): string {
  return tieneAviso ? `${etiqueta}, hay avisos` : etiqueta;
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
