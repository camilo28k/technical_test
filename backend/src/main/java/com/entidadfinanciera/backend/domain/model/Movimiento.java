package com.entidadfinanciera.backend.domain.model;

import java.math.BigDecimal;

/**
 * Efecto de una operación sobre una cuenta. {@code saldoResultante} es el saldo tras confirmar
 * la operación; se informa en la respuesta y no se almacena (contrato REST, sección 4).
 */
public record Movimiento(Long id, Long cuentaId, String numeroCuenta, Sentido sentido, BigDecimal saldoResultante) {

	public Movimiento conId(Long nuevoId) {
		return new Movimiento(nuevoId, cuentaId, numeroCuenta, sentido, saldoResultante);
	}

}
