import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { api } from '../../api/cliente';
import type { Cliente, Cuenta, Pagina } from '../../api/tipos';
import { MensajeError } from '../../componentes/MensajeError';
import { Paginador } from '../../componentes/Paginador';
import { CuentasDeCliente } from '../cuentas/CuentasDeCliente';
import { FormularioCliente } from './FormularioCliente';

interface Props {
  onVerEstado: (cuenta: Cuenta) => void;
}

/** Gestión de clientes (alta, consulta, edición, eliminación) y acceso a sus cuentas. */
export function PaginaClientes({ onVerEstado }: Props) {
  const [pagina, setPagina] = useState<Pagina<Cliente> | null>(null);
  const [page, setPage] = useState(0);
  const [filtro, setFiltro] = useState('');
  const [filtroAplicado, setFiltroAplicado] = useState('');
  const [editando, setEditando] = useState<Cliente | null>(null);
  const [seleccionado, setSeleccionado] = useState<Cliente | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  const cargar = useCallback(async () => {
    try {
      setPagina(await api.listarClientes(page, filtroAplicado || undefined));
    } catch (err) {
      setError(err);
    }
  }, [page, filtroAplicado]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  function buscar(e: FormEvent) {
    e.preventDefault();
    setPage(0);
    setFiltroAplicado(filtro.trim());
  }

  async function eliminar(cliente: Cliente) {
    if (!window.confirm(`¿Eliminar a ${cliente.nombres} ${cliente.apellido}?`)) return;
    setError(null);
    setAviso(null);
    try {
      await api.eliminarCliente(cliente.id);
      if (seleccionado?.id === cliente.id) setSeleccionado(null);
      setAviso('Cliente eliminado.');
      await cargar();
    } catch (err) {
      setError(err);
    }
  }

  return (
    <div className="pila">
      <section className="tarjeta">
        <h2>Registrar cliente</h2>
        <FormularioCliente
          textoBoton="Registrar"
          onGuardar={async (datos) => {
            const creado = await api.crearCliente(datos);
            setAviso(`Cliente ${creado.nombres} ${creado.apellido} registrado.`);
            setSeleccionado(creado);
            await cargar();
          }}
        />
      </section>

      <section className="tarjeta">
        <h2>Clientes</h2>
        <form className="acciones" onSubmit={buscar}>
          <input
            aria-label="Buscar por número de identificación"
            placeholder="Número de identificación"
            value={filtro}
            onChange={(e) => setFiltro(e.target.value)}
          />
          <button type="submit">Buscar</button>
        </form>
        {aviso && <div role="status" className="alerta alerta-exito">{aviso}</div>}
        <MensajeError error={error} />
        {pagina && pagina.contenido.length === 0 && <p className="nota">No hay clientes registrados.</p>}
        {pagina && pagina.contenido.length > 0 && (
          <table>
            <thead>
              <tr><th>Identificación</th><th>Nombre</th><th>Correo</th><th>Nacimiento</th><th /></tr>
            </thead>
            <tbody>
              {pagina.contenido.map((c) => (
                <tr key={c.id} className={seleccionado?.id === c.id ? 'seleccionada' : undefined}>
                  <td>{c.tipoIdentificacion} {c.numeroIdentificacion}</td>
                  <td>{c.nombres} {c.apellido}</td>
                  <td>{c.correo}</td>
                  <td>{c.fechaNacimiento}</td>
                  <td className="acciones-fila">
                    <button type="button" onClick={() => setSeleccionado(c)}>Cuentas</button>
                    <button type="button" onClick={() => setEditando(c)}>Editar</button>
                    <button type="button" className="peligro" onClick={() => eliminar(c)}>Eliminar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {pagina && (
          <Paginador
            page={pagina.page}
            totalPaginas={pagina.totalPaginas}
            totalElementos={pagina.totalElementos}
            onCambiar={setPage}
          />
        )}
      </section>

      {editando && (
        <section className="tarjeta">
          <h2>Editar cliente</h2>
          <FormularioCliente
            key={editando.id}
            inicial={editando}
            textoBoton="Guardar cambios"
            onCancelar={() => setEditando(null)}
            onGuardar={async (datos) => {
              const actualizado = await api.actualizarCliente(editando.id, datos);
              setEditando(null);
              if (seleccionado?.id === actualizado.id) setSeleccionado(actualizado);
              setAviso('Cliente actualizado.');
              await cargar();
            }}
          />
        </section>
      )}

      {seleccionado && <CuentasDeCliente key={seleccionado.id} cliente={seleccionado} onVerEstado={onVerEstado} />}
    </div>
  );
}
