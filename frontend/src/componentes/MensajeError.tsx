import { ApiError } from '../api/cliente';

interface Props {
  error: unknown;
}

/** Presenta el cuerpo de error del contrato: código, mensaje y detalles de validación. */
export function MensajeError({ error }: Props) {
  if (!error) return null;
  const cuerpo = error instanceof ApiError
    ? error.cuerpo
    : { codigo: 'ERROR', mensaje: error instanceof Error ? error.message : String(error), detalles: [] };
  return (
    <div role="alert" className="alerta alerta-error">
      <strong>{cuerpo.mensaje}</strong>
      <span className="codigo">{cuerpo.codigo}</span>
      {cuerpo.detalles && cuerpo.detalles.length > 0 && (
        <ul>
          {cuerpo.detalles.map((d) => (
            <li key={d.campo + d.mensaje}>
              <code>{d.campo}</code>: {d.mensaje}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
