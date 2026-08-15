import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';
import { VitePWA } from 'vite-plugin-pwa';

// En produccion, el frontend y la API se publican bajo un mismo origen. Durante
// el desarrollo ese contrato se conserva con este proxy: el navegador siempre
// llama a rutas relativas y Vite las reenvia a Spring Boot.
export default defineConfig(({ mode }) => {
  // El puerto del backend sale del mismo `.env` de la raiz que usa el resto del
  // monorepo, para no mantener dos definiciones del mismo valor.
  const entorno = loadEnv(mode, '..', '');
  const destinoApi = `http://localhost:${entorno.MOICA_BACKEND_PORT ?? '8080'}`;

  return {
    plugins: [
      react(),
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: ['icono-192.png', 'icono-512.png'],
        // Manifiesto minimo: solo lo necesario para que la aplicacion sea
        // instalable. Los colores son neutros a proposito porque la paleta de
        // marca todavia no esta aprobada.
        manifest: {
          name: 'Moica',
          short_name: 'Moica',
          description:
            'Moica conecta clientes con trabajadores independientes, emprendimientos y pequeñas empresas que ofrecen servicios.',
          lang: 'es',
          start_url: '/',
          scope: '/',
          display: 'standalone',
          orientation: 'portrait',
          background_color: '#ffffff',
          theme_color: '#ffffff',
          icons: [
            { src: 'icono-192.png', sizes: '192x192', type: 'image/png' },
            { src: 'icono-512.png', sizes: '512x512', type: 'image/png' },
          ],
        },
      }),
    ],
    server: {
      port: 5173,
      proxy: {
        '/api': { target: destinoApi, changeOrigin: false },
        '/actuator': { target: destinoApi, changeOrigin: false },
      },
    },
  };
});
