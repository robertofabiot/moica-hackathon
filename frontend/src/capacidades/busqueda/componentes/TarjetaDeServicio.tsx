import { Link } from 'react-router';

import {
  EstrellasCalificacion,
  IconoDeCategoria,
  InsigniaVerificado,
} from '../../../comun/componentes/ui';
import { precioEnTarjeta } from '../presentacion';
import { rutaDeDetalleDeServicio } from '../rutas';
import type { ResumenPublicoDeServicio } from '../tipos';
import estilos from './tarjeta.module.css';

/**
 * Tarjeta de un servicio visible: foto o icono, nota, precio e insignia.
 *
 * La nota es la del **prestador que publica**, no la del servicio: no existe una
 * reputación por servicio, así que todas las tarjetas de un mismo prestador
 * muestran la misma cifra.
 */
export default function TarjetaDeServicio({ servicio }: { servicio: ResumenPublicoDeServicio }) {
  const precio = precioEnTarjeta(servicio.precioReferencia);
  const estaVerificado = servicio.prestador.nivelVerificacion !== 'SIN_VERIFICAR';

  return (
    <li>
      <Link className={estilos.tarjeta} to={rutaDeDetalleDeServicio(servicio.idServicioPublicado)}>
        {servicio.imagenPrincipal === null ? (
          <div className={estilos.sinImagen}>
            <span className={estilos.iconoDeRespaldo}>
              <IconoDeCategoria nombreCategoria={servicio.nombreCategoria} />
            </span>
          </div>
        ) : (
          <img
            className={estilos.imagen}
            src={servicio.imagenPrincipal.urlImagen}
            alt={servicio.imagenPrincipal.textoAlternativo ?? servicio.nombre}
            loading="lazy"
          />
        )}
        <div className={estilos.cuerpo}>
          <div className={estilos.titular}>
            <h2 className={estilos.nombre}>{servicio.nombre}</h2>
            {estaVerificado ? <InsigniaVerificado /> : null}
          </div>
          <div className={estilos.filaMeta}>
            <EstrellasCalificacion
              calificacion={servicio.reputacionPrestador.promedio}
              totalCalificaciones={
                servicio.reputacionPrestador.cantidad === 0
                  ? undefined
                  : servicio.reputacionPrestador.cantidad
              }
            />
            <p className={estilos.precio}>
              {precio.prefijo === null ? (
                precio.valor
              ) : (
                <>
                  {precio.prefijo} <span className={estilos.monto}>{precio.valor}</span>
                </>
              )}
            </p>
          </div>
        </div>
      </Link>
    </li>
  );
}
