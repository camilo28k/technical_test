import { useState, type FormEvent } from 'react';
import { api } from '../../api/cliente';
import { formatearImporte, normalizarImporte, validarImporte } from '../../api/formato';
import type { Operacion } from '../../api/tipos';
import { Campo } from '../../componentes/Campo';
import { MensajeError } from '../../componentes/MensajeError';
import { ResumenOperacion } from './ResumenOperacion';

type Tipo = 'CONSIGNACION' | 'RETIRO' | 'TRANSFERENCIA';

const ETIQUETAS: Record<Tipo, string> = {
  CONSIGNACION: 'Consignación',
  RETIRO: 'Retiro',
  TRANSFERENCIA: 'Transferencia',
};

/** Registro de consignaciones, retiros y transferencias por número de cuenta. */
export function FormularioOperacion() {
  const [tipo, setTipo] = useState<Tipo>('CONSIGNACION');
  const [cuenta, setCuenta] = useState('');
  const [destino, setDestino] = useState('');
  const [importe, setImporte] = useState('');
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [errorApi, setErrorApi] = useState<unknown>(null);
  const [resultado, setResultado] = useState<Operacion | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    const encontrados: Record<string, string> = {};
    if (!cuenta.trim()) encontrados.cuenta = 'Es obligatoria.';
    if (tipo === 'TRANSFERENCIA') {
      if (!destino.trim()) encontrados.destino = 'Es obligatoria.';
      else if (destino.trim() === cuenta.trim()) encontrados.destino = 'Debe ser distinta de la cuenta de origen.';
    }
    const errorImporte = validarImporte(importe);
    if (errorImporte) encontrados.importe = errorImporte;
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) return;

    const monto = normalizarImporte(importe);
    setEnviando(true);
    setErrorApi(null);
    setResultado(null);
    try {
      const operacion = tipo === 'CONSIGNACION'
        ? await api.consignar(cuenta.trim(), monto)
        : tipo === 'RETIRO'
          ? await api.retirar(cuenta.trim(), monto)
          : await api.transferir(cuenta.trim(), destino.trim(), monto);
      setResultado(operacion);
      setImporte('');
    } catch (err) {
      setErrorApi(err);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section className="tarjeta">
      <h2>Registrar operación</h2>
      <div className="pestanas-internas" role="radiogroup" aria-label="Tipo de operación">
        {(Object.keys(ETIQUETAS) as Tipo[]).map((t) => (
          <label key={t} className={tipo === t ? 'activa' : undefined}>
            <input type="radio" name="tipo" value={t} checked={tipo === t} onChange={() => setTipo(t)} />
            {ETIQUETAS[t]}
          </label>
        ))}
      </div>
      <form className="formulario" onSubmit={enviar} noValidate>
        <div className="rejilla">
          <Campo etiqueta={tipo === 'TRANSFERENCIA' ? 'Cuenta de origen' : 'Número de cuenta'} error={errores.cuenta}>
            <input value={cuenta} onChange={(e) => setCuenta(e.target.value)} inputMode="numeric" />
          </Campo>
          {tipo === 'TRANSFERENCIA' && (
            <Campo etiqueta="Cuenta de destino" error={errores.destino}>
              <input value={destino} onChange={(e) => setDestino(e.target.value)} inputMode="numeric" />
            </Campo>
          )}
          <Campo etiqueta="Importe (COP)" error={errores.importe}>
            <input value={importe} onChange={(e) => setImporte(e.target.value)} inputMode="decimal" placeholder="150000.00" />
          </Campo>
        </div>
        <MensajeError error={errorApi} />
        <div className="acciones">
          <button type="submit" className="primario" disabled={enviando}>Registrar {ETIQUETAS[tipo].toLowerCase()}</button>
        </div>
      </form>
      {resultado && (
        <div role="status" className="alerta alerta-exito">
          Operación #{resultado.id} registrada por {formatearImporte(resultado.importe)}.
          <ResumenOperacion operacion={resultado} />
        </div>
      )}
    </section>
  );
}
