import { useState, type FormEvent } from 'react';
import type { DatosCliente, TipoIdentificacion } from '../../api/tipos';
import { Campo } from '../../componentes/Campo';
import { MensajeError } from '../../componentes/MensajeError';

const VACIO: DatosCliente = {
  tipoIdentificacion: 'CC',
  numeroIdentificacion: '',
  nombres: '',
  apellido: '',
  correo: '',
  fechaNacimiento: '',
};

type Errores = Partial<Record<keyof DatosCliente, string>>;

/** Validación de formato únicamente; la edad mínima y la unicidad las decide la API. */
export function validarCliente(d: DatosCliente): Errores {
  const errores: Errores = {};
  if (!d.numeroIdentificacion.trim()) errores.numeroIdentificacion = 'Es obligatorio.';
  else if (d.numeroIdentificacion.trim().length > 20) errores.numeroIdentificacion = 'Máximo 20 caracteres.';
  const longitud = (v: string) => v.trim().length >= 2 && v.trim().length <= 100;
  if (!longitud(d.nombres)) errores.nombres = 'Entre 2 y 100 caracteres.';
  if (!longitud(d.apellido)) errores.apellido = 'Entre 2 y 100 caracteres.';
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(d.correo.trim())) errores.correo = 'Formato usuario@dominio.ext.';
  if (!d.fechaNacimiento) errores.fechaNacimiento = 'Es obligatoria.';
  return errores;
}

interface Props {
  inicial?: DatosCliente;
  textoBoton: string;
  onGuardar: (datos: DatosCliente) => Promise<unknown>;
  onCancelar?: () => void;
}

export function FormularioCliente({ inicial = VACIO, textoBoton, onGuardar, onCancelar }: Props) {
  const [datos, setDatos] = useState<DatosCliente>(inicial);
  const [errores, setErrores] = useState<Errores>({});
  const [errorApi, setErrorApi] = useState<unknown>(null);
  const [enviando, setEnviando] = useState(false);

  const cambiar = (campo: keyof DatosCliente) => (e: { target: { value: string } }) =>
    setDatos((d) => ({ ...d, [campo]: e.target.value }));

  async function enviar(e: FormEvent) {
    e.preventDefault();
    const encontrados = validarCliente(datos);
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) return;
    setEnviando(true);
    setErrorApi(null);
    try {
      await onGuardar({
        ...datos,
        numeroIdentificacion: datos.numeroIdentificacion.trim(),
        nombres: datos.nombres.trim(),
        apellido: datos.apellido.trim(),
        correo: datos.correo.trim(),
      });
      if (!onCancelar) setDatos(VACIO);
    } catch (err) {
      setErrorApi(err);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form className="formulario" onSubmit={enviar} noValidate>
      <div className="rejilla">
        <Campo etiqueta="Tipo de identificación">
          <select
            value={datos.tipoIdentificacion}
            onChange={(e) => setDatos((d) => ({ ...d, tipoIdentificacion: e.target.value as TipoIdentificacion }))}
          >
            <option value="CC">Cédula de ciudadanía</option>
            <option value="CE">Cédula de extranjería</option>
            <option value="PASAPORTE">Pasaporte</option>
          </select>
        </Campo>
        <Campo etiqueta="Número de identificación" error={errores.numeroIdentificacion}>
          <input value={datos.numeroIdentificacion} onChange={cambiar('numeroIdentificacion')} />
        </Campo>
        <Campo etiqueta="Nombres" error={errores.nombres}>
          <input value={datos.nombres} onChange={cambiar('nombres')} />
        </Campo>
        <Campo etiqueta="Apellidos" error={errores.apellido}>
          <input value={datos.apellido} onChange={cambiar('apellido')} />
        </Campo>
        <Campo etiqueta="Correo" error={errores.correo}>
          <input type="email" value={datos.correo} onChange={cambiar('correo')} />
        </Campo>
        <Campo etiqueta="Fecha de nacimiento" error={errores.fechaNacimiento}>
          <input type="date" value={datos.fechaNacimiento} onChange={cambiar('fechaNacimiento')} />
        </Campo>
      </div>
      <MensajeError error={errorApi} />
      <div className="acciones">
        <button type="submit" className="primario" disabled={enviando}>{textoBoton}</button>
        {onCancelar && <button type="button" onClick={onCancelar}>Cancelar</button>}
      </div>
    </form>
  );
}
