import { useState } from 'react';
import type { Cuenta } from './api/tipos';
import { PaginaClientes } from './funcionalidades/clientes/PaginaClientes';
import { PaginaEstadoDeCuenta } from './funcionalidades/estado/PaginaEstadoDeCuenta';
import { FormularioOperacion } from './funcionalidades/operaciones/FormularioOperacion';
import { FormularioReverso } from './funcionalidades/operaciones/FormularioReverso';

type Seccion = 'clientes' | 'operaciones' | 'estado';

const SECCIONES: { id: Seccion; titulo: string }[] = [
  { id: 'clientes', titulo: 'Clientes y cuentas' },
  { id: 'operaciones', titulo: 'Operaciones' },
  { id: 'estado', titulo: 'Estado de cuenta' },
];

export default function App() {
  const [seccion, setSeccion] = useState<Seccion>('clientes');
  const [numeroCuenta, setNumeroCuenta] = useState('');

  function verEstado(cuenta: Cuenta) {
    setNumeroCuenta(cuenta.numero);
    setSeccion('estado');
  }

  return (
    <>
      <header className="cabecera">
        <h1>Entidad Financiera</h1>
        <nav aria-label="Secciones">
          {SECCIONES.map((s) => (
            <button
              key={s.id}
              type="button"
              aria-current={seccion === s.id ? 'page' : undefined}
              onClick={() => setSeccion(s.id)}
            >
              {s.titulo}
            </button>
          ))}
        </nav>
      </header>
      <main className="contenido">
        {seccion === 'clientes' && <PaginaClientes onVerEstado={verEstado} />}
        {seccion === 'operaciones' && (
          <div className="pila">
            <FormularioOperacion />
            <FormularioReverso />
          </div>
        )}
        {seccion === 'estado' && <PaginaEstadoDeCuenta key={numeroCuenta} numeroInicial={numeroCuenta} />}
      </main>
    </>
  );
}
