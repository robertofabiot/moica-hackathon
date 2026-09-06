import estilos from './ItemConversacion.module.css';

type PropiedadesDeAvatarDeChat = {
  nombre: string;
  iniciales: string;
  urlFoto?: string;
  grande?: boolean;
};

/** Avatar circular: foto si hay, si no las iniciales sobre el fondo secundario. */
export function AvatarDeChat({
  nombre,
  iniciales,
  urlFoto,
  grande = false,
}: PropiedadesDeAvatarDeChat) {
  return (
    <span className={unirClases(estilos.avatar, grande ? estilos.avatarGrande : undefined)}>
      {urlFoto !== undefined && urlFoto !== '' ? (
        <img className={estilos.foto} src={urlFoto} alt="" />
      ) : (
        <span aria-hidden="true">{iniciales || nombre.slice(0, 1).toUpperCase()}</span>
      )}
    </span>
  );
}

type PropiedadesDeItemConversacion = {
  nombre: string;
  servicio: string;
  extracto: string;
  instante: string;
  fechaIso: string;
  iniciales: string;
  urlFoto?: string;
  seleccionado: boolean;
  alSeleccionar: () => void;
};

/**
 * Una fila de la bandeja: avatar, nombre, servicio, extracto y hora.
 *
 * Es un botón porque elige la conversación en esta misma pantalla, no navega a otra.
 */
export default function ItemConversacion({
  nombre,
  servicio,
  extracto,
  instante,
  fechaIso,
  iniciales,
  urlFoto,
  seleccionado,
  alSeleccionar,
}: PropiedadesDeItemConversacion) {
  return (
    <button
      type="button"
      className={unirClases(estilos.item, seleccionado ? estilos.seleccionado : undefined)}
      aria-current={seleccionado ? 'true' : undefined}
      onClick={alSeleccionar}
    >
      <AvatarDeChat nombre={nombre} iniciales={iniciales} urlFoto={urlFoto} />
      <span className={estilos.cuerpo}>
        <span className={estilos.filaSuperior}>
          <span className={estilos.nombre}>{nombre}</span>
          <time className={estilos.instante} dateTime={fechaIso}>
            {instante}
          </time>
        </span>
        <span className={estilos.servicio}>{servicio}</span>
        <span className={estilos.extracto}>{extracto}</span>
      </span>
    </button>
  );
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
