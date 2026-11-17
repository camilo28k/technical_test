package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cliente;

import java.time.LocalDate;

import com.entidadfinanciera.backend.domain.model.DatosCliente;
import com.entidadfinanciera.backend.domain.model.TipoIdentificacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de alta y actualización de un cliente (contrato REST, sección 2). */
public record ClienteRequest(
		@NotBlank @Pattern(regexp = "CC|CE|PASAPORTE", message = "debe ser CC, CE o PASAPORTE") String tipoIdentificacion,
		@NotBlank @Size(max = 20) String numeroIdentificacion,
		@NotBlank @Size(min = 2, max = 100) String nombres,
		@NotBlank @Size(min = 2, max = 100) String apellido,
		@NotBlank @Size(max = 254) @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "debe tener el formato usuario@dominio.ext") String correo,
		@NotNull @Past LocalDate fechaNacimiento) {

	DatosCliente aDatos() {
		return new DatosCliente(TipoIdentificacion.valueOf(tipoIdentificacion), numeroIdentificacion, nombres, apellido,
				correo, fechaNacimiento);
	}

}
