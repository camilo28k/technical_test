import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { simularFetch } from '../../pruebas/utilidades';
import { PaginaEstadoDeCuenta } from './PaginaEstadoDeCuenta';

const CUENTA = { id: 10, numero: '5300000001' };
const ESTADO = {
  cuenta: { id: 10, numero: '5300000001', tipoCuenta: 'AHORROS', estado: 'ACTIVA', titular: 'Ana María Gómez' },
  saldo: '120000.00',
  saldoDisponible: '120000.00',
  fechaConsulta: '2026-11-16T11:05:00-05:00',
  movimientos: {
    contenido: [
      { operacionId: 502, clase: 'REVERSO', sentido: 'CREDITO', importe: '30000.00', fecha: '2026-11-16T11:02:00-05:00', contraparte: '3300000004', reversada: false },
      { operacionId: 501, clase: 'TRANSFERENCIA', sentido: 'DEBITO', importe: '30000.00', fecha: '2026-11-16T11:00:00-05:00', contraparte: '3300000004', reversada: true },
      { operacionId: 500, clase: 'CONSIGNACION', sentido: 'CREDITO', importe: '120000.00', fecha: '2026-11-16T10:40:00-05:00', contraparte: null, reversada: false },
    ],
    page: 0, size: 20, totalElementos: 3, totalPaginas: 1,
  },
};

describe('PaginaEstadoDeCuenta', () => {
  it('busca la cuenta por número y presenta saldo e historial', async () => {
    const fetch = simularFetch({ status: 200, body: CUENTA }, { status: 200, body: ESTADO });
    render(<PaginaEstadoDeCuenta />);
    const u = userEvent.setup();
    await u.type(screen.getByLabelText('Número de cuenta'), '5300000001');
    await u.type(screen.getByLabelText('Desde'), '2026-11-01');
    await u.type(screen.getByLabelText('Hasta'), '2026-11-30');
    await u.click(screen.getByRole('button', { name: 'Consultar' }));

    expect(await screen.findByText('Ana María Gómez')).toBeInTheDocument();
    expect(fetch.mock.calls[0][0]).toBe('/api/v1/cuentas/numero/5300000001');
    expect(fetch.mock.calls[1][0]).toBe('/api/v1/cuentas/10/estado-de-cuenta?desde=2026-11-01&hasta=2026-11-30&page=0&size=20');

    const filas = screen.getAllByRole('row').slice(1);
    expect(filas).toHaveLength(3);
    expect(within(filas[1]).getByText('Reversada')).toBeInTheDocument();
    // Solo la consignación puede reversarse: la transferencia ya lo está y un reverso no es reversable.
    expect(within(filas[0]).queryByRole('button', { name: 'Reversar' })).toBeNull();
    expect(within(filas[1]).queryByRole('button', { name: 'Reversar' })).toBeNull();
    expect(within(filas[2]).getByRole('button', { name: 'Reversar' })).toBeInTheDocument();
  });

  it('rechaza un rango de fechas invertido sin consultar', async () => {
    const fetch = simularFetch();
    render(<PaginaEstadoDeCuenta />);
    const u = userEvent.setup();
    await u.type(screen.getByLabelText('Número de cuenta'), '5300000001');
    await u.type(screen.getByLabelText('Desde'), '2026-11-30');
    await u.type(screen.getByLabelText('Hasta'), '2026-11-01');
    await u.click(screen.getByRole('button', { name: 'Consultar' }));
    expect(screen.getByRole('alert')).toHaveTextContent('no puede ser posterior');
    expect(fetch).not.toHaveBeenCalled();
  });

  it('informa cuando la cuenta no existe', async () => {
    simularFetch({ status: 404, body: { codigo: 'CUENTA_NO_ENCONTRADA', mensaje: 'No existe la cuenta 5399999999.' } });
    render(<PaginaEstadoDeCuenta numeroInicial="5399999999" />);
    expect(await screen.findByRole('alert')).toHaveTextContent('CUENTA_NO_ENCONTRADA');
  });
});
