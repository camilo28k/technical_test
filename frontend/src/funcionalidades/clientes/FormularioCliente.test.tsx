import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '../../api/cliente';
import { FormularioCliente } from './FormularioCliente';

async function llenar() {
  const u = userEvent.setup();
  await u.type(screen.getByLabelText('Número de identificación'), '1075123456');
  await u.type(screen.getByLabelText('Nombres'), 'Ana María');
  await u.type(screen.getByLabelText('Apellido'), 'Gómez');
  await u.type(screen.getByLabelText('Correo'), 'ana@correo.com');
  await u.type(screen.getByLabelText('Fecha de nacimiento'), '1995-04-12');
  return u;
}

describe('FormularioCliente', () => {
  it('valida formato sin llamar a la API', async () => {
    const onGuardar = vi.fn();
    render(<FormularioCliente textoBoton="Registrar" onGuardar={onGuardar} />);
    const u = userEvent.setup();
    await u.type(screen.getByLabelText('Correo'), 'sin-arroba');
    await u.click(screen.getByRole('button', { name: 'Registrar' }));
    expect(screen.getByText('Formato usuario@dominio.ext.')).toBeInTheDocument();
    expect(screen.getAllByText('Entre 2 y 100 caracteres.')).toHaveLength(2);
    expect(onGuardar).not.toHaveBeenCalled();
  });

  it('envía los datos y limpia el formulario', async () => {
    const onGuardar = vi.fn().mockResolvedValue(undefined);
    render(<FormularioCliente textoBoton="Registrar" onGuardar={onGuardar} />);
    const u = await llenar();
    await u.click(screen.getByRole('button', { name: 'Registrar' }));
    expect(onGuardar).toHaveBeenCalledWith({
      tipoIdentificacion: 'CC', numeroIdentificacion: '1075123456', nombres: 'Ana María',
      apellido: 'Gómez', correo: 'ana@correo.com', fechaNacimiento: '1995-04-12',
    });
    expect(screen.getByLabelText('Nombres')).toHaveValue('');
  });

  it('presenta la regla de negocio que rechaza la API', async () => {
    const onGuardar = vi.fn().mockRejectedValue(new ApiError(409, {
      codigo: 'CLIENTE_MENOR_DE_EDAD', mensaje: 'El cliente debe ser mayor de edad.',
    }));
    render(<FormularioCliente textoBoton="Registrar" onGuardar={onGuardar} />);
    const u = await llenar();
    await u.click(screen.getByRole('button', { name: 'Registrar' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('CLIENTE_MENOR_DE_EDAD');
    expect(screen.getByLabelText('Nombres')).toHaveValue('Ana María');
  });
});
