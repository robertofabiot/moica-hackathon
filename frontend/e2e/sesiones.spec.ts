import { expect, test } from '@playwright/test';
import { crearCuentaApi, ejecutarSql, iniciarSesionEnPagina, mutar } from './support';

test('una cuenta ordinaria no accede a admin ni por página ni por API', async ({ page }) => {
  const cuenta = await crearCuentaApi('sin-rol');
  await iniciarSesionEnPagina(page, cuenta);
  expect((await page.request.get('/api/admin/resumen')).status()).toBe(403);
  await page.goto('/admin');
  await expect(
    page.getByRole('heading', { name: 'Esta zona requiere otros permisos' })
  ).toBeVisible();
});

test('la expiración persistida invalida el JWT y solicita autenticarse', async ({ page }) => {
  const cuenta = await crearCuentaApi('expiracion');
  await iniciarSesionEnPagina(page, cuenta);
  // Solo la cuenta aislada de esta prueba: conservar la restricción fechaFin > fechaInicio.
  await ejecutarSql(`UPDATE sesion SET fecha_inicio = CURRENT_TIMESTAMP - INTERVAL '2 days',
    fecha_expiracion = CURRENT_TIMESTAMP - INTERVAL '1 day' WHERE id_usuario = ${cuenta.idUsuario};`);
  expect((await page.request.get('/api/auth/sesion')).status()).toBe(401);
  await page.goto('/seguridad');
  await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
});

test('cambiar credenciales revoca otra sesión que aún conserva su cookie', async ({
  browser,
  page,
}) => {
  const cuenta = await crearCuentaApi('revocacion');
  await iniciarSesionEnPagina(page, cuenta);
  const otra = await browser.newContext();
  try {
    const segundaPagina = await otra.newPage();
    await iniciarSesionEnPagina(segundaPagina, cuenta);
    await mutar(page.request, 'PUT', '/api/auth/clave', {
      claveActual: cuenta.clave,
      claveNueva: `${cuenta.clave}X`,
    });
    expect((await segundaPagina.request.get('/api/auth/sesion')).status()).toBe(401);
    await segundaPagina.goto('/seguridad');
    await expect(segundaPagina.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  } finally {
    await otra.close();
  }
});
