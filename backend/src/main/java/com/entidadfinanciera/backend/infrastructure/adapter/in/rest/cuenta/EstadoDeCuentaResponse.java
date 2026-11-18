package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cuenta;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import com.entidadfinanciera.backend.application.consulta.EstadoDeCuenta;
import com.entidadfinanciera.backend.application.consulta.LineaEstadoDeCuenta;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.infrastructure.adapter.in.rest.comun.PaginaResponse;

/** Estado de cuenta (contrato REST, sección 5). */
public record EstadoDeCuentaResponse(CuentaResumen cuenta, String saldo, String saldoDisponible,
		OffsetDateTime fechaConsulta, PaginaResponse<Linea> movimientos) {

	public record CuentaResumen(Long id, String numero, String tipoCuenta, String estado, String titular) {
	}

	public record Linea(Long operacionId, String clase, String sentido, String importe, OffsetDateTime fecha,
			String contraparte, boolean reversada) {
	}

	static EstadoDeCuentaResponse desde(EstadoDeCuenta estado, ZoneId zona) {
		Cuenta c = estado.cuenta();
		return new EstadoDeCuentaResponse(
				new CuentaResumen(c.getId(), c.getNumero(), c.getTipo().name(), c.getEstado().name(), estado.titular()),
				c.getSaldo().toPlainString(), c.getSaldoDisponible().toPlainString(), enZona(estado.fechaConsulta(), zona),
				PaginaResponse.desde(estado.movimientos().map(l -> linea(l, zona))));
	}

	private static Linea linea(LineaEstadoDeCuenta l, ZoneId zona) {
		return new Linea(l.operacionId(), l.clase().name(), l.sentido().name(), l.importe().toPlainString(),
				enZona(l.fecha(), zona), l.contraparte(), l.reversada());
	}

	private static OffsetDateTime enZona(OffsetDateTime fecha, ZoneId zona) {
		return fecha.atZoneSameInstant(zona).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS);
	}

}
