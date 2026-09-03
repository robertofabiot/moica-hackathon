import estilos from './BurbujaMensaje.module.css';

type PropiedadesDeBurbujaMensaje = {
  contenido: string;
  instante: string;
  esPropio: boolean;
};

/**
 * Una burbuja de chat. El lado, el fondo y la esquina recortada dicen de quién es;
 * la hora queda abajo a la derecha en ambos casos.
 */
export default function BurbujaMensaje({
  contenido,
  instante,
  esPropio,
}: PropiedadesDeBurbujaMensaje) {
  return (
    <article className={unirClases(estilos.burbuja, esPropio ? estilos.propia : estilos.ajena)}>
      <p className={estilos.texto}>{contenido}</p>
      <p className={estilos.hora}>{instante}</p>
    </article>
  );
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
