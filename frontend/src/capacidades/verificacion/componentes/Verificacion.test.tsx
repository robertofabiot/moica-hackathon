import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  definirTiempoDeEsperaDeArchivoMs,
  TIEMPO_DE_ESPERA_DE_ARCHIVO_MS,
} from '../../../comun/api';
import {
  cuerpoDeError,
  documentoDeVerificacionDeEjemplo,
  estadoDeVerificacionDeEjemplo,
  instalarApiFalsa,
  sesionDeEjemplo,
  solicitudDeVerificacionDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import { useCierreSesion, useSesionActual } from '../../auth';
import { CLAVE_DE_SOLICITUDES, CLAVE_DE_VERIFICACION } from '../hooks/useVerificacion';
import Verificacion from './Verificacion';

/**
 * La sección junto al botón de salir, como convive en la aplicación real.
 *
 * La sección desaparece cuando la sesión termina, igual que hace `RutaProtegida` al llevar a
 * iniciar sesión. Sin eso, la consulta seguiría observada y React Query volvería a pedirla en
 * cuanto se descarta, que es justo lo contrario de lo que se quiere comprobar.
 */
function PerfilConSalida() {
  const { solicitarCierre } = useCierreSesion();
  const sesion = useSesionActual();

  return (
    <>
      {sesion.data !== null && <Verificacion />}
      <button type="button" onClick={solicitarCierre}>
        Cerrar sesión
      </button>
    </>
  );
}

/**
 * La verificación vista desde el prestador: qué ofrece, qué exige y qué explica.
 *
 * Lo que se comprueba no es la maqueta sino las reglas visibles: la profesional no se ofrece sin la
 * básica vigente, un archivo inadmisible no llega a la lista, quitarlo lo retira, el envío va en una
 * sola petición multipart con un tipo por archivo, y el motivo de un rechazo se lee junto a la
 * posibilidad de volver a intentarlo.
 */

const RUTA_ESTADO = 'GET /api/prestador/verificacion';
const RUTA_SOLICITUDES = 'GET /api/prestador/verificacion/solicitudes';
const RUTA_ENVIO = 'POST /api/prestador/verificacion/solicitudes';

/** Un archivo de prueba con el tipo y el tamaño que declara el navegador. */
function archivo(nombre: string, tipo: string, bytes = 1024): File {
  const contenido = new File(['x'.repeat(10)], nombre, { type: tipo });
  Object.defineProperty(contenido, 'size', { value: bytes });
  return contenido;
}

describe('verificación del prestador', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder(RUTA_SOLICITUDES, { estado: 200, cuerpo: [] });
    // Las cargas de archivo usan su propia espera de 90 s; en las pruebas se
    // acorta para no depender de un temporizador real.
    definirTiempoDeEsperaDeArchivoMs(200);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    definirTiempoDeEsperaDeArchivoMs(TIEMPO_DE_ESPERA_DE_ARCHIVO_MS);
  });

  function conEstado(cuerpo: unknown) {
    api.responder(RUTA_ESTADO, { estado: 200, cuerpo });
  }

  it('explica el nivel vigente y qué significa una insignia', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    renderizarConProveedores(<Verificacion />);

    expect(await screen.findByText('Sin verificar')).toBeInTheDocument();
    expect(screen.getByText(/no superó la verificación documental/i)).toBeInTheDocument();
    expect(screen.getByText(/No garantiza la calidad futura de tu trabajo/i)).toBeInTheDocument();
  });

  it('ofrece la básica cuando el perfil está sin verificar y la profesional no', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    renderizarConProveedores(<Verificacion />);

    expect(
      await screen.findByRole('button', { name: 'Solicitar verificación básica' })
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Solicitar verificación profesional' })
    ).not.toBeInTheDocument();
    expect(screen.getByText(/primero necesitas la básica vigente/i)).toBeInTheDocument();
  });

  it('ofrece la profesional solo con la básica vigente', async () => {
    conEstado(
      estadoDeVerificacionDeEjemplo({
        nivelVerificacion: 'VERIFICADO_BASICO',
        significado: 'Una persona administradora revisó y aprobó tu documentación de identidad.',
        puedeSolicitarBasica: false,
        puedeSolicitarProfesional: true,
      })
    );
    renderizarConProveedores(<Verificacion />);

    expect(await screen.findByText('Verificado Básico')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Solicitar verificación profesional' })
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Solicitar verificación básica' })
    ).not.toBeInTheDocument();
  });

  it('avisa de que los documentos no serán públicos antes de elegir ninguno', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    renderizarConProveedores(<Verificacion />);

    await userEvent.click(
      await screen.findByRole('button', { name: 'Solicitar verificación básica' })
    );

    expect(screen.getByText(/Tus documentos no serán públicos/i)).toBeInTheDocument();
    expect(screen.getByText(/Todavía no has elegido ningún documento/i)).toBeInTheDocument();
  });

  it('acepta varios documentos y permite retirar uno antes de enviar', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    renderizarConProveedores(<Verificacion />);

    await userEvent.click(
      await screen.findByRole('button', { name: 'Solicitar verificación básica' })
    );
    await userEvent.upload(screen.getByLabelText('Elige tus documentos'), [
      archivo('cedula.png', 'image/png'),
      archivo('constancia.pdf', 'application/pdf'),
    ]);

    expect(screen.getByText('cedula.png')).toBeInTheDocument();
    expect(screen.getByText('constancia.pdf')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Quitar constancia.pdf' }));

    expect(screen.queryByText('constancia.pdf')).not.toBeInTheDocument();
    expect(screen.getByText('cedula.png')).toBeInTheDocument();
  });

  it('rechaza en el navegador un archivo mayor que el máximo', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    renderizarConProveedores(<Verificacion />);

    await userEvent.click(
      await screen.findByRole('button', { name: 'Solicitar verificación básica' })
    );
    await userEvent.upload(
      screen.getByLabelText('Elige tus documentos'),
      archivo('enorme.png', 'image/png', 6 * 1024 * 1024)
    );

    expect(await screen.findByText(/enorme.png: El archivo supera el máximo/i)).toBeInTheDocument();
    expect(screen.getByText(/Todavía no has elegido ningún documento/i)).toBeInTheDocument();
  });

  it('rechaza un formato que el expediente no admite aunque el selector lo deje pasar', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    renderizarConProveedores(<Verificacion />);

    await userEvent.click(
      await screen.findByRole('button', { name: 'Solicitar verificación básica' })
    );

    // `userEvent.upload` respeta el atributo `accept` y descartaría el archivo
    // antes de llegar al componente. El filtro del selector es una comodidad,
    // no un control: se dispara el evento a mano para comprobar la regla que sí
    // lo es. La definitiva, de todos modos, es la del backend.
    fireEvent.change(screen.getByLabelText('Elige tus documentos'), {
      target: { files: [archivo('hoja.docx', 'application/msword')] },
    });

    expect(
      await screen.findByText(/hoja.docx: Solo se admiten archivos JPEG/i)
    ).toBeInTheDocument();
    expect(screen.getByText(/Todavía no has elegido ningún documento/i)).toBeInTheDocument();
  });

  it('no deja enviar una básica sin documento de identidad', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    renderizarConProveedores(<Verificacion />);

    await userEvent.click(
      await screen.findByRole('button', { name: 'Solicitar verificación básica' })
    );
    await userEvent.upload(
      screen.getByLabelText('Elige tus documentos'),
      archivo('constancia.pdf', 'application/pdf')
    );
    await userEvent.selectOptions(
      screen.getByLabelText('Qué es este documento'),
      'Constancia de experiencia'
    );

    expect(
      screen.getByText(/necesita al menos un documento oficial de identidad/i)
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Revisar y enviar' })).toBeDisabled();
  });

  it('pide confirmación y envía todo el expediente en una sola petición', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    api.responder(RUTA_ENVIO, { estado: 201, cuerpo: solicitudDeVerificacionDeEjemplo() });
    renderizarConProveedores(<Verificacion />);

    await userEvent.click(
      await screen.findByRole('button', { name: 'Solicitar verificación básica' })
    );
    await userEvent.upload(screen.getByLabelText('Elige tus documentos'), [
      archivo('cedula.png', 'image/png'),
      archivo('constancia.pdf', 'application/pdf'),
    ]);

    await userEvent.click(screen.getByRole('button', { name: 'Revisar y enviar' }));
    expect(screen.getByText(/no podrás editarlo ni sustituirlo/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Confirmar y enviar' }));

    await waitFor(() => expect(api.ultima(RUTA_ENVIO)).toBeDefined());
    const formulario = api.ultima(RUTA_ENVIO)?.formulario;
    expect(formulario?.get('nivelSolicitado')).toBe('BASICA');
    expect(formulario?.getAll('archivo')).toHaveLength(2);
    expect(formulario?.getAll('tipoDocumento')).toEqual(['IDENTIDAD', 'IDENTIDAD']);
    expect(api.ultima(RUTA_ENVIO)?.cabeceras['X-XSRF-TOKEN']).toBe('token-de-prueba');
  });

  it('muestra el mensaje del backend cuando ya hay una solicitud abierta', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    api.responder(RUTA_ENVIO, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'SOLICITUD_ABIERTA_DUPLICADA',
        'Ya tienes una solicitud de ese nivel esperando revisión.'
      ),
    });
    renderizarConProveedores(<Verificacion />);

    await userEvent.click(
      await screen.findByRole('button', { name: 'Solicitar verificación básica' })
    );
    await userEvent.upload(
      screen.getByLabelText('Elige tus documentos'),
      archivo('cedula.png', 'image/png')
    );
    await userEvent.click(screen.getByRole('button', { name: 'Revisar y enviar' }));
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar y enviar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya tienes una solicitud de ese nivel esperando revisión.'
    );
  });

  it('sin conexión lo dice y no deja el envío cargando para siempre', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    api.rechazar(RUTA_ENVIO);
    renderizarConProveedores(<Verificacion />);

    await userEvent.click(
      await screen.findByRole('button', { name: 'Solicitar verificación básica' })
    );
    await userEvent.upload(
      screen.getByLabelText('Elige tus documentos'),
      archivo('cedula.png', 'image/png')
    );
    await userEvent.click(screen.getByRole('button', { name: 'Revisar y enviar' }));
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar y enviar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/Revisa tu conexión/i);
    expect(screen.getByRole('button', { name: 'Confirmar y enviar' })).toBeEnabled();
  });

  it('muestra la solicitud abierta y deja de ofrecer otra del mismo nivel', async () => {
    conEstado(
      estadoDeVerificacionDeEjemplo({
        puedeSolicitarBasica: false,
        solicitudAbierta: solicitudDeVerificacionDeEjemplo({ estadoSolicitud: 'EN_REVISION' }),
      })
    );
    renderizarConProveedores(<Verificacion />);

    expect(await screen.findByText('En revisión')).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Solicitar verificación básica' })
    ).not.toBeInTheDocument();
  });

  it('lee el motivo del rechazo en el historial y vuelve a ofrecer el envío', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    api.responder(RUTA_SOLICITUDES, {
      estado: 200,
      cuerpo: [
        solicitudDeVerificacionDeEjemplo({
          estadoSolicitud: 'RECHAZADA',
          observacionResolucion: 'El documento está ilegible; envía una foto más nítida.',
          fechaResolucion: '2026-08-27T09:00:00-06:00',
        }),
      ],
    });
    renderizarConProveedores(<Verificacion />);

    expect(
      await screen.findByText(/El documento está ilegible; envía una foto más nítida./)
    ).toBeInTheDocument();
    expect(screen.getByText('Rechazada')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Solicitar verificación básica' })
    ).toBeInTheDocument();
  });

  it('el historial muestra los metadatos de cada documento y ninguna forma de abrirlo', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    api.responder(RUTA_SOLICITUDES, {
      estado: 200,
      cuerpo: [
        solicitudDeVerificacionDeEjemplo({
          documentos: [
            documentoDeVerificacionDeEjemplo(1, 'IDENTIDAD', 'cedula.png'),
            documentoDeVerificacionDeEjemplo(2, 'CERTIFICACION', 'titulo.pdf'),
          ],
        }),
      ],
    });
    renderizarConProveedores(<Verificacion />);

    expect(await screen.findByText('cedula.png')).toBeInTheDocument();
    expect(screen.getByText('titulo.pdf')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Abrir/i })).not.toBeInTheDocument();
  });

  it('al cerrar sesión no queda en memoria nada del expediente', async () => {
    conEstado(estadoDeVerificacionDeEjemplo());
    api.responder(RUTA_SOLICITUDES, {
      estado: 200,
      cuerpo: [solicitudDeVerificacionDeEjemplo()],
    });
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    api.responder('DELETE /api/auth/sesion', { estado: 204 });
    const { cliente } = renderizarConProveedores(<PerfilConSalida />);

    await screen.findByText('Sin verificar');
    await waitFor(() => expect(cliente.getQueryData(CLAVE_DE_VERIFICACION)).toBeDefined());

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    await waitFor(() => expect(cliente.getQueryData(CLAVE_DE_VERIFICACION)).toBeUndefined());
    expect(cliente.getQueryData(CLAVE_DE_SOLICITUDES)).toBeUndefined();
  });

  it('si el estado no carga lo dice y permite reintentar', async () => {
    api.responder(RUTA_ESTADO, {
      estado: 500,
      cuerpo: cuerpoDeError(500, 'ERROR_INTERNO', 'No pudimos completar la operación.'),
    });
    renderizarConProveedores(<Verificacion />);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos completar la operación.'
    );
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
