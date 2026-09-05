import { type ReactNode } from 'react';
import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import { BarraLateral } from '../../../comun/componentes/ui';
import { Portafolio } from '../../portafolio';
import { Verificacion } from '../../verificacion';
import Contactos from '../componentes/Contactos';
import Disponibilidad from '../componentes/Disponibilidad';
import FormularioDePerfil from '../componentes/FormularioDePerfil';
import ImagenDePerfil from '../componentes/ImagenDePerfil';
import { usePerfilPrestador } from '../hooks/usePerfilPrestador';
import { RUTA_PRESTADOR } from '../rutas';
import propios from './prestador.module.css';

const DESTINOS_DE_BARRA = {
  inicio: '/panel',
  mensajes: '/mensajes',
  perfil: RUTA_PRESTADOR,
  configuracion: '/seguridad',
};

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
      <MarcoDePagina>
        <p className={propios.estado} role="status">
          Cargando tu perfil…
        </p>
      </MarcoDePagina>
    );
  }

  if (perfil.isError) {
    return (
      <MarcoDePagina>
        <p className={`${propios.aviso} ${propios.avisoDeError}`} role="alert">
          {perfil.error instanceof ErrorDeApi
            ? perfil.error.message
            : 'No pudimos cargar tu perfil.'}{' '}
          <button
            className={propios.enlaceDeTexto}
            type="button"
            onClick={() => void perfil.refetch()}
          >
            Reintentar
          </button>
        </p>
        <p className={propios.pie}>
          <Link to="/">Volver al inicio</Link>
        </p>
      </MarcoDePagina>
    );
  }

  const datos = perfil.data;

  return (
    <MarcoDePagina>
      <header className={propios.encabezado}>
        <h1 className={propios.titulo}>Tu perfil de prestador</h1>
        <p className={propios.subtitulo}>
          {datos === null
            ? 'Crea tu perfil para empezar a preparar tu presentación y tu portafolio.'
            : 'Desde aquí administras tu presentación, tus contactos y tu portafolio.'}
        </p>
      </header>

      {datos !== null && datos.nivelVerificacion === 'SIN_VERIFICAR' && (
        <p className={propios.avisoDePrivacidad} role="status">
          <strong>Tu perfil todavía es privado.</strong> Nadie más puede verlo mientras esté sin
          verificar. Puedes prepararlo completo desde ahora y presentar tu expediente cuando
          quieras, en la sección de verificación.
        </p>
      )}

      <FormularioDePerfil perfil={datos} />

      {datos !== null && (
        <>
          <section className={propios.tarjetaIdentidad} aria-label="Identidad y disponibilidad">
            <ImagenDePerfil perfil={datos} />
            <hr className={propios.divisorIdentidad} aria-hidden="true" />
            <Disponibilidad perfil={datos} />
          </section>
          <Verificacion />
          <Contactos />
          <Portafolio />
        </>
      )}

      <p className={propios.pie}>
        <Link to="/">Volver al inicio</Link>
      </p>
    </MarcoDePagina>
  );
}

function MarcoDePagina({ children }: { children: ReactNode }) {
  return (
    <div className={propios.pagina}>
      <div className={propios.barraLateral}>
        <BarraLateral itemActivo="perfil" destinos={DESTINOS_DE_BARRA} />
      </div>
      <main className={propios.principal}>
        <div className={propios.contenido}>{children}</div>
      </main>
    </div>
  );
}
