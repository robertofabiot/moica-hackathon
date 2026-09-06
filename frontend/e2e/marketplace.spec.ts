import { expect, test } from '@playwright/test';

import {
  crearPrestadorApi,
  E2E_PASSWORD,
  type CuentaE2E,
  type ServicioE2E,
  identificadorUnico,
  iniciarSesionEnPagina,
} from './support';

test.describe('Recorrido principal del marketplace', () => {
  let servicio: ServicioE2E;
  let cliente: CuentaE2E;
  let idSolicitud: number;

  test.beforeAll(async ({ browser }) => {
    servicio = await crearPrestadorApi('marketplace');
    cliente = {
      nombre: 'Cliente Marketplace',
      correo: `${identificadorUnico('cliente-marketplace')}@example.org`,
      clave: E2E_PASSWORD,
      idUsuario: 0,
    };
    const contexto = await browser.newContext();
    const pagina = await contexto.newPage();
    await pagina.goto('/registro');
    await pagina.getByLabel('Nombre completo').fill(cliente.nombre);
    await pagina.getByLabel('Correo electrónico').fill(cliente.correo);
    await pagina.getByLabel('Contraseña', { exact: true }).fill(cliente.clave);
    await pagina.getByLabel('Confirmar contraseña').fill(cliente.clave);
    await pagina.getByRole('button', { name: 'Registrarme' }).click();
    await pagina.waitForURL('**/iniciar-sesion*');
    await contexto.close();
  });

  test('explora, solicita, acepta, conversa, completa, califica y reporta', async ({ browser }) => {
    const clienteContexto = await browser.newContext();
    const clientePage = await clienteContexto.newPage();
    await iniciarSesionEnPagina(clientePage, cliente);

    await clientePage.goto('/explorar');
    await expect(clientePage.getByRole('heading', { name: 'Explorar servicios' })).toBeVisible();
    await clientePage.getByLabel('Buscar servicios').fill(servicio.nombreServicio);
    await clientePage.keyboard.press('Enter');
    await expect(clientePage.getByRole('link', { name: servicio.nombreServicio })).toBeVisible();
    await clientePage.getByRole('link', { name: servicio.nombreServicio }).click();
    await expect(clientePage.getByRole('heading', { name: servicio.nombreServicio })).toBeVisible();
    await clientePage.getByRole('link', { name: 'Solicitar este servicio' }).click();
    await clientePage
      .getByRole('textbox', { name: 'Qué necesitas' })
      .fill('Reparar una fuga en la cocina.');
    await clientePage
      .getByRole('textbox', { name: 'Dirección, sector o referencia' })
      .fill('Barrio Marketplace, casa 10.');
    await clientePage.getByRole('combobox', { name: 'Municipio' }).selectOption({ index: 1 });
    await clientePage.getByRole('button', { name: 'Enviar solicitud' }).click();
    await expect(clientePage.getByRole('heading', { name: servicio.nombreServicio })).toBeVisible();
    await expect(
      clientePage.locator('header').getByText('Pendiente', { exact: true })
    ).toBeVisible();
    idSolicitud = Number(new URL(clientePage.url()).pathname.split('/').pop());

    const prestadorContexto = await browser.newContext();
    const prestadorPage = await prestadorContexto.newPage();
    await iniciarSesionEnPagina(prestadorPage, servicio);
    await prestadorPage.goto('/solicitudes');
    await expect(prestadorPage.getByRole('heading', { name: 'Recibidas' })).toBeVisible();
    await prestadorPage
      .getByRole('link', { name: new RegExp(servicio.nombreServicio) })
      .first()
      .click();
    await prestadorPage.getByRole('button', { name: 'Aceptar' }).click();
    await prestadorPage.getByRole('button', { name: 'Sí, aceptar' }).click();
    await expect(
      prestadorPage.locator('header').getByText('Aceptada', { exact: true })
    ).toBeVisible();

    await clientePage.reload();
    await expect(
      clientePage.locator('header').getByText('Aceptada', { exact: true })
    ).toBeVisible();
    await expect(
      clientePage.getByRole('heading', { name: `Contactos de ${servicio.nombre}` })
    ).toBeVisible();
    await clientePage
      .getByRole('textbox', { name: 'Mensaje', exact: true })
      .fill('Ya estoy disponible para coordinar.');
    await clienteContexto.setOffline(true);
    await clientePage.getByRole('button', { name: 'Enviar', exact: true }).click();
    await expect(
      clientePage.getByRole('alert').filter({ hasText: /Revisa tu conexión/ })
    ).toBeVisible();
    await expect(clientePage.getByRole('textbox', { name: 'Mensaje', exact: true })).toHaveValue(
      'Ya estoy disponible para coordinar.'
    );
    await clienteContexto.setOffline(false);
    // La escritura fallida no se envía automáticamente al reconectar.
    const mensajes = await clientePage.request.get(`/api/solicitudes/${idSolicitud}/mensajes`);
    expect(await mensajes.text()).not.toContain('Ya estoy disponible para coordinar.');
    await clientePage.getByRole('button', { name: 'Enviar', exact: true }).click();
    await expect(clientePage.getByText('Ya estoy disponible para coordinar.')).toBeVisible();

    await prestadorPage.reload();
    await prestadorPage.getByRole('button', { name: 'Marcar como completada' }).click();
    await prestadorPage.getByRole('button', { name: 'Sí, completar' }).click();
    await expect(
      prestadorPage.locator('header').getByText('Completada', { exact: true })
    ).toBeVisible();

    await clientePage.reload();
    await expect(
      clientePage.locator('header').getByText('Completada', { exact: true })
    ).toBeVisible();
    await clientePage.getByRole('radio', { name: '5 estrellas' }).press('Space');
    await expect(clientePage.getByRole('radio', { name: '5 estrellas' })).toBeChecked();
    await clientePage.getByRole('textbox', { name: 'Comentario' }).fill('Trabajo claro y puntual.');
    await clientePage.getByRole('button', { name: 'Guardar calificación' }).click();
    await expect(clientePage.getByText('Trabajo claro y puntual.')).toBeVisible();
    await clientePage.getByRole('button', { name: 'Reportar un problema' }).click();
    await clientePage
      .getByRole('textbox', { name: 'Motivo' })
      .fill('El trato durante el servicio fue inapropiado.');
    await clientePage
      .getByRole('textbox', { name: 'Descripción' })
      .fill('El caso debe quedar disponible para moderación.');
    await clientePage.getByRole('button', { name: 'Enviar reporte' }).click();
    await expect(clientePage.getByText(/Tu caso quedó abierto para revisión/)).toBeVisible();

    await prestadorContexto.close();
    await clienteContexto.close();
    expect(idSolicitud).toBeGreaterThan(0);
  });
});
