package com.entidadfinanciera.backend.domain.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.Objects;
import java.util.regex.Pattern;

import com.entidadfinanciera.backend.domain.exception.ClienteMenorDeEdadException;
import com.entidadfinanciera.backend.domain.exception.DatoInvalidoException;

/**
 * Cliente de la entidad financiera. Garantiza sus invariantes al registrarse y al
 * actualizarse: mayoría de edad en Colombia, formato de correo, longitud de nombres y
 * fechas de auditoría automáticas.
 */
public final class Cliente {

	public static final int EDAD_MINIMA = 18;

	private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	private final Long id;
	private TipoIdentificacion tipoIdentificacion;
	private String numeroIdentificacion;
	private String nombres;
	private String apellido;
	private String correo;
	private LocalDate fechaNacimiento;
	private final OffsetDateTime fechaCreacion;
	private OffsetDateTime fechaModificacion;

	private Cliente(Long id, DatosCliente datos, OffsetDateTime fechaCreacion, OffsetDateTime fechaModificacion) {
		this.id = id;
		asignar(datos);
		this.fechaCreacion = fechaCreacion;
		this.fechaModificacion = fechaModificacion;
	}

	/** RF-02, RN-01, RN-03: crea un cliente válido con fecha de creación automática. */
	public static Cliente registrar(DatosCliente datos, Clock reloj) {
		validar(datos, reloj);
		return new Cliente(null, normalizar(datos), OffsetDateTime.now(reloj), null);
	}

	/** Reconstruye un cliente ya persistido, sin volver a aplicar reglas de alta. */
	public static Cliente reconstruir(Long id, DatosCliente datos, OffsetDateTime fechaCreacion,
			OffsetDateTime fechaModificacion) {
		return new Cliente(id, datos, fechaCreacion, fechaModificacion);
	}

	/** RN-01, RN-03: actualiza los datos y registra la fecha de la última modificación. */
	public void actualizar(DatosCliente datos, Clock reloj) {
		validar(datos, reloj);
		asignar(normalizar(datos));
		this.fechaModificacion = OffsetDateTime.now(reloj);
	}

	public boolean tieneIdentificacion(TipoIdentificacion tipo, String numero) {
		return tipoIdentificacion == tipo && numeroIdentificacion.equals(numero == null ? null : numero.trim());
	}

	private static void validar(DatosCliente datos, Clock reloj) {
		Objects.requireNonNull(datos, "datos");
		requerido("tipoIdentificacion", datos.tipoIdentificacion());
		texto("numeroIdentificacion", datos.numeroIdentificacion(), 1, 20);
		texto("nombres", datos.nombres(), 2, 100);
		texto("apellido", datos.apellido(), 2, 100);
		texto("correo", datos.correo(), 3, 254);
		if (!CORREO.matcher(datos.correo().trim()).matches()) {
			throw new DatoInvalidoException("correo", "debe tener el formato usuario@dominio.ext");
		}
		requerido("fechaNacimiento", datos.fechaNacimiento());
		LocalDate hoy = LocalDate.now(reloj);
		if (Period.between(datos.fechaNacimiento(), hoy).getYears() < EDAD_MINIMA) {
			throw new ClienteMenorDeEdadException();
		}
	}

	private static void requerido(String campo, Object valor) {
		if (valor == null) {
			throw new DatoInvalidoException(campo, "es obligatorio");
		}
	}

	private static void texto(String campo, String valor, int minimo, int maximo) {
		requerido(campo, valor);
		int longitud = valor.trim().length();
		if (longitud < minimo || longitud > maximo) {
			throw new DatoInvalidoException(campo, "debe tener entre " + minimo + " y " + maximo + " caracteres");
		}
	}

	private static DatosCliente normalizar(DatosCliente d) {
		return new DatosCliente(d.tipoIdentificacion(), d.numeroIdentificacion().trim(), d.nombres().trim(),
				d.apellido().trim(), d.correo().trim().toLowerCase(), d.fechaNacimiento());
	}

	private void asignar(DatosCliente datos) {
		this.tipoIdentificacion = datos.tipoIdentificacion();
		this.numeroIdentificacion = datos.numeroIdentificacion();
		this.nombres = datos.nombres();
		this.apellido = datos.apellido();
		this.correo = datos.correo();
		this.fechaNacimiento = datos.fechaNacimiento();
	}

	public Long getId() {
		return id;
	}

	public TipoIdentificacion getTipoIdentificacion() {
		return tipoIdentificacion;
	}

	public String getNumeroIdentificacion() {
		return numeroIdentificacion;
	}

	public String getNombres() {
		return nombres;
	}

	public String getApellido() {
		return apellido;
	}

	public String getCorreo() {
		return correo;
	}

	public LocalDate getFechaNacimiento() {
		return fechaNacimiento;
	}

	public OffsetDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public OffsetDateTime getFechaModificacion() {
		return fechaModificacion;
	}

}
