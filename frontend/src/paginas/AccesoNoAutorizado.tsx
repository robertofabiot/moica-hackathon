import type { ReactNode } from 'react';
import { Link } from 'react-router';

import logoHorizontal from '../assets/logos/moica-horizontal.png';
import { RUTA_INICIO_SESION, RUTA_SEGURIDAD } from '../capacidades/auth';
import { RUTA_EXPLORAR } from '../capacidades/busqueda';
import { Boton, IconoCasa, IconoEscudo, IconoReloj, IconoUsuario } from '../comun/componentes/ui';
import estilos from './AccesoNoAutorizado.module.css';
import { IlustracionDeAccesoDenegado } from './IlustracionDeAccesoDenegado';

export type TipoDeAccesoDenegado =
  'permisos-insuficientes' | 'sesion-expirada' | 'requiere-segundo-factor';

export type PropiedadesDeAccesoNoAutorizado = {
  tipo?: TipoDeAccesoDenegado;
  codigo?: 401 | 403;
  mensajePersonalizado?: string;
  destinoRetorno?: string;
};

type ContenidoDeAcceso = {
  titulo: string;
  explicacion: string;
  insignia: string;
  accionPrincipal: {
    texto: string;
    destino: string;
    icono: ReactNode;
  };
  accionSecundaria: {
    texto: string;
    destino: string;
  };
};

const CONTENIDO: Record<TipoDeAccesoDenegado, Omit<ContenidoDeAcceso, 'accionPrincipal'>> = {
  'permisos-insuficientes': {
    titulo: 'Esta zona requiere otros permisos',
    explicacion:
      'Tu cuenta no dispone de los privilegios necesarios para acceder a esta sección de Moica. Si crees que deberías tener acceso, ponte en contacto con el administrador o con soporte.',
    insignia: 'HTTP 403 · ACCESO RESTRINGIDO',
    accionSecundaria: { texto: 'Volver a explorar', destino: RUTA_EXPLORAR },
  },
  'sesion-expirada': {
    titulo: 'Tu sesión no está activa',
    explicacion:
      'Para proteger la información de tu cuenta, necesitamos verificar quién eres antes de continuar. Vuelve a iniciar sesión para retomar tu actividad.',
    insignia: 'HTTP 401 · SESIÓN REQUERIDA',
    accionSecundaria: { texto: 'Volver a explorar', destino: RUTA_EXPLORAR },
  },
  'requiere-segundo-factor': {
    titulo: 'Verificación adicional requerida',
    explicacion:
      'Esta área exige tener el segundo factor de autenticación verificado en esta sesión activa. Configúralo en la sección de seguridad para continuar.',
    insignia: 'HTTP 403 · ACCESO RESTRINGIDO',
    accionSecundaria: { texto: 'Volver a explorar', destino: RUTA_EXPLORAR },
  },
};

/**
 * Pantalla independiente cuando la navegación choca con un 401 o un 403.
 *
 * El lockup horizontal (PNG de marca, el mismo que el resto de pantallas) va
 * arriba. No sustituye al control del backend: explica con calma por qué esa
 * zona no está disponible y ofrece el siguiente paso concreto.
 */
export default function AccesoNoAutorizado({
  tipo,
  codigo,
  mensajePersonalizado,
  destinoRetorno,
}: PropiedadesDeAccesoNoAutorizado) {
  const estado = resolverEstado(tipo, codigo);
  const base = CONTENIDO[estado.tipo];
  const accionPrincipal = accionPrincipalDe(estado.tipo, destinoRetorno);
  const explicacion = mensajePersonalizado ?? base.explicacion;
  const iconoDeInsignia = estado.codigo === 401 ? <IconoReloj /> : <IconoEscudo />;

  return (
    <main className={estilos.contenedor}>
      <Link className={estilos.marca} to="/" aria-label="Moica, ir al inicio">
        <img className={estilos.logoHorizontal} src={logoHorizontal} alt="" />
      </Link>

      <div className={estilos.cuerpo}>
        <div className={estilos.escena}>
          <IlustracionDeAccesoDenegado />
          <p className={estilos.insignia}>
            {iconoDeInsignia}
            {base.insignia}
          </p>
        </div>

        <h1 className={estilos.titulo}>{base.titulo}</h1>
        <p className={estilos.explicacion} role="alert">
          {explicacion}
        </p>

        <div className={estilos.acciones}>
          <Boton
            className={estilos.accion}
            forma="pildora"
            variante="primario"
            to={accionPrincipal.destino}
          >
            {accionPrincipal.icono}
            {accionPrincipal.texto}
          </Boton>
          <Boton
            className={estilos.accion}
            forma="pildora"
            variante="secundario"
            to={base.accionSecundaria.destino}
          >
            {base.accionSecundaria.texto}
          </Boton>
        </div>
      </div>
    </main>
  );
}

function resolverEstado(
  tipo: TipoDeAccesoDenegado | undefined,
  codigo: 401 | 403 | undefined
): { tipo: TipoDeAccesoDenegado; codigo: 401 | 403 } {
  if (tipo === 'sesion-expirada') {
    return { tipo, codigo: 401 };
  }
  if (tipo === 'requiere-segundo-factor' || tipo === 'permisos-insuficientes') {
    return { tipo, codigo: 403 };
  }
  if (codigo === 401) {
    return { tipo: 'sesion-expirada', codigo: 401 };
  }
  return { tipo: 'permisos-insuficientes', codigo: 403 };
}

function accionPrincipalDe(
  tipo: TipoDeAccesoDenegado,
  destinoRetorno: string | undefined
): ContenidoDeAcceso['accionPrincipal'] {
  if (tipo === 'sesion-expirada') {
    return { texto: 'Iniciar sesión', destino: RUTA_INICIO_SESION, icono: <IconoUsuario /> };
  }
  if (tipo === 'requiere-segundo-factor') {
    return { texto: 'Configurar seguridad', destino: RUTA_SEGURIDAD, icono: <IconoEscudo /> };
  }
  return {
    texto: 'Volver al inicio',
    destino: destinoRetorno ?? '/',
    icono: <IconoCasa />,
  };
}
