package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cuenta;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import com.entidadfinanciera.backend.domain.model.Cuenta;

/** Representación pública de una cuenta. Los importes viajan como texto con dos decimales (P-03). */
public record CuentaResponse(
		Long id,
		Long clienteId,
		String tipoCuenta,
		String numero,
		String estado,
		String saldo,
		String saldoDisponible,
		boolean exentaGmf,
		OffsetDateTime fechaCreacion,
		OffsetDateTime fechaModificacion) {

	static CuentaResponse desde(Cuenta c, ZoneId zona) {
		return new CuentaResponse(c.getId(), c.getClienteId(), c.getTipo().name(), c.getNumero(), c.getEstado().name(),
				c.getSaldo().toPlainString(), c.getSaldoDisponible().toPlainString(), c.isExentaGmf(),
				enZona(c.getFechaCreacion(), zona), enZona(c.getFechaModificacion(), zona));
	}

	private static OffsetDateTime enZona(OffsetDateTime fecha, ZoneId zona) {
		return fecha == null ? null : fecha.atZoneSameInstant(zona).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS);
	}

}
