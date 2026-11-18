import { useCallback, useEffect, useState } from 'react';
import { api } from '../../api/cliente';
import { formatearImporte } from '../../api/formato';
import type { Cliente, Cuenta, TipoCuenta } from '../../api/tipos';
import { MensajeError } from '../../componentes/MensajeError';

interface Props {
  cliente: Cliente;
  onVerEstado: (cuenta: Cuenta) => void;
}

/** Productos de un cliente: apertura, cambio de estado y exención GMF. */
export function CuentasDeCliente({ cliente, onVerEstado }: Props) {
  const [cuentas, setCuentas] = useState<Cuenta[]>([]);
  const [tipo, setTipo] = useState<TipoCuenta>('AHORROS');
  const [error, setError] = useState<unknown>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  const cargar = useCallback(async () => {
    try {
      setCuentas(await api.cuentasDeCliente(cliente.id));
    } catch (err) {
      setError(err);
    }
  }, [cliente.id]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  async function ejecutar(accion: () => Promise<Cuenta>, mensaje: (c: Cuenta) => string) {
    setError(null);
    setAviso(null);
    try {
      const cuenta = await accion();
      setAviso(mensaje(cuenta));
      await cargar();
    } catch (err) {
      setError(err);
    }
  }

  return (
    <section className="tarjeta">
      <h3>Cuentas de {cliente.nombres} {cliente.apellido}</h3>
      <div className="acciones">
        <select aria-label="Tipo de cuenta" value={tipo} onChange={(e) => setTipo(e.target.value as TipoCuenta)}>
          <option value="AHORROS">Ahorros</option>
          <option value="CORRIENTE">Corriente</option>
        </select>
        <button
          type="button"
          className="primario"
          onClick={() => ejecutar(() => api.abrirCuenta(cliente.id, tipo), (c) => `Cuenta ${c.numero} abierta.`)}
        >
          Abrir cuenta
        </button>
      </div>
      {aviso && <div role="status" className="alerta alerta-exito">{aviso}</div>}
      <MensajeError error={error} />
      {cuentas.length === 0 ? (
        <p className="nota">El cliente no tiene cuentas.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Número</th><th>Tipo</th><th>Estado</th><th className="num">Saldo</th><th>Exenta GMF</th><th />
            </tr>
          </thead>
          <tbody>
            {cuentas.map((c) => (
              <tr key={c.id}>
                <td><code>{c.numero}</code></td>
                <td>{c.tipoCuenta === 'AHORROS' ? 'Ahorros' : 'Corriente'}</td>
                <td><span className={`estado estado-${c.estado.toLowerCase()}`}>{c.estado}</span></td>
                <td className="num">{formatearImporte(c.saldo)}</td>
                <td>
                  <input
                    type="checkbox"
                    aria-label={`Exenta GMF ${c.numero}`}
                    checked={c.exentaGmf}
                    disabled={c.estado === 'CANCELADA'}
                    onChange={(e) => ejecutar(
                      () => api.cambiarExencionGmf(c.id, e.target.checked),
                      (r) => `Cuenta ${r.numero}: exención GMF ${r.exentaGmf ? 'activada' : 'retirada'}.`,
                    )}
                  />
                </td>
                <td className="acciones-fila">
                  <button type="button" onClick={() => onVerEstado(c)}>Estado de cuenta</button>
                  {c.estado === 'ACTIVA' && (
                    <button type="button" onClick={() => ejecutar(
                      () => api.cambiarEstadoCuenta(c.id, 'inactivar'), (r) => `Cuenta ${r.numero} inactivada.`,
                    )}>Inactivar</button>
                  )}
                  {c.estado === 'INACTIVA' && (
                    <button type="button" onClick={() => ejecutar(
                      () => api.cambiarEstadoCuenta(c.id, 'activar'), (r) => `Cuenta ${r.numero} activada.`,
                    )}>Activar</button>
                  )}
                  {c.estado !== 'CANCELADA' && (
                    <button type="button" className="peligro" onClick={() => {
                      if (window.confirm(`¿Cancelar la cuenta ${c.numero}? No se puede deshacer.`)) {
                        void ejecutar(() => api.cambiarEstadoCuenta(c.id, 'cancelar'), (r) => `Cuenta ${r.numero} cancelada.`);
                      }
                    }}>Cancelar</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
