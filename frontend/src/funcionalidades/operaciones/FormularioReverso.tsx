import { useState, type FormEvent } from 'react';
import { api } from '../../api/cliente';
import type { Operacion } from '../../api/tipos';
import { Campo } from '../../componentes/Campo';
import { MensajeError } from '../../componentes/MensajeError';
import { ResumenOperacion } from './ResumenOperacion';

/** Reverso completo de una operación por su id (consultándola antes de confirmar). */
export function FormularioReverso() {
  const [id, setId] = useState('');
  const [operacion, setOperacion] = useState<Operacion | null>(null);
  const [reverso, setReverso] = useState<Operacion | null>(null);
  const [error, setError] = useState<unknown>(null);

  async function consultar(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setOperacion(null);
    setReverso(null);
    if (!/^\d+$/.test(id.trim())) {
      setError(new Error('Escribe el número de la operación.'));
      return;
    }
    try {
      setOperacion(await api.consultarOperacion(Number(id)));
    } catch (err) {
      setError(err);
    }
  }

  async function reversar() {
    if (!operacion) return;
    setError(null);
    try {
      setReverso(await api.reversar(operacion.id));
      setOperacion(null);
    } catch (err) {
      setError(err);
    }
  }

  return (
    <section className="tarjeta">
      <h2>Reversar operación</h2>
      <form className="acciones" onSubmit={consultar} noValidate>
        <Campo etiqueta="Número de operación">
          <input value={id} onChange={(e) => setId(e.target.value)} inputMode="numeric" />
        </Campo>
        <button type="submit">Consultar</button>
      </form>
      <MensajeError error={error} />
      {operacion && (
        <div className="alerta">
          Operación #{operacion.id}: {operacion.clase} por {operacion.importe} COP.
          <ResumenOperacion operacion={operacion} />
          <button type="button" className="peligro" onClick={reversar}>Reversar operación #{operacion.id}</button>
        </div>
      )}
      {reverso && (
        <div role="status" className="alerta alerta-exito">
          Reverso #{reverso.id} registrado para la operación #{reverso.operacionOriginalId}.
          <ResumenOperacion operacion={reverso} />
        </div>
      )}
    </section>
  );
}
