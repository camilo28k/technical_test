package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.operacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code POST /operaciones/transferencias} (contrato REST, sección 4). */
public record TransferenciaRequest(
		@NotBlank(message = "es obligatorio") String numeroCuentaOrigen,
		@NotBlank(message = "es obligatorio") String numeroCuentaDestino,
		@NotNull(message = "es obligatorio")
		@DecimalMin(value = "0.01", message = "debe ser mayor que cero")
		@Digits(integer = 17, fraction = 2, message = "admite como máximo dos decimales") BigDecimal importe) {
}
