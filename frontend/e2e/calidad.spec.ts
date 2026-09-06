import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page, type TestInfo } from '@playwright/test';
import { writeFile } from 'node:fs/promises';

import {
  activarSegundoFactorApi,
  crearAdministradorApi,
  crearPrestadorApi,
  crearSolicitudPendienteApi,
  iniciarSesionEnPagina,
} from './support';

test('audita accesibilidad crítica, PWA y overflow en los viewports finales', async ({
  page,
}, testInfo) => {
  test.setTimeout(180_000);
  const rutas = ['/', '/registro', '/401', '/403', '/403-2fa', '/iniciar-sesion', '/explorar'];
  for (const ruta of rutas) {
    await page.goto(ruta);
    await page.addStyleTag({
      content: '*,:before,:after { animation: none !important; transition: none !important; }',
    });
    const resultado = await new AxeBuilder({ page }).analyze();
    expect(resultado.violations, `violaciones axe en ${ruta}`).toEqual([]);
  }

  await page.goto('/401');
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus-visible')).toBeVisible();
  await page.goto('/iniciar-sesion');
  await page.getByLabel('Contraseña', { exact: true }).focus();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('button', { name: 'Mostrar contraseña' })).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.getByLabel('Contraseña', { exact: true })).toHaveAttribute('type', 'text');
  await evidenciaResponsive(page, 'acceso', testInfo);

  const manifest = await page.evaluate(async () => {
    const respuesta = await fetch('/manifest.webmanifest', { cache: 'no-store' });
    return { status: respuesta.status, body: (await respuesta.json()) as Record<string, unknown> };
  });
  expect(manifest.status).toBe(200);
  expect(manifest.body).toMatchObject({
    name: 'Moica',
    short_name: 'Moica',
    start_url: '/',
    display: 'standalone',
  });
  expect(manifest.body.icons).toEqual(
    expect.arrayContaining([
      expect.objectContaining({ src: 'icono-192.png', sizes: '192x192' }),
      expect.objectContaining({ src: 'icono-512.png', sizes: '512x512' }),
    ])
  );
  const serviceWorker = await page.evaluate(async () => (await fetch('/sw.js')).text());
  expect(serviceWorker).not.toContain("'/api/");
  expect(serviceWorker).not.toContain('"/api/');

  const servicio = await crearPrestadorApi('responsive');
  const solicitud = await crearSolicitudPendienteApi(servicio);
  await iniciarSesionEnPagina(page, solicitud.cliente);
  await page.goto(`/solicitudes/${solicitud.idSolicitud}`);
  await expect(page.getByRole('heading', { name: servicio.nombreServicio })).toBeVisible();
  await evidenciaResponsive(page, 'solicitud', testInfo);

  const admin = await crearAdministradorApi('responsive');
  const secreto = await activarSegundoFactorApi(admin);
  await iniciarSesionEnPagina(page, admin, secreto);
  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Área administrativa' })).toBeVisible();
  await evidenciaResponsive(page, 'admin', testInfo);

  for (const [ancho, alto] of [
    [375, 812],
    [768, 1024],
    [1280, 800],
  ] as const) {
    await page.setViewportSize({ width: ancho, height: alto });
    await page.goto('/403-2fa');
    await expect(
      page.getByRole('heading', { name: 'Verificación adicional requerida' })
    ).toBeVisible();
    await comprobarOverflow(page, ancho, alto, `403-2fa ${ancho}x${alto}`);
  }

  await page.setViewportSize({ width: 320, height: 800 });
  await page.goto('/registro');
  await comprobarOverflow(page, 320, 800, 'reflow equivalente a 400% sobre 1280');
});

async function evidenciaResponsive(page: Page, nombre: string, info: TestInfo) {
  const medidas = [];
  for (const [width, height] of [
    [375, 812],
    [768, 1024],
    [1280, 800],
  ]) {
    if (width === undefined || height === undefined) throw new Error('Viewport incompleto');
    await page.setViewportSize({ width, height });
    const resultado = await new AxeBuilder({ page }).analyze();
    expect(resultado.violations, `axe ${nombre} ${width}`).toEqual([]);
    await comprobarOverflow(page, width, height, nombre);
    medidas.push(
      await page.evaluate(() => ({
        ruta: location.pathname,
        width: innerWidth,
        height: innerHeight,
        scrollWidth: document.documentElement.scrollWidth,
        clientWidth: document.documentElement.clientWidth,
      }))
    );
    await page.screenshot({
      path: info.outputPath(`evidencia-${nombre}-${width}x${height}.png`),
      fullPage: true,
    });
  }
  await writeFile(
    info.outputPath(`evidencia-medidas-${nombre}.json`),
    JSON.stringify(medidas, null, 2)
  );
}

async function comprobarOverflow(page: Page, ancho: number, alto: number, nombre: string) {
  await page.setViewportSize({ width: ancho, height: alto });
  const medicion = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
    scrollHeight: document.documentElement.scrollHeight,
    clientHeight: document.documentElement.clientHeight,
  }));
  expect(medicion.scrollWidth, `overflow horizontal en ${nombre}`).toBe(medicion.clientWidth);
  expect(medicion.scrollHeight).toBeGreaterThanOrEqual(medicion.clientHeight);
}
