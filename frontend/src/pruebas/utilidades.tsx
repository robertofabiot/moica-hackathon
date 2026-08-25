import { QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router';

import { crearClienteDeConsultas } from '../capacidades/auth';

/**
 * Monta un componente con los proveedores que necesita la aplicación real.
 *
 * El cliente es el mismo que arma la aplicación —con su regla de «un 401 autenticado termina la
 * sesión»— para que las pruebas no comprueben una configuración que no existe fuera de ellas. Solo
 * cambia el número de reintentos: cada prueba estrena el suyo para que ninguna vea la caché de
 * otra, y un fallo debe verse a la primera.
 *
 * Devuelve también el cliente, porque hay reglas que se comprueban mirando la caché: que el secreto
 * del segundo factor no sobreviva a la pantalla que lo mostró, o que al terminar una sesión no
 * quede en memoria nada de la cuenta anterior.
 */
export function renderizarConProveedores(ui: ReactElement, rutaInicial = '/') {
  const cliente = crearClienteDeConsultas();
  cliente.setDefaultOptions({
    queries: { retry: false, refetchOnWindowFocus: false },
    mutations: { retry: false },
  });

  return {
    ...render(
      <MemoryRouter initialEntries={[rutaInicial]}>
        <QueryClientProvider client={cliente}>{ui}</QueryClientProvider>
      </MemoryRouter>
    ),
    cliente,
  };
}
