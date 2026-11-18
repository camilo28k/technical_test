import { render, screen } from '@testing-library/react';
import { ApiError } from '../api/cliente';
import { MensajeError } from './MensajeError';

describe('MensajeError', () => {
  it('no muestra nada sin error', () => {
    const { container } = render(<MensajeError error={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('muestra mensaje, código y detalles de validación de la API', () => {
    render(<MensajeError error={new ApiError(400, {
      codigo: 'VALIDACION', mensaje: 'La petición tiene datos inválidos.',
      detalles: [{ campo: 'correo', mensaje: 'formato inválido' }],
    })} />);
    const alerta = screen.getByRole('alert');
    expect(alerta).toHaveTextContent('La petición tiene datos inválidos.');
    expect(alerta).toHaveTextContent('VALIDACION');
    expect(alerta).toHaveTextContent('correo: formato inválido');
  });
});
