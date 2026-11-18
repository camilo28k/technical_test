package com.entidadfinanciera.backend.application.port.in;

import java.math.BigDecimal;

import com.entidadfinanciera.backend.domain.model.Operacion;

/** Puerto de entrada de las operaciones financieras. Cada operación es atómica (ADR-002). */
public interface RegistrarOperacionesUseCase {

	Operacion consignar(String numeroCuenta, BigDecimal importe);

	Operacion retirar(String numeroCuenta, BigDecimal importe);

	/** Reverso total de una operación (P-04). */
	Operacion reversar(Long operacionId);

	Operacion transferir(String numeroCuentaOrigen, String numeroCuentaDestino, BigDecimal importe);

}
