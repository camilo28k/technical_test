package com.entidadfinanciera.backend.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import com.entidadfinanciera.backend.application.port.in.RegistrarOperacionesUseCase;
import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.OperacionRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.CuentaNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.MismaCuentaException;
import com.entidadfinanciera.backend.domain.exception.OperacionNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.OperacionYaReversadaException;
import com.entidadfinanciera.backend.domain.exception.ReversoNoReversableException;
import com.entidadfinanciera.backend.domain.model.ClaseOperacion;
import com.entidadfinanciera.backend.domain.model.Movimiento;
import com.entidadfinanciera.backend.domain.model.Sentido;
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

	/**
	 * Bloquea ambas cuentas en orden ascendente de identificador, sin importar cuál es el
	 * origen, para que dos transferencias cruzadas no se interbloqueen (documento 03). Estados y
	 * fondos se comprueban después de adquirir los bloqueos.
	 */
	@Override
	public Operacion transferir(String numeroCuentaOrigen, String numeroCuentaDestino, BigDecimal importe) {
		BigDecimal valor = Operacion.validarImporte(importe);
		String numeroOrigen = limpiar(numeroCuentaOrigen);
		String numeroDestino = limpiar(numeroCuentaDestino);
		if (numeroOrigen.equals(numeroDestino)) {
			throw new MismaCuentaException(numeroOrigen);
		}
		Long idOrigen = existente(numeroOrigen).getId();
		Long idDestino = existente(numeroDestino).getId();

		Cuenta primera = bloquearPorId(Math.min(idOrigen, idDestino));
		Cuenta segunda = bloquearPorId(Math.max(idOrigen, idDestino));
		Cuenta origen = primera.getId().equals(idOrigen) ? primera : segunda;
		Cuenta destino = origen == primera ? segunda : primera;

		origen.debitar(valor, reloj);
		destino.acreditar(valor, reloj);
		Cuenta origenGuardado = cuentas.guardar(origen);
		Cuenta destinoGuardado = cuentas.guardar(destino);
		return operaciones.registrar(Operacion.transferencia(valor, origenGuardado, destinoGuardado, reloj));
	}

	/**
	 * Reverso total: bloquea las cuentas del original en orden de id y, ya con los bloqueos,
	 * vuelve a comprobar que no exista otro reverso. Dos solicitudes simultáneas sobre el mismo
	 * original se serializan en esos bloqueos; la segunda ve el reverso confirmado y se rechaza.
	 * La restricción única en base de datos es la última defensa.
	 */
	@Override
	public Operacion reversar(Long operacionId) {
		Operacion original = operaciones.buscarPorId(operacionId)
			.orElseThrow(() -> new OperacionNoEncontradaException(operacionId));
		if (original.clase() == ClaseOperacion.REVERSO) {
			throw new ReversoNoReversableException(operacionId);
		}
		exigirSinReverso(operacionId);

		Map<Long, Cuenta> bloqueadas = new LinkedHashMap<>();
		original.movimientos()
			.stream()
			.map(Movimiento::cuentaId)
			.sorted()
			.forEach(id -> bloqueadas.put(id, bloquearPorId(id)));
		exigirSinReverso(operacionId);

		for (Movimiento m : original.movimientos()) {
			Cuenta cuenta = bloqueadas.get(m.cuentaId());
			if (m.sentido() == Sentido.CREDITO) {
				cuenta.debitar(original.importe(), reloj);
			}
			else {
				cuenta.acreditar(original.importe(), reloj);
			}
		}
		bloqueadas.replaceAll((id, cuenta) -> cuentas.guardar(cuenta));
		return operaciones.registrar(Operacion.reverso(original, bloqueadas, reloj));
	}

	private void exigirSinReverso(Long operacionId) {
		if (operaciones.existeReversoDe(operacionId)) {
			throw new OperacionYaReversadaException(operacionId);
		}
	}

	private Cuenta bloquear(String numero) {
		String limpio = limpiar(numero);
		return cuentas.buscarPorNumeroParaActualizar(limpio)
			.orElseThrow(() -> CuentaNoEncontradaException.porNumero(limpio));
	}

	private Cuenta existente(String numero) {
		return cuentas.buscarPorNumero(numero).orElseThrow(() -> CuentaNoEncontradaException.porNumero(numero));
	}

	private Cuenta bloquearPorId(Long id) {
		return cuentas.buscarPorIdParaActualizar(id).orElseThrow(() -> CuentaNoEncontradaException.porId(id));
	}

	private static String limpiar(String numero) {
		return numero == null ? "" : numero.trim();
	}

}
