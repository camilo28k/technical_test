package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cuenta;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de {@code POST /cuentas}. */
public record AperturaCuentaRequest(
		@NotNull(message = "es obligatorio") Long clienteId,
		@NotNull(message = "es obligatorio")
		@Pattern(regexp = "AHORROS|CORRIENTE", message = "debe ser AHORROS o CORRIENTE") String tipoCuenta) {
}
