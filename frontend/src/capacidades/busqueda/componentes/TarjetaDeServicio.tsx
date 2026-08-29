import { Link } from 'react-router';

import { InsigniaDeVerificacion } from '../../verificacion';
import { precioVisible } from '../presentacion';
import { rutaDeDetalleDeServicio } from '../rutas';
import type { ResumenPublicoDeServicio } from '../tipos';
import estilos from './tarjeta.module.css';

/** Tarjeta de un servicio visible: precio o «A convenir», prestador e insignia. */
export default function TarjetaDeServicio({ servicio }: { servicio: ResumenPublicoDeServicio }) {
  return (
    <li>
      <Link className={estilos.tarjeta} to={rutaDeDetalleDeServicio(servicio.idServicioPublicado)}>
        {servicio.imagenPrincipal === null ? (
          <div className={estilos.sinImagen}>Sin imagen</div>
        ) : (
          <img
            className={estilos.imagen}
            src={servicio.imagenPrincipal.urlImagen}
            alt={servicio.imagenPrincipal.textoAlternativo ?? servicio.nombre}
            loading="lazy"
          />
        )}
        <h2 className={estilos.nombre}>{servicio.nombre}</h2>
        <p className={estilos.meta}>
          {servicio.nombreCategoria} · {servicio.nombreSubcategoria}
        </p>
        <p className={estilos.precio}>{precioVisible(servicio.precioReferencia)}</p>
        <p className={estilos.meta}>
          {servicio.prestador.nombrePublico} ·{' '}
          {servicio.prestador.municipioPrincipal.nombreMunicipio}
        </p>
        <p>
          <InsigniaDeVerificacion nivel={servicio.prestador.nivelVerificacion} />
        </p>
      </Link>
    </li>
  );
}
