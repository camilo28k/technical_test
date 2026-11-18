import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { api } from '../../api/cliente';
import { formatearFechaHora, formatearImporte } from '../../api/formato';
import type { EstadoDeCuenta } from '../../api/tipos';
import { Campo } from '../../componentes/Campo';
import { MensajeError } from '../../componentes/MensajeError';
import { Paginador } from '../../componentes/Paginador';

const CLASES: Record<string, string> = {
  CONSIGNACION: 'Consignación',
  RETIRO: 'Retiro',
  TRANSFERENCIA: 'Transferencia',
  REVERSO: 'Reverso',
};

interface Props {
  numeroInicial?: string;
}

/** Saldo e historial de una cuenta con filtro de fechas (Colombia, inclusivas) y paginación. */
export function PaginaEstadoDeCuenta({ numeroInicial = '' }: Props) {
  const [numero, setNumero] = useState(numeroInicial);
  const [cuentaId, setCuentaId] = useState<number | null>(null);
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [filtros, setFiltros] = useState<{ desde?: string; hasta?: string; page: number }>({ page: 0 });
  const [estado, setEstado] = useState<EstadoDeCuenta | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [reversando, setReversando] = useState<number | null>(null);

  const cargar = useCallback(async () => {
    if (cuentaId === null) return;
    try {
      setEstado(await api.estadoDeCuenta(cuentaId, filtros));
      setError(null);
    } catch (err) {
      setError(err);
    }
  }, [cuentaId, filtros]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  const buscarCuenta = useCallback(async (valor: string) => {
    setError(null);
    setEstado(null);
    setCuentaId(null);
    try {
      const cuenta = await api.buscarCuentaPorNumero(valor.trim());
      setCuentaId(cuenta.id);
    } catch (err) {
      setError(err);
    }
  }, []);

  useEffect(() => {
    if (numeroInicial) void buscarCuenta(numeroInicial);
  }, [numeroInicial, buscarCuenta]);

  function enviar(e: FormEvent) {
    e.preventDefault();
    if (!numero.trim()) {
      setError(new Error('Escribe el número de cuenta.'));
      return;
    }
    if (desde && hasta && desde > hasta) {
      setError(new Error('La fecha "desde" no puede ser posterior a "hasta".'));
      return;
    }
    setFiltros({ desde: desde || undefined, hasta: hasta || undefined, page: 0 });
    if (estado?.cuenta.numero !== numero.trim()) void buscarCuenta(numero);
  }

  async function reversar(operacionId: number) {
    if (!window.confirm(`¿Reversar la operación #${operacionId}?`)) return;
    setReversando(operacionId);
    setError(null);
    try {
      await api.reversar(operacionId);
      await cargar();
    } catch (err) {
      setError(err);
    } finally {
      setReversando(null);
    }
  }

  return (
    <section className="tarjeta">
      <h2>Estado de cuenta</h2>
      <form className="acciones" onSubmit={enviar} noValidate>
        <Campo etiqueta="Número de cuenta">
          <input value={numero} onChange={(e) => setNumero(e.target.value)} inputMode="numeric" />
        </Campo>
        <Campo etiqueta="Desde">
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
        </Campo>
        <Campo etiqueta="Hasta">
          <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
        </Campo>
        <button type="submit" className="primario">Consultar</button>
      </form>
      <MensajeError error={error} />
      {estado && (
        <>
          <dl className="resumen">
            <div><dt>Cuenta</dt><dd><code>{estado.cuenta.numero}</code> · {estado.cuenta.tipoCuenta}</dd></div>
            <div><dt>Titular</dt><dd>{estado.cuenta.titular}</dd></div>
            <div><dt>Estado</dt><dd>{estado.cuenta.estado}</dd></div>
            <div><dt>Saldo</dt><dd className="saldo">{formatearImporte(estado.saldo)}</dd></div>
            <div><dt>Saldo disponible</dt><dd>{formatearImporte(estado.saldoDisponible)}</dd></div>
            <div><dt>Consultado</dt><dd>{formatearFechaHora(estado.fechaConsulta)}</dd></div>
          </dl>
          {estado.movimientos.contenido.length === 0 ? (
            <p className="nota">La cuenta no tiene movimientos en el periodo.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Fecha</th><th>Operación</th><th>Clase</th><th>Contraparte</th>
                  <th className="num">Débito</th><th className="num">Crédito</th><th />
                </tr>
              </thead>
              <tbody>
                {estado.movimientos.contenido.map((l) => (
                  <tr key={`${l.operacionId}-${l.sentido}`}>
                    <td>{formatearFechaHora(l.fecha)}</td>
                    <td>#{l.operacionId}</td>
                    <td>
                      {CLASES[l.clase] ?? l.clase}
                      {l.reversada && <span className="etiqueta">Reversada</span>}
                    </td>
                    <td>{l.contraparte ? <code>{l.contraparte}</code> : '—'}</td>
                    <td className="num">{l.sentido === 'DEBITO' ? formatearImporte(l.importe) : ''}</td>
                    <td className="num">{l.sentido === 'CREDITO' ? formatearImporte(l.importe) : ''}</td>
                    <td>
                      {!l.reversada && l.clase !== 'REVERSO' && (
                        <button type="button" disabled={reversando === l.operacionId} onClick={() => reversar(l.operacionId)}>
                          Reversar
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <Paginador
            page={estado.movimientos.page}
            totalPaginas={estado.movimientos.totalPaginas}
            totalElementos={estado.movimientos.totalElementos}
            onCambiar={(page) => setFiltros((f) => ({ ...f, page }))}
          />
        </>
      )}
    </section>
  );
}
