import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router';

/**
 * Monta un componente con los proveedores que necesita la aplicación real.
 *
 * Cada prueba estrena su propio cliente de React Query para que ninguna vea la caché de otra, y no
 * reintenta: en una prueba, un fallo debe verse a la primera.
 */
export function renderizarConProveedores(ui: ReactElement, rutaInicial = '/') {
  const cliente = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <MemoryRouter initialEntries={[rutaInicial]}>
      <QueryClientProvider client={cliente}>{ui}</QueryClientProvider>
    </MemoryRouter>
  );
}
