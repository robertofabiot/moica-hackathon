import { useEffect, useMemo, useRef, useState, type SVGProps } from 'react';
import { Link, useSearchParams } from 'react-router';

import logoHorizontal from '../../../assets/logos/moica-horizontal.png';
import logoIcono from '../../../assets/logos/moica-icono.svg';
import { RUTA_ADMIN } from '../../admin';
import { RUTA_INICIO_SESION, RUTA_SEGURIDAD, useCierreSesion, useSesionActual } from '../../auth';
import { RUTA_PRESTADOR } from '../../prestador';
import { RUTA_SERVICIOS } from '../../servicio';
import { RUTA_SOLICITUDES } from '../../solicitud';
import { ErrorDeApi } from '../../../comun/api';
import { Boton, Entrada, IconoLupa, IconoPin, PieDePagina } from '../../../comun/componentes/ui';
import FiltrosPublicos from '../componentes/FiltrosPublicos';
import TarjetaDeServicio from '../componentes/TarjetaDeServicio';
import lista from '../componentes/tarjeta.module.css';
import { useDepartamentosPublicos, useServiciosPublicos } from '../hooks/useBusquedaPublica';
import type { FiltrosDeBusqueda } from '../tipos';
import propios from './explorar.module.css';

const FILTROS_VACIOS: FiltrosDeBusqueda = {
  texto: '',
  idCategoria: '',
  idSubcategoria: '',
  idMunicipio: '',
  precioMaximo: '',
};

function filtrosDesdeParametros(params: URLSearchParams): FiltrosDeBusqueda {
  return {
    texto: params.get('texto') ?? '',
    idCategoria: params.get('idCategoria') ?? '',
    idSubcategoria: params.get('idSubcategoria') ?? '',
    idMunicipio: params.get('idMunicipio') ?? '',
    precioMaximo: params.get('precioMaximo') ?? '',
  };
}

function parametrosDesdeFiltros(filtros: FiltrosDeBusqueda): Record<string, string> {
  const parametros: Record<string, string> = {};
  const texto = filtros.texto.trim();
  if (texto !== '') {
    parametros.texto = texto;
  }
  if (filtros.idCategoria !== '') {
    parametros.idCategoria = filtros.idCategoria;
  }
  if (filtros.idSubcategoria !== '') {
    parametros.idSubcategoria = filtros.idSubcategoria;
  }
  if (filtros.idMunicipio !== '') {
    parametros.idMunicipio = filtros.idMunicipio;
  }
  if (filtros.precioMaximo && filtros.precioMaximo !== '' && filtros.precioMaximo !== 'cualquiera') {
    parametros.precioMaximo = filtros.precioMaximo;
  }
  return parametros;
}

/**
 * Exploración pública de servicios. No exige sesión.
 *
 * Solo el backend decide qué aparece: servicios activos de cuentas operativas, prestadores
 * disponibles y perfiles con al menos verificación básica. Los filtros iniciales salen de la
 * URL para que el hero de la portada pueda abrir esta pantalla con un texto.
 */
export default function ExplorarServicios() {
  const [parametros, setParametros] = useSearchParams();
  const aplicados = filtrosDesdeParametros(parametros);
  const [borrador, setBorrador] = useState<FiltrosDeBusqueda>(aplicados);
  const resultados = useServiciosPublicos(aplicados);

  useEffect(() => {
    setBorrador(filtrosDesdeParametros(parametros));
  }, [parametros]);

  const serviciosFiltrados = useMemo(() => {
    const todos = resultados.data ?? [];
    if (
      !aplicados.precioMaximo ||
      aplicados.precioMaximo === '' ||
      aplicados.precioMaximo === 'cualquiera'
    ) {
      return todos;
    }
    const tope = Number(aplicados.precioMaximo);
    if (Number.isNaN(tope)) {
      return todos;
    }
    return todos.filter(
      (servicio) => servicio.precioReferencia !== null && Number(servicio.precioReferencia) <= tope
    );
  }, [resultados.data, aplicados.precioMaximo]);

  function aplicar(siguientes?: FiltrosDeBusqueda) {
    const destino = siguientes ?? borrador;
    setBorrador(destino);
    setParametros(parametrosDesdeFiltros(destino), { replace: true });
  }

  return (
    <div className={propios.paginaExplorar}>
      <BarraDeExploracion
        filtros={borrador}
        onCambiar={setBorrador}
        onAplicar={(siguientes) => aplicar(siguientes)}
      />

      <div className={propios.cuerpoExplorar}>
        <FiltrosPublicos
          filtros={borrador}
          onCambiar={setBorrador}
          onAplicar={(siguientes) => aplicar(siguientes)}
          onLimpiar={() => {
            setBorrador(FILTROS_VACIOS);
            setParametros({}, { replace: true });
          }}
        />

        <main className={propios.principalExplorar}>
          <header className={propios.encabezadoDeResultados}>
            <h1 className={propios.tituloDeResultados}>Explorar servicios</h1>
          </header>

          {resultados.isPending && (
            <p className={propios.estado} role="status">
              Buscando servicios…
            </p>
          )}

          {resultados.isError && (
            <p className={`${propios.aviso} ${propios.avisoDeError}`} role="alert">
              {resultados.error instanceof ErrorDeApi
                ? resultados.error.message
                : 'No pudimos cargar los servicios.'}{' '}
              <button
                className={propios.reintentar}
                type="button"
                onClick={() => void resultados.refetch()}
              >
                Reintentar
              </button>
            </p>
          )}

          {resultados.data !== undefined && serviciosFiltrados.length === 0 && (
            <p className={propios.vacio}>
              No hay servicios que coincidan con esos filtros. Prueba otra combinación.
            </p>
          )}

          {serviciosFiltrados.length > 0 && (
            <ul className={lista.lista}>
              {serviciosFiltrados.map((servicio) => (
                <TarjetaDeServicio key={servicio.idServicioPublicado} servicio={servicio} />
              ))}
            </ul>
          )}
        </main>
      </div>

      <PieDePagina />
    </div>
  );
}

function BarraDeExploracion({
  filtros,
  onCambiar,
  onAplicar,
}: {
  filtros: FiltrosDeBusqueda;
  onCambiar: (filtros: FiltrosDeBusqueda) => void;
  onAplicar: (siguientes?: FiltrosDeBusqueda) => void;
}) {
  const sesion = useSesionActual();
  const departamentos = useDepartamentosPublicos();
  const usuario = sesion.data?.usuario;
  const inicial = usuario === undefined ? null : inicialDe(usuario.nombreCompleto);

  return (
    <header className={propios.barraSuperior}>
      <Link className={propios.marca} to="/" aria-label="Moica, ir al inicio">
        <img className={propios.logoIcono} src={logoIcono} alt="" />
        <img className={propios.logoCompleto} src={logoHorizontal} alt="" />
      </Link>

      <form
        className={propios.pildoraDeBusqueda}
        role="search"
        onSubmit={(evento) => {
          evento.preventDefault();
          onAplicar(filtros);
        }}
      >
        <div className={propios.campoDeBusqueda}>
          <Entrada
            variante="fusionada"
            type="search"
            autoComplete="off"
            aria-label="Buscar servicios"
            placeholder="Buscar servicios..."
            value={filtros.texto}
            onChange={(evento) => onCambiar({ ...filtros, texto: evento.target.value })}
            icono={<IconoLupa />}
          />
        </div>
        <span className={propios.divisorDePildora} aria-hidden="true" />
        <div className={propios.campoDeCiudad}>
          <span className={propios.iconoDeCiudad} aria-hidden="true">
            <IconoPin />
          </span>
          <select
            className={propios.selectDeCiudad}
            aria-label="Ciudad"
            value={filtros.idMunicipio}
            onChange={(evento) => {
              const siguientes = { ...filtros, idMunicipio: evento.target.value };
              onCambiar(siguientes);
              onAplicar(siguientes);
            }}
          >
            <option value="">Managua, NIC</option>
            {(departamentos.data ?? []).flatMap((departamento) =>
              departamento.municipios.map((municipio) => (
                <option key={municipio.idMunicipio} value={municipio.idMunicipio}>
                  {municipio.nombre}, NIC
                </option>
              ))
            )}
          </select>
        </div>
        <Boton type="submit" forma="pildora" className={propios.botonBuscar} aria-label="Buscar">
          <span className={propios.textoBuscar}>Buscar</span>
          <span className={propios.iconoBuscar} aria-hidden="true">
            <IconoLupa />
          </span>
        </Boton>
      </form>

      <div className={propios.accionesDeBarra}>
        <button
          type="button"
          className={propios.botonIcono}
          aria-label="Notificaciones, hay avisos"
        >
          <IconoCampana />
          <span className={propios.puntoDeAviso} aria-hidden="true" />
        </button>
        {usuario === undefined || inicial === null ? (
          <Link className={propios.avatar} to={RUTA_INICIO_SESION} aria-label="Iniciar sesión">
            <IconoUsuario />
          </Link>
        ) : (
          <MenuUsuarioAvatar usuario={usuario} inicial={inicial} />
        )}
      </div>
    </header>
  );
}

function MenuUsuarioAvatar({
  usuario,
  inicial,
}: {
  usuario: { nombreCompleto: string; esAdministrador?: boolean };
  inicial: string;
}) {
  const [abierto, setAbierto] = useState(false);
  const cierre = useCierreSesion();
  const contenedorRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function manejarClicFuera(evento: MouseEvent) {
      if (contenedorRef.current && !contenedorRef.current.contains(evento.target as Node)) {
        setAbierto(false);
      }
    }
    if (abierto) {
      document.addEventListener('mousedown', manejarClicFuera);
    }
    return () => {
      document.removeEventListener('mousedown', manejarClicFuera);
    };
  }, [abierto]);

  return (
    <div ref={contenedorRef} className={propios.menuUsuarioContenedor}>
      <button
        type="button"
        className={propios.avatarBoton}
        aria-expanded={abierto}
        aria-haspopup="true"
        aria-label={`Cuenta de ${usuario.nombreCompleto}`}
        onClick={() => setAbierto((prev) => !prev)}
      >
        {inicial}
      </button>

      {abierto && (
        <ul className={propios.panelDeSesion} role="menu">
          <li>
            <Link
              className={propios.opcionDeSesion}
              to={RUTA_PRESTADOR}
              onClick={() => setAbierto(false)}
            >
              Mi perfil de prestador
            </Link>
          </li>
          <li>
            <Link
              className={propios.opcionDeSesion}
              to={RUTA_SERVICIOS}
              onClick={() => setAbierto(false)}
            >
              Mis servicios
            </Link>
          </li>
          <li>
            <Link
              className={propios.opcionDeSesion}
              to={RUTA_SOLICITUDES}
              onClick={() => setAbierto(false)}
            >
              Mis solicitudes
            </Link>
          </li>
          <li>
            <Link
              className={propios.opcionDeSesion}
              to={RUTA_SEGURIDAD}
              onClick={() => setAbierto(false)}
            >
              Seguridad de la cuenta
            </Link>
          </li>
          {usuario.esAdministrador && (
            <li>
              <Link
                className={propios.opcionDeSesion}
                to={RUTA_ADMIN}
                onClick={() => setAbierto(false)}
              >
                Área administrativa
              </Link>
            </li>
          )}
          <li>
            <button
              type="button"
              className={propios.opcionDeSesion}
              onClick={() => {
                setAbierto(false);
                cierre.solicitarCierre();
              }}
              disabled={cierre.isPending}
            >
              {cierre.isPending ? 'Cerrando sesión…' : 'Cerrar sesión'}
            </button>
          </li>
        </ul>
      )}
    </div>
  );
}

function inicialDe(nombreCompleto: string): string {
  const primero = nombreCompleto.trim().split(/\s+/)[0] ?? '';
  return primero.slice(0, 1).toUpperCase();
}

type PropiedadesDeIcono = SVGProps<SVGSVGElement>;

function Trazo({ children, ...rest }: PropiedadesDeIcono) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...rest}
    >
      {children}
    </svg>
  );
}

function IconoCampana() {
  return (
    <Trazo>
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
      <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
    </Trazo>
  );
}

function IconoUsuario() {
  return (
    <Trazo>
      <circle cx="12" cy="8" r="5" />
      <path d="M20 21a8 8 0 0 0-16 0" />
    </Trazo>
  );
}
