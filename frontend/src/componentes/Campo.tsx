import type { ReactNode } from 'react';

interface Props {
  etiqueta: string;
  error?: string | null;
  children: ReactNode;
}

/** Etiqueta + control + mensaje de validación local. */
export function Campo({ etiqueta, error, children }: Props) {
  return (
    <label className="campo">
      <span>{etiqueta}</span>
      {children}
      {error && <small className="error-campo">{error}</small>}
    </label>
  );
}
