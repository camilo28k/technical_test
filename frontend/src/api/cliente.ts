import type {
  Cliente, Cuenta, CuerpoError, DatosCliente, EstadoDeCuenta, Operacion, Pagina, TipoCuenta,
} from './tipos';

const BASE = '/api/v1';

/** Error de la API con el cuerpo estándar del contrato (sección 6). */
export class ApiError extends Error {
  readonly estado: number;
  readonly cuerpo: CuerpoError;

  constructor(estado: number, cuerpo: CuerpoError) {
    super(cuerpo.mensaje);
    this.name = 'ApiError';
    this.estado = estado;
    this.cuerpo = cuerpo;
  }
}

async function solicitar<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  let respuesta: Response;
  try {
    respuesta = await fetch(BASE + ruta, {
      ...opciones,
      headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...opciones.headers },
    });
  } catch {
    throw new ApiError(0, { codigo: 'SIN_CONEXION', mensaje: 'No fue posible comunicarse con el servidor.' });
  }
  if (respuesta.status === 204) {
    return undefined as T;
  }
  const texto = await respuesta.text();
  const cuerpo = texto ? JSON.parse(texto) : undefined;
  if (!respuesta.ok) {
    throw new ApiError(respuesta.status, cuerpo?.codigo
      ? cuerpo
      : { codigo: 'ERROR_INTERNO', mensaje: `Error inesperado (HTTP ${respuesta.status}).` });
  }
  return cuerpo as T;
}

function consulta(parametros: Record<string, string | number | undefined>): string {
  const p = new URLSearchParams();
  Object.entries(parametros).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== '') p.set(clave, String(valor));
  });
  const texto = p.toString();
  return texto ? `?${texto}` : '';
}

const json = (cuerpo: unknown) => JSON.stringify(cuerpo);

export const api = {
  listarClientes: (page = 0, numeroIdentificacion?: string) =>
    solicitar<Pagina<Cliente>>(`/clientes${consulta({ page, size: 20, numeroIdentificacion })}`),
  crearCliente: (datos: DatosCliente) => solicitar<Cliente>('/clientes', { method: 'POST', body: json(datos) }),
  actualizarCliente: (id: number, datos: DatosCliente) =>
    solicitar<Cliente>(`/clientes/${id}`, { method: 'PUT', body: json(datos) }),
  eliminarCliente: (id: number) => solicitar<void>(`/clientes/${id}`, { method: 'DELETE' }),
  cuentasDeCliente: (id: number) => solicitar<Cuenta[]>(`/clientes/${id}/cuentas`),

  abrirCuenta: (clienteId: number, tipoCuenta: TipoCuenta) =>
    solicitar<Cuenta>('/cuentas', { method: 'POST', body: json({ clienteId, tipoCuenta }) }),
  cambiarExencionGmf: (id: number, exentaGmf: boolean) =>
    solicitar<Cuenta>(`/cuentas/${id}`, { method: 'PATCH', body: json({ exentaGmf }) }),
  cambiarEstadoCuenta: (id: number, accion: 'activar' | 'inactivar' | 'cancelar') =>
    solicitar<Cuenta>(`/cuentas/${id}/${accion}`, { method: 'POST' }),
  buscarCuentaPorNumero: (numero: string) => solicitar<Cuenta>(`/cuentas/numero/${encodeURIComponent(numero)}`),
  estadoDeCuenta: (id: number, filtros: { desde?: string; hasta?: string; page?: number }) =>
    solicitar<EstadoDeCuenta>(`/cuentas/${id}/estado-de-cuenta${consulta({ ...filtros, size: 20 })}`),

  consignar: (numeroCuenta: string, importe: string) =>
    solicitar<Operacion>('/operaciones/consignaciones', { method: 'POST', body: json({ numeroCuenta, importe }) }),
  retirar: (numeroCuenta: string, importe: string) =>
    solicitar<Operacion>('/operaciones/retiros', { method: 'POST', body: json({ numeroCuenta, importe }) }),
  transferir: (numeroCuentaOrigen: string, numeroCuentaDestino: string, importe: string) =>
    solicitar<Operacion>('/operaciones/transferencias', {
      method: 'POST', body: json({ numeroCuentaOrigen, numeroCuentaDestino, importe }),
    }),
  reversar: (operacionId: number) => solicitar<Operacion>(`/operaciones/${operacionId}/reverso`, { method: 'POST' }),
  consultarOperacion: (id: number) => solicitar<Operacion>(`/operaciones/${id}`),
};
