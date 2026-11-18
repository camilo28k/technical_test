package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.operacion;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.entidadfinanciera.backend.domain.model.Operacion;

/** Operación con sus movimientos. Los importes viajan como texto con dos decimales (P-03). */
public record OperacionResponse(Long id, String clase, String importe, OffsetDateTime fecha, Long operacionOriginalId,
		List<MovimientoResponse> movimientos) {

	public record MovimientoResponse(Long id, String numeroCuenta, String sentido, String saldoResultante) {
	}

	static OperacionResponse desde(Operacion o, ZoneId zona) {
		return new OperacionResponse(o.id(), o.clase().name(), o.importe().toPlainString(),
				o.fecha().atZoneSameInstant(zona).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS), o.originalId(),
				o.movimientos()
					.stream()
					.map(m -> new MovimientoResponse(m.id(), m.numeroCuenta(), m.sentido().name(),
							m.saldoResultante() == null ? null : m.saldoResultante().toPlainString()))
					.toList());
	}

}
