import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import App from './App';
import './estilos/global.css';

// React Query gestiona todo el estado remoto de Moica. Se crea una sola
// instancia en la raiz para que el cache se comparta en toda la aplicacion.
const clienteDeConsultas = new QueryClient({
  defaultOptions: {
    queries: {
      // Una peticion fallida se reintenta una vez; mas reintentos solo
      // retrasarian el mensaje de error en una conexion mala.
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

const contenedor = document.getElementById('root');

if (!contenedor) {
  throw new Error('No se encontró el elemento #root en index.html');
}

createRoot(contenedor).render(
  <StrictMode>
    <QueryClientProvider client={clienteDeConsultas}>
      <App />
    </QueryClientProvider>
  </StrictMode>
);
