import { expect, test, type Page } from '@playwright/test';

import { crearCuentaApi, iniciarSesionEnPagina } from './support';

test('PWA instalable, rutas SPA offline y caché sin API ni datos privados', async ({
  page,
  context,
}) => {
  await page.goto('/iniciar-sesion');
  await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready;
  });
  await page.reload();
  await expect
    .poll(() => page.evaluate(() => navigator.serviceWorker.controller !== null))
    .toBe(true);
  const manifest = await (await page.request.get('/manifest.webmanifest')).json();
  expect(manifest).toMatchObject({
    name: 'Moica',
    short_name: 'Moica',
    display: 'standalone',
    start_url: '/',
    scope: '/',
    theme_color: '#b45309',
  });
  for (const dimension of [192, 512]) {
    const tamaño = await page.evaluate(async (size) => {
      const bitmap = await createImageBitmap(await (await fetch(`/icono-${size}.png`)).blob());
      return [bitmap.width, bitmap.height];
    }, dimension);
    expect(tamaño).toEqual([dimension, dimension]);
  }
  const cdp = await context.newCDPSession(page);
  await cdp.send('Page.enable');
  const instalabilidad = await cdp.send('Page.getInstallabilityErrors');
  expect(instalabilidad.installabilityErrors).toEqual([]);
  await cdp.detach();

  await context.setOffline(true);
  await page.goto('/registro');
  await expect(page.getByRole('heading', { name: 'Crear cuenta' })).toBeVisible();
  await page.goto('/explorar');
  await expect(page.getByRole('alert').filter({ hasText: /conexión/ })).toBeVisible();
  expect((await recursosEnCache(page)).length).toBeGreaterThan(0);
  await comprobarCacheSoloEstatica(page);

  await context.setOffline(false);
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Explorar servicios' })).toBeVisible();

  // Navegar autenticado no debe agregar nada privado a la cache del service worker.
  const cuenta = await crearCuentaApi('pwa');
  await iniciarSesionEnPagina(page, cuenta);
  await page.goto('/solicitudes');
  await expect(page.getByRole('heading', { name: 'Mis solicitudes' })).toBeVisible();
  await page.goto('/seguridad');
  await expect(page.getByRole('heading', { name: 'Seguridad de tu cuenta' })).toBeVisible();
  await comprobarCacheSoloEstatica(page);
});

/** Devuelve todas las URL guardadas por el service worker en cualquier cache. */
async function recursosEnCache(page: Page): Promise<string[]> {
  return page.evaluate(async () => {
    const urls: string[] = [];
    for (const nombre of await caches.keys()) {
      for (const entrada of await (await caches.open(nombre)).keys()) urls.push(entrada.url);
    }
    return urls;
  });
}

/**
 * La cache solo puede contener el cascaron estatico. Cualquier ruta de `/api`, del
 * chat, de los contactos, de un expediente, de administracion o de la sesion queda
 * fuera por construccion: no encaja en esta lista.
 */
async function comprobarCacheSoloEstatica(page: Page) {
  for (const recurso of await recursosEnCache(page)) {
    expect(new URL(recurso).pathname, `recurso cacheado ${recurso}`).toMatch(
      /^\/(?:index\.html|registerSW\.js|manifest\.webmanifest|icono-(192|512)\.png|assets\/[^/]+\.(?:js|css))$/
    );
  }
}
