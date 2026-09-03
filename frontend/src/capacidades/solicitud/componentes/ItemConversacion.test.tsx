import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import ItemConversacion, { AvatarDeChat } from './ItemConversacion';
import estilos from './ItemConversacion.module.css';

describe('ItemConversacion', () => {
  const propiedades = {
    nombre: 'Ana Cliente',
    servicio: 'Reparación de fugas',
    extracto: 'Aceptada',
    instante: '11:05 a. m.',
    fechaIso: '2026-08-29T11:05:00-06:00',
    iniciales: 'AC',
    seleccionado: false,
    alSeleccionar: vi.fn(),
  };

  it('muestra nombre, servicio, extracto y hora', () => {
    render(<ItemConversacion {...propiedades} />);

    const fila = screen.getByRole('button', { name: /Ana Cliente/ });
    expect(fila).toHaveTextContent('Reparación de fugas');
    expect(fila).toHaveTextContent('Aceptada');
    expect(fila).toHaveTextContent('11:05 a. m.');
    expect(fila).not.toHaveAttribute('aria-current');
    expect(screen.getByText('AC')).toBeInTheDocument();
  });

  it('marca la fila seleccionada', () => {
    render(<ItemConversacion {...propiedades} seleccionado />);

    const fila = screen.getByRole('button', { name: /Ana Cliente/ });
    expect(fila).toHaveAttribute('aria-current', 'true');
    expect(fila).toHaveClass(estilos.seleccionado ?? '');
  });

  it('avisa al padre al elegir la conversación', async () => {
    const alSeleccionar = vi.fn();
    const persona = userEvent.setup();
    render(<ItemConversacion {...propiedades} alSeleccionar={alSeleccionar} />);

    await persona.click(screen.getByRole('button', { name: /Ana Cliente/ }));
    expect(alSeleccionar).toHaveBeenCalledTimes(1);
  });
});

describe('AvatarDeChat', () => {
  it('usa la foto cuando hay una dirección', () => {
    const { container } = render(
      <AvatarDeChat nombre="Ana Cliente" iniciales="AC" urlFoto="https://moica.test/ana.png" />
    );

    expect(container.querySelector('img')).toHaveAttribute('src', 'https://moica.test/ana.png');
    expect(screen.queryByText('AC')).not.toBeInTheDocument();
  });
});
