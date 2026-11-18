import { vi } from 'vitest';

type Respuesta = { status: number; body?: unknown };

/** Sustituye fetch por respuestas en orden y devuelve el mock para inspeccionar las llamadas. */
export function simularFetch(...respuestas: Respuesta[]) {
  const mock = vi.fn();
  respuestas.forEach(({ status, body }) => {
    mock.mockResolvedValueOnce(new Response(body === undefined ? null : JSON.stringify(body), { status }));
  });
  vi.stubGlobal('fetch', mock);
  return mock;
}

export const cuerpoEnviado = (mock: ReturnType<typeof vi.fn>, llamada = 0) =>
  JSON.parse(mock.mock.calls[llamada][1].body as string);
