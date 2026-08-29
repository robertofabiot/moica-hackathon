import { zodResolver } from '@hookform/resolvers/zod';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useForm } from 'react-hook-form';
import { describe, expect, it } from 'vitest';
import { z } from 'zod';

import { Entrada } from './Entrada';
import estilos from './Entrada.module.css';

describe('Entrada', () => {
  it('anuncia el mensaje de error debajo del campo', () => {
    render(
      <>
        <label htmlFor="correo">Correo</label>
        <Entrada id="correo" mensajeDeError="Escribe un correo electrónico válido." />
      </>
    );

    const campo = screen.getByLabelText('Correo');
    expect(campo).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('alert')).toHaveTextContent('Escribe un correo electrónico válido.');
    expect(campo).toHaveAccessibleDescription('Escribe un correo electrónico válido.');
  });

  it('entrega el ref a react-hook-form para validar con zod', async () => {
    const persona = userEvent.setup();
    render(<FormularioDeNombre />);

    await persona.click(screen.getByRole('button', { name: 'Enviar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Escribe un nombre.');
  });

  it('pinta el icono a la izquierda del campo', () => {
    render(
      <>
        <label htmlFor="servicio">Servicio</label>
        <Entrada id="servicio" icono={<span data-testid="icono-lupa" />} />
      </>
    );

    expect(screen.getByTestId('icono-lupa')).toBeInTheDocument();
  });

  it('quita el borde propio en la variante fusionada', () => {
    render(
      <>
        <label htmlFor="ubicacion">Ubicación</label>
        <Entrada id="ubicacion" variante="fusionada" />
      </>
    );

    expect(screen.getByLabelText('Ubicación')).toHaveClass(estilos.fusionada ?? '');
  });
});

function FormularioDeNombre() {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<{ nombre: string }>({
    resolver: zodResolver(z.object({ nombre: z.string().min(1, 'Escribe un nombre.') })),
  });

  return (
    <form onSubmit={handleSubmit(() => undefined)}>
      <label htmlFor="nombre">Nombre</label>
      <Entrada id="nombre" mensajeDeError={errors.nombre?.message} {...register('nombre')} />
      <button type="submit">Enviar</button>
    </form>
  );
}
