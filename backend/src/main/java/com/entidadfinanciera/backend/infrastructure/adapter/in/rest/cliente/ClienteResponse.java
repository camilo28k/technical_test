package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cliente;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import com.entidadfinanciera.backend.domain.model.Cliente;

/** Representación pública de un cliente. Las fechas de auditoría se expresan en hora de Colombia. */
public record ClienteResponse(
		Long id,
		String tipoIdentificacion,
		String numeroIdentificacion,
		String nombres,
		String apellido,
		String correo,
		LocalDate fechaNacimiento,
		OffsetDateTime fechaCreacion,
		OffsetDateTime fechaModificacion) {

	static ClienteResponse desde(Cliente c, ZoneId zona) {
		return new ClienteResponse(c.getId(), c.getTipoIdentificacion().name(), c.getNumeroIdentificacion(),
				c.getNombres(), c.getApellido(), c.getCorreo(), c.getFechaNacimiento(), enZona(c.getFechaCreacion(), zona),
				enZona(c.getFechaModificacion(), zona));
	}

	private static OffsetDateTime enZona(OffsetDateTime fecha, ZoneId zona) {
		return fecha == null ? null : fecha.atZoneSameInstant(zona).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS);
	}

}
