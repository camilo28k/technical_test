package com.entidadfinanciera.backend.domain.model;

import java.time.LocalDate;

/** Datos editables de un cliente, comunes al registro y a la actualización. */
public record DatosCliente(
		TipoIdentificacion tipoIdentificacion,
		String numeroIdentificacion,
		String nombres,
		String apellido,
		String correo,
		LocalDate fechaNacimiento) {
}
