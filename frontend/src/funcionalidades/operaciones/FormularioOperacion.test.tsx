import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { cuerpoEnviado, simularFetch } from '../../pruebas/utilidades';
import { FormularioOperacion } from './FormularioOperacion';

describe('FormularioOperacion', () => {
  it('registra una transferencia con importe normalizado y muestra los saldos', async () => {
    const fetch = simularFetch({
      status: 201,
      body: {
        id: 501, clase: 'TRANSFERENCIA', importe: '30000.00', fecha: '2026-11-16T11:00:00-05:00',
        operacionOriginalId: null,
        movimientos: [
          { id: 1, numeroCuenta: '5300000001', sentido: 'DEBITO', saldoResultante: '120000.00' },
          { id: 2, numeroCuenta: '3300000004', sentido: 'CREDITO', saldoResultante: '30000.00' },
        ],
      },
    });
    render(<FormularioOperacion />);
    const u = userEvent.setup();
    await u.click(screen.getByLabelText('Transferencia'));
    await u.type(screen.getByLabelText('Cuenta de origen'), '5300000001');
    await u.type(screen.getByLabelText('Cuenta de destino'), '3300000004');
    await u.type(screen.getByLabelText('Importe (COP)'), '30000');
    await u.click(screen.getByRole('button', { name: /registrar transferencia/i }));

    expect(await screen.findByRole('status')).toHaveTextContent('Operación #501 registrada');
    expect(fetch.mock.calls[0][0]).toBe('/api/v1/operaciones/transferencias');
    expect(cuerpoEnviado(fetch)).toEqual({
      numeroCuentaOrigen: '5300000001', numeroCuentaDestino: '3300000004', importe: '30000.00',
    });
  });

  it('no envía transferencias a la misma cuenta ni importes inválidos', async () => {
    const fetch = simularFetch();
    render(<FormularioOperacion />);
    const u = userEvent.setup();
    await u.click(screen.getByLabelText('Transferencia'));
    await u.type(screen.getByLabelText('Cuenta de origen'), '5300000001');
    await u.type(screen.getByLabelText('Cuenta de destino'), '5300000001');
    await u.type(screen.getByLabelText('Importe (COP)'), '10.555');
    await u.click(screen.getByRole('button', { name: /registrar transferencia/i }));
    expect(screen.getByText('Debe ser distinta de la cuenta de origen.')).toBeInTheDocument();
    expect(screen.getByText(/hasta dos decimales/)).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });

  it('muestra el error de fondos insuficientes de la API', async () => {
    simularFetch({
      status: 409,
      body: { codigo: 'FONDOS_INSUFICIENTES', mensaje: 'La cuenta 5300000001 no tiene fondos suficientes.', detalles: [] },
    });
    render(<FormularioOperacion />);
    const u = userEvent.setup();
    await u.click(screen.getByLabelText('Retiro'));
    await u.type(screen.getByLabelText('Número de cuenta'), '5300000001');
    await u.type(screen.getByLabelText('Importe (COP)'), '200000');
    await u.click(screen.getByRole('button', { name: /registrar retiro/i }));
    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('no tiene fondos suficientes');
    expect(alerta).toHaveTextContent('FONDOS_INSUFICIENTES');
  });
});
