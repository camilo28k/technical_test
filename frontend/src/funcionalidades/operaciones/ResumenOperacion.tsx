import { formatearImporte } from '../../api/formato';
import type { Operacion } from '../../api/tipos';

/** Movimientos de una operación con el saldo resultante cuando la API lo informa. */
export function ResumenOperacion({ operacion }: { operacion: Operacion }) {
  return (
    <ul className="movimientos">
      {operacion.movimientos.map((m) => (
        <li key={m.id}>
          <code>{m.numeroCuenta}</code> · {m.sentido === 'DEBITO' ? 'Débito' : 'Crédito'}
          {m.saldoResultante !== null && <> · saldo {formatearImporte(m.saldoResultante)}</>}
        </li>
      ))}
    </ul>
  );
}
