import secciones from '../../../comun/estilos/secciones.module.css';
import type { ImagenDeServicio } from '../tipos';
import propios from './acciones.module.css';

/**
 * Las evidencias que ya existían del trato reportado.
 *
 * Hoy son las imágenes del servicio contratado, que es lo único material que Moica guarda sobre lo
 * que se acordó. No hay forma de adjuntar nada nuevo a un caso: un reporte se presenta con motivo y
 * descripción, y el expediente muestra lo que ya estaba.
 */
export default function EvidenciasDelCaso({ imagenes }: { imagenes: ImagenDeServicio[] }) {
  return (
    <section className={secciones.seccion} aria-labelledby="evidencias">
      <h2 className={secciones.tituloDeSeccion} id="evidencias">
        Evidencias del servicio
      </h2>

      {imagenes.length === 0 ? (
        <p className={secciones.vacio}>El servicio contratado no tiene imágenes publicadas.</p>
      ) : (
        <ul className={propios.galeria}>
          {imagenes.map((imagen) => (
            <li key={imagen.idImagenServicioPublicado}>
              <img
                className={propios.evidencia}
                src={imagen.urlImagen}
                /*
                 * Sin texto alternativo propio la imagen queda como decorativa:
                 * inventarle una descripción sería afirmar sobre una evidencia
                 * algo que nadie escribió.
                 */
                alt={imagen.textoAlternativo ?? ''}
                loading="lazy"
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
