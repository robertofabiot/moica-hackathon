import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import BurbujaMensaje from './BurbujaMensaje';
import estilos from './BurbujaMensaje.module.css';

describe('BurbujaMensaje', () => {
  it('alinea a la derecha el mensaje propio', () => {
    render(<BurbujaMensaje contenido="Voy en camino." instante="11:05 a. m." esPropio />);

    const burbuja = screen.getByText('Voy en camino.').closest('article');
    expect(burbuja).toHaveClass(estilos.propia ?? '');
    expect(burbuja).toHaveTextContent('11:05 a. m.');
  });

  it('alinea a la izquierda el mensaje de la contraparte', () => {
    render(
      <BurbujaMensaje contenido="Llego a las tres." instante="11:06 a. m." esPropio={false} />
    );

    const burbuja = screen.getByText('Llego a las tres.').closest('article');
    expect(burbuja).toHaveClass(estilos.ajena ?? '');
    expect(burbuja).not.toHaveClass(estilos.propia ?? '');
  });
});
