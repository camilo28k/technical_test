// Tipos del contrato REST (docs/07-contrato-api-rest.md). Los importes viajan como texto.

export type TipoIdentificacion = 'CC' | 'CE' | 'PASAPORTE';
export type TipoCuenta = 'AHORROS' | 'CORRIENTE';
export type EstadoCuenta = 'ACTIVA' | 'INACTIVA' | 'CANCELADA';
export type ClaseOperacion = 'CONSIGNACION' | 'RETIRO' | 'TRANSFERENCIA' | 'REVERSO';
export type Sentido = 'DEBITO' | 'CREDITO';

export interface Pagina<T> {
  contenido: T[];
  page: number;
  size: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface DatosCliente {
  tipoIdentificacion: TipoIdentificacion;
  numeroIdentificacion: string;
  nombres: string;
  apellido: string;
  correo: string;
  fechaNacimiento: string;
}

export interface Cliente extends DatosCliente {
  id: number;
  fechaCreacion: string;
  fechaModificacion: string | null;
}

export interface Cuenta {
  id: number;
  clienteId: number;
  tipoCuenta: TipoCuenta;
  numero: string;
  estado: EstadoCuenta;
  saldo: string;
  saldoDisponible: string;
  exentaGmf: boolean;
  fechaCreacion: string;
  fechaModificacion: string | null;
}

export interface Movimiento {
  id: number;
  numeroCuenta: string;
  sentido: Sentido;
  saldoResultante: string | null;
}

export interface Operacion {
  id: number;
  clase: ClaseOperacion;
  importe: string;
  fecha: string;
  operacionOriginalId: number | null;
  movimientos: Movimiento[];
}

export interface LineaEstadoDeCuenta {
  operacionId: number;
  clase: ClaseOperacion;
  sentido: Sentido;
  importe: string;
  fecha: string;
  contraparte: string | null;
  reversada: boolean;
}

export interface EstadoDeCuenta {
  cuenta: { id: number; numero: string; tipoCuenta: TipoCuenta; estado: EstadoCuenta; titular: string };
  saldo: string;
  saldoDisponible: string;
  fechaConsulta: string;
  movimientos: Pagina<LineaEstadoDeCuenta>;
}

export interface DetalleError {
  campo: string;
  mensaje: string;
}

export interface CuerpoError {
  codigo: string;
  mensaje: string;
  fecha?: string;
  ruta?: string;
  detalles?: DetalleError[];
}
