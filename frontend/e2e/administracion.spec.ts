import { expect, test } from '@playwright/test';

import {
  activarSegundoFactorApi,
  crearAdministradorApi,
  crearCasoModeracionApi,
  crearPrestadorApi,
  iniciarSesionEnPagina,
  mutar,
  seedVerificationRequest,
} from './support';

test('revisa verificación, asigna moderación, resuelve y deja visible la medida', async ({
  page,
  browser,
}) => {
  const escenario = await crearCasoModeracionApi('administracion');
  const admin = await crearAdministradorApi('administracion');
  const secreto = await activarSegundoFactorApi(admin);
  const candidato = await crearPrestadorApi('verificacion', false);
  await seedVerificationRequest(candidato.idUsuario);

  await iniciarSesionEnPagina(page, admin, secreto);
  await page.goto('/admin/verificaciones');
  await expect(page.getByRole('heading', { name: 'Verificaciones documentales' })).toBeVisible();
  await expect(page.getByText(candidato.nombre)).toBeVisible();
  await page.getByRole('button', { name: `Abrir el expediente de ${candidato.nombre}` }).click();
  await expect(page.getByRole('heading', { name: 'Expediente', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Tomar para revisar' }).click();
  await page.getByRole('button', { name: 'Aprobar' }).click();
  await page.getByLabel('Estado', { exact: true }).selectOption('APROBADA');
  await expect(
    page
      .getByRole('row')
      .filter({ hasText: candidato.nombre })
      .getByText('Aprobada', { exact: true })
  ).toBeVisible();
  const prestadorContexto = await browser.newContext();
  const prestadorPage = await prestadorContexto.newPage();
  await iniciarSesionEnPagina(prestadorPage, candidato);
  await mutar(
    prestadorPage.request,
    'PUT',
    `/api/prestador/servicios/${candidato.idServicio}/estado`,
    { estado: 'ACTIVO' }
  );
  expect((await page.request.get(`/api/servicios/${candidato.idServicio}`)).status()).toBe(200);
  await prestadorContexto.close();

  await page.goto('/admin/casos');
  await expect(page.getByRole('heading', { name: 'Casos de moderación' })).toBeVisible();
  await page.getByRole('link', { name: /Trato irrespetuoso/ }).click();
  await page
    .getByRole('combobox', { name: 'Asignar responsable' })
    .selectOption(String(admin.idUsuario));
  await page.getByRole('button', { name: 'Asignar', exact: true }).click();
  await page.getByRole('button', { name: 'Iniciar la revisión' }).click();
  await page.getByRole('radio', { name: /Procedente/ }).check();
  await page
    .getByRole('textbox', { name: 'Resolución' })
    .fill('La conducta quedó acreditada en el expediente.');
  await page.getByRole('button', { name: 'Cerrar el caso' }).click();
  await expect(page.getByText('Cerrado · Procedente', { exact: true })).toBeVisible();

  const afectado = await browser.newContext();
  const paginaAfectada = await afectado.newPage();
  await iniciarSesionEnPagina(paginaAfectada, escenario.servicio);
  await page.getByLabel('Medida que vas a aplicar').selectOption({ label: 'Suspensión E2E' });
  await page
    .getByLabel('Por qué la aplicas')
    .fill('Se deja constancia de la medida del recorrido E2E.');
  await page.getByRole('button', { name: 'Aplicar la medida' }).click();
  await expect(page.getByText(/Sostiene «Suspensión E2E»/)).toBeVisible();
  expect((await paginaAfectada.request.get('/api/auth/sesion')).status()).toBe(401);
  await paginaAfectada.goto('/iniciar-sesion');
  await paginaAfectada.getByLabel('Correo electrónico').fill(escenario.servicio.correo);
  await paginaAfectada.getByLabel('Contraseña', { exact: true }).fill(escenario.servicio.clave);
  await paginaAfectada.getByRole('button', { name: 'Iniciar sesión' }).click();
  await expect(paginaAfectada.getByText(/suspendida/i).first()).toBeVisible();

  await page
    .getByLabel('Qué expuso la persona')
    .fill('Apelación ficticia recibida por el canal externo.');
  await page.getByRole('button', { name: 'Registrar la apelación' }).click();
  await page.getByRole('radio', { name: /Aceptarla/ }).check();
  await page
    .locator('#resolucion-apelacion')
    .fill('Se acepta tras revisar la evidencia adicional.');
  await page.getByRole('button', { name: 'Registrar la decisión' }).click();
  await expect(page.getByText(/La apelación se aceptó/)).toBeVisible();
  // Aceptar la apelación no levanta por sí sola la medida.
  await expect(page.getByText(/Sostiene «Suspensión E2E»/)).toBeVisible();
  await page
    .getByLabel('Motivo para revocar la medida')
    .fill('Decisión manual tras aceptar la apelación.');
  await page.getByRole('button', { name: 'Revocar la medida' }).click();
  await expect(page.getByText(/No tiene ninguna medida vigente/)).toBeVisible();
  await page
    .getByLabel('Motivo para reabrir el caso')
    .fill('Revisar la evidencia adicional aportada.');
  await page.getByRole('button', { name: 'Reabrir el caso' }).click();
  await expect(page.getByText('Reabierto', { exact: true }).first()).toBeVisible();
  await iniciarSesionEnPagina(paginaAfectada, escenario.servicio);
  expect((await paginaAfectada.request.get('/api/auth/sesion')).status()).toBe(200);
  await afectado.close();
});
