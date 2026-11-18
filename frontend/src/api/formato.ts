const moneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 2 });
const fechaHora = new Intl.DateTimeFormat('es-CO', {
  dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Bogota',
});

/** Formatea un importe del contrato ("150000.00") como pesos colombianos. */
export const formatearImporte = (importe: string) => moneda.format(Number(importe));

export const formatearFechaHora = (iso: string) => fechaHora.format(new Date(iso));

/** Valida formato de importe: positivo y con hasta dos decimales. La API sigue siendo la autoridad. */
export function validarImporte(texto: string): string | null {
  const limpio = texto.trim();
  if (!/^\d+(\.\d{1,2})?$/.test(limpio)) return 'Usa solo números y hasta dos decimales (ej. 150000.00).';
  if (Number(limpio) <= 0) return 'El importe debe ser mayor que cero.';
  return null;
}

/** Normaliza a dos decimales sin pasar por número, para no perder precisión. */
export function normalizarImporte(texto: string): string {
  const [entero, decimales = ''] = texto.trim().split('.');
  return `${entero.replace(/^0+(?=\d)/, '')}.${decimales.padEnd(2, '0')}`;
}
