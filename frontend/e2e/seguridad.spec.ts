import { expect, test } from '@playwright/test';

import { crearAdministradorApi, iniciarSesionEnPagina } from './support';

test('presenta 401/403/403-2fa y permite entrar al área administrativa tras TOTP', async ({
  page,
}) => {
  await page.goto('/401');
  await expect(page.getByRole('heading', { name: 'Tu sesión no está activa' })).toBeVisible();
  await page.goto('/403');
  await expect(
    page.getByRole('heading', { name: 'Esta zona requiere otros permisos' })
  ).toBeVisible();
  await page.goto('/403-2fa');
  await expect(
    page.getByRole('heading', { name: 'Verificación adicional requerida' })
  ).toBeVisible();

  const admin = await crearAdministradorApi('seguridad');
  await iniciarSesionEnPagina(page, admin);
  expect((await page.request.get('/api/admin/resumen')).status()).toBe(403);
  await page.goto('/admin');
  await expect(
    page.getByRole('heading', { name: 'Verificación adicional requerida' })
  ).toBeVisible();
  await page.goto('/seguridad');
  await page.getByRole('button', { name: 'Activar el segundo factor' }).click();
  const secreto = await page.getByText(/^[A-Z2-7]{20,}$/).textContent();
  if (secreto === null) throw new Error('No se encontró la clave TOTP de la activación');
  const { codigoTotp } = await import('./support');
  await page.getByLabel('Código de verificación').fill(codigoTotp(secreto));
  await page.getByRole('button', { name: 'Confirmar activación' }).click();
  await expect(page.getByText('Estado: Activo')).toBeVisible();
  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Área administrativa' })).toBeVisible();
  expect((await page.request.get('/api/admin/resumen')).status()).toBe(200);

  await page.goto('/iniciar-sesion');
  await page.getByLabel('Correo electrónico').fill(admin.correo);
  await page.getByLabel('Contraseña', { exact: true }).fill(admin.clave);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await page.waitForURL('**/verificar-segundo-factor');
  expect((await page.request.get('/api/admin/resumen')).status()).toBe(403);
  await page.getByLabel('Código de verificación').fill(codigoTotp(secreto));
  await page.getByRole('button', { name: 'Verificar y entrar' }).click();
  await page.waitForURL('**/');
  expect((await page.request.get('/api/admin/resumen')).status()).toBe(200);
});
