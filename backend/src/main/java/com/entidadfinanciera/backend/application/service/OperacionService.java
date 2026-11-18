package com.entidadfinanciera.backend.application.service;

import java.math.BigDecimal;
import java.time.Clock;

import com.entidadfinanciera.backend.application.port.in.RegistrarOperacionesUseCase;
import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.OperacionRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.CuentaNoEncontradaException;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.Operacion;

/**
 * Registra operaciones financieras. Bloquea la cuenta antes de leer su saldo, aplica la regla
 * del dominio y escribe saldo e historial en la misma transacción, que aporta el decorador
 * (ADR-002). Así, dos retiros concurrentes sobre la misma cuenta se serializan.
 */
public class OperacionService implements RegistrarOperacionesUseCase {

	private final CuentaRepositoryPort cuentas;
	private final OperacionRepositoryPort operaciones;
	private final Clock reloj;

	public OperacionService(CuentaRepositoryPort cuentas, OperacionRepositoryPort operaciones, Clock reloj) {
		this.cuentas = cuentas;
		this.operaciones = operaciones;
		this.reloj = reloj;
	}

	@Override
	public Operacion consignar(String numeroCuenta, BigDecimal importe) {
		BigDecimal valor = Operacion.validarImporte(importe);
		Cuenta cuenta = bloquear(numeroCuenta);
		cuenta.acreditar(valor, reloj);
		return operaciones.registrar(Operacion.consignacion(valor, cuentas.guardar(cuenta), reloj));
	}

	@Override
	public Operacion retirar(String numeroCuenta, BigDecimal importe) {
		BigDecimal valor = Operacion.validarImporte(importe);
		Cuenta cuenta = bloquear(numeroCuenta);
		cuenta.debitar(valor, reloj);
		return operaciones.registrar(Operacion.retiro(valor, cuentas.guardar(cuenta), reloj));
	}

	private Cuenta bloquear(String numero) {
		String limpio = numero == null ? "" : numero.trim();
		return cuentas.buscarPorNumeroParaActualizar(limpio)
			.orElseThrow(() -> CuentaNoEncontradaException.porNumero(limpio));
	}

}
