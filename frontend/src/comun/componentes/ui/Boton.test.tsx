import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';

import { Boton } from './Boton';
import estilos from './Boton.module.css';

describe('Boton', () => {
  it('usa type button por omisión para no enviar un formulario', () => {
    render(
      <form>
        <Boton>Guardar</Boton>
      </form>
    );

    expect(screen.getByRole('button', { name: 'Guardar' })).toHaveAttribute('type', 'button');
  });

  it('respeta type submit cuando la acción envía el formulario', () => {
    render(<Boton type="submit">Enviar</Boton>);

    expect(screen.getByRole('button', { name: 'Enviar' })).toHaveAttribute('type', 'submit');
  });

  it('aplica la variante secundaria cuando se pide', () => {
    render(<Boton variante="secundario">Cancelar</Boton>);

    expect(screen.getByRole('button', { name: 'Cancelar' })).toHaveClass(estilos.secundario ?? '');
  });

  it('aplica la forma de píldora cuando se pide', () => {
    render(<Boton forma="pildora">Volver a explorar</Boton>);

    expect(screen.getByRole('button', { name: 'Volver a explorar' })).toHaveClass(
      estilos.pildora ?? ''
    );
  });

  it('con destino se pinta como enlace conservando la variante primaria', () => {
    render(
      <MemoryRouter>
        <Boton variante="primario" to="/prestador/servicios/nuevo">
          Publicar servicio
        </Boton>
      </MemoryRouter>
    );

    const enlace = screen.getByRole('link', { name: 'Publicar servicio' });
    expect(enlace).toHaveAttribute('href', '/prestador/servicios/nuevo');
    expect(enlace).toHaveClass(estilos.primario ?? '');
  });
});
