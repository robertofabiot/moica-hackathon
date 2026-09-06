// Agrega a Vitest los matchers de Testing Library (`toBeInTheDocument`,
// `toBeVisible`, ...), que describen la intencion mejor que comprobar nodos.
import '@testing-library/jest-dom/vitest';

import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Sin variables globales de Vitest, la limpieza automatica de Testing Library
// no se registra sola: sin esto, cada prueba veria tambien lo que renderizo la
// anterior.
afterEach(() => {
  cleanup();
});
