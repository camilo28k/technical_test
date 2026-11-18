package com.entidadfinanciera.backend.infrastructure.transaction;

import java.math.BigDecimal;

import org.springframework.transaction.annotation.Transactional;

import com.entidadfinanciera.backend.application.port.in.RegistrarOperacionesUseCase;
import com.entidadfinanciera.backend.domain.model.Operacion;

/**
 * Decorador transaccional de las operaciones: saldo e historial se confirman juntos o no se
 * confirman (ACID, ADR-002). La respuesta HTTP se emite después del commit.
 */
public class RegistrarOperacionesTransaccional implements RegistrarOperacionesUseCase {

	private final RegistrarOperacionesUseCase delegado;

	public RegistrarOperacionesTransaccional(RegistrarOperacionesUseCase delegado) {
		this.delegado = delegado;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Operacion consignar(String numeroCuenta, BigDecimal importe) {
		return delegado.consignar(numeroCuenta, importe);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Operacion retirar(String numeroCuenta, BigDecimal importe) {
		return delegado.retirar(numeroCuenta, importe);
	}

}
