import { QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';

import App from './App';
import { crearClienteDeConsultas } from './capacidades/auth';
import './estilos/global.css';

// React Query gestiona todo el estado remoto de Moica. Se crea una sola
// instancia en la raiz para que el cache se comparta en toda la aplicacion, con
// las mismas reglas que aplican las pruebas.
const clienteDeConsultas = crearClienteDeConsultas();

const contenedor = document.getElementById('root');

if (!contenedor) {
  throw new Error('No se encontró el elemento #root en index.html');
}

createRoot(contenedor).render(
  <StrictMode>
    <QueryClientProvider client={clienteDeConsultas}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>
);
