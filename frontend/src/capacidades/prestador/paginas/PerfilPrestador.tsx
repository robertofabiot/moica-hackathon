import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import { Portafolio } from '../../portafolio';
import Contactos from '../componentes/Contactos';
import Disponibilidad from '../componentes/Disponibilidad';
import FormularioDePerfil from '../componentes/FormularioDePerfil';
import ImagenDePerfil from '../componentes/ImagenDePerfil';
import { usePerfilPrestador } from '../hooks/usePerfilPrestador';
import secciones from '../../../comun/estilos/secciones.module.css';
import propios from './prestador.module.css';

/**
 * Perfil de prestador: crearlo y administrarlo.
 *
 * Mientras la cuenta no tenga perfil solo se ofrece el formulario de creación; lo que cuelga del
 * perfil —imagen, disponibilidad, contactos y portafolio— aparece cuando ya existe algo a lo que
 * colgarlo.
 *
 * Solo pinta y ordena: cada sección se encarga de sus datos y de sus estados de carga y error, de
 * modo que un fallo al cargar los contactos no impida editar el perfil.
 */
export default function PerfilPrestador() {
  const perfil = usePerfilPrestador();

  if (perfil.isPending) {
    return (
      <main className={propios.pantalla}>
        <p className={secciones.estado} role="status">
          Cargando tu perfil…
        </p>
      </main>
    );
  }

  if (perfil.isError) {
    return (
      <main className={propios.pantalla}>
        <div className={propios.contenido}>
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {perfil.error instanceof ErrorDeApi
              ? perfil.error.message
              : 'No pudimos cargar tu perfil.'}{' '}
            <button
              className={estilos.enlaceDeTexto}
              type="button"
              onClick={() => void perfil.refetch()}
            >
              Reintentar
            </button>
          </p>
          <p className={propios.pie}>
            <Link to="/">Volver al inicio</Link>
          </p>
        </div>
      </main>
    );
  }

  const datos = perfil.data;

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>Tu perfil de prestador</h1>
          <p className={secciones.explicacion}>
            {datos === null
              ? 'Crea tu perfil para empezar a preparar tu presentación y tu portafolio.'
              : 'Desde aquí administras tu presentación, tus contactos y tu portafolio.'}
          </p>
        </header>

        {datos !== null && datos.nivelVerificacion === 'SIN_VERIFICAR' && (
          <p className={propios.avisoDePrivacidad} role="status">
            <strong>Tu perfil todavía es privado.</strong> Nadie más puede verlo mientras esté sin
            verificar. Puedes prepararlo completo desde ahora: la verificación documental llega en
            una próxima entrega.
          </p>
        )}

        <FormularioDePerfil perfil={datos} />

        {datos !== null && (
          <>
            <ImagenDePerfil perfil={datos} />
            <Disponibilidad perfil={datos} />
            <Contactos />
            <Portafolio />
          </>
        )}

        <p className={propios.pie}>
          <Link to="/">Volver al inicio</Link>
        </p>
      </div>
    </main>
  );
}
