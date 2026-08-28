import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import type { ReactElement } from 'react';

import { BarraLateral } from './BarraLateral';
import estilos from './BarraLateral.module.css';

function renderizar(ui: ReactElement, ruta = '/') {
  return render(<MemoryRouter initialEntries={[ruta]}>{ui}</MemoryRouter>);
}

describe('BarraLateral', () => {
  it('muestra los siete destinos solo con su nombre accesible', () => {
    renderizar(<BarraLateral />);

    expect(screen.getByRole('navigation', { name: 'Navegación principal' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Notificaciones' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Inicio' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Calendario' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Mensajes' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Perfil' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Pagos' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Configuración' })).toBeVisible();
    expect(screen.queryByText('Inicio')).not.toBeInTheDocument();
  });

  it('marca el ítem activo y lo anuncia como página actual', () => {
    renderizar(<BarraLateral itemActivo="inicio" />);

    const inicio = screen.getByRole('button', { name: 'Inicio' });
    expect(inicio).toHaveAttribute('aria-current', 'page');
    expect(inicio).toHaveClass(estilos.controlActivo ?? '');
    expect(inicio.closest('li')).toHaveClass(estilos.itemActivo ?? '');
    expect(screen.getByRole('button', { name: 'Perfil' })).not.toHaveAttribute('aria-current');
  });

  it('anuncia el aviso de mensajes sin mostrar un número', () => {
    renderizar(<BarraLateral tieneMensajesSinLeer />);

    expect(screen.getByRole('button', { name: 'Mensajes, hay avisos' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Mensajes' })).not.toBeInTheDocument();
  });

  it('usa enlaces cuando el padre aporta destinos y activa el de la ruta actual', () => {
    renderizar(<BarraLateral destinos={{ inicio: '/', perfil: '/prestador' }} />, '/prestador');

    const perfil = screen.getByRole('link', { name: 'Perfil' });
    expect(perfil).toHaveAttribute('href', '/prestador');
    expect(perfil).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Inicio' })).not.toHaveAttribute(
      'aria-current',
      'page'
    );
    expect(screen.getByRole('button', { name: 'Calendario' })).toBeVisible();
  });
});
