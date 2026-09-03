import { BarraLateral } from '../../../comun/componentes/ui';
import { RUTA_PRESTADOR } from '../../prestador';
import CambioDeClave from '../componentes/CambioDeClave';
import PestaniasDeConfiguracion from '../componentes/PestaniasDeConfiguracion';
import SegundoFactorDeLaCuenta from '../componentes/SegundoFactorDeLaCuenta';
import { RUTA_SEGURIDAD } from '../rutas';
import estilos from './seguridad.module.css';

const DESTINOS_DE_BARRA = {
  inicio: '/',
  mensajes: '/mensajes',
  perfil: RUTA_PRESTADOR,
  configuracion: RUTA_SEGURIDAD,
};

/**
 * Seguridad de la cuenta: contraseña y segundo factor.
 *
 * Solo pinta y ordena. Cada sección se encarga de sus datos y de sus estados de carga y error, que
 * es lo que permite que un fallo al consultar el segundo factor no impida cambiar la contraseña.
 */
export default function SeguridadCuenta() {
  return (
    <div className={estilos.pagina}>
      <div className={estilos.barraLateral}>
        <BarraLateral itemActivo="configuracion" destinos={DESTINOS_DE_BARRA} />
      </div>
      <main className={estilos.principal}>
        <div className={estilos.columna}>
          <div className={estilos.tarjetaPrincipal}>
            <h1 className={estilos.titulo}>Configuración</h1>
            <PestaniasDeConfiguracion />

            <h2 className={estilos.tituloDeGrupo}>Seguridad de tu cuenta</h2>

            <div className={estilos.filas}>
              <div className={estilos.filaDeConfiguracion}>
                <div className={estilos.datosDeFila}>
                  <p className={estilos.etiquetaDeFila}>Correo electrónico</p>
                  <p className={estilos.valorDeFila}>usuario@ejemplo.com</p>
                </div>
                <span className={estilos.accionInactiva}>Próximamente</span>
              </div>

              <hr className={estilos.divisor} />

              <CambioDeClave />

              <hr className={estilos.divisor} />

              <div className={estilos.filaDeConfiguracion}>
                <div className={estilos.datosDeFila}>
                  <p className={estilos.etiquetaDeFila}>Número de teléfono</p>
                  <p className={estilos.valorDeFila}>+505 0000 0000</p>
                </div>
                <span className={estilos.accionInactiva}>Próximamente</span>
              </div>

              <hr className={estilos.divisor} />

              <div className={estilos.bloqueDeSelect}>
                <label className={estilos.etiquetaDeFila} htmlFor="idioma">
                  Idioma
                </label>
                <select
                  id="idioma"
                  className={estilos.selectPresentacional}
                  defaultValue="es"
                  disabled
                >
                  <option value="es">Español</option>
                </select>
              </div>

              <hr className={estilos.divisor} />

              <div className={estilos.bloqueDeSelect}>
                <label className={estilos.etiquetaDeFila} htmlFor="zonaHoraria">
                  Zona horaria
                </label>
                <select
                  id="zonaHoraria"
                  className={estilos.selectPresentacional}
                  defaultValue="america-managua"
                  disabled
                >
                  <option value="america-managua">(UTC-6) Centroamérica</option>
                </select>
              </div>
            </div>
          </div>

          <SegundoFactorDeLaCuenta />
        </div>
      </main>
    </div>
  );
}
