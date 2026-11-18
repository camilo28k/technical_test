package com.entidadfinanciera.backend.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.consulta.EstadoDeCuenta;
import com.entidadfinanciera.backend.application.port.in.ConsultarOperacionesUseCase;
import com.entidadfinanciera.backend.application.port.out.ClienteRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.HistorialQueryPort;
import com.entidadfinanciera.backend.application.port.out.OperacionRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.CuentaNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.DatoInvalidoException;
import com.entidadfinanciera.backend.domain.exception.OperacionNoEncontradaException;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.Operacion;

/**
 * Consultas de solo lectura. La coherencia entre saldo e historial la aporta el decorador,
 * que ejecuta cada consulta en una transacción de lectura repetible.
 */
public class ConsultaService implements ConsultarOperacionesUseCase {

	private final CuentaRepositoryPort cuentas;
	private final ClienteRepositoryPort clientes;
	private final OperacionRepositoryPort operaciones;
	private final HistorialQueryPort historial;
	private final Clock reloj;

	public ConsultaService(CuentaRepositoryPort cuentas, ClienteRepositoryPort clientes,
			OperacionRepositoryPort operaciones, HistorialQueryPort historial, Clock reloj) {
		this.cuentas = cuentas;
		this.clientes = clientes;
		this.operaciones = operaciones;
		this.historial = historial;
		this.reloj = reloj;
	}

	@Override
	public Operacion consultarOperacion(Long id) {
		return operaciones.buscarPorId(id).orElseThrow(() -> new OperacionNoEncontradaException(id));
	}

	@Override
	public EstadoDeCuenta estadoDeCuenta(Long cuentaId, Optional<LocalDate> desde, Optional<LocalDate> hasta,
			Paginacion paginacion) {
		if (desde.isPresent() && hasta.isPresent() && desde.get().isAfter(hasta.get())) {
			throw new DatoInvalidoException("desde", "no puede ser posterior a hasta");
		}
		Cuenta cuenta = cuentas.buscarPorId(cuentaId).orElseThrow(() -> CuentaNoEncontradaException.porId(cuentaId));
		String titular = clientes.buscarPorId(cuenta.getClienteId())
			.map(c -> c.getNombres() + " " + c.getApellido())
			.orElse(null);
		ZoneId zona = reloj.getZone();
		return new EstadoDeCuenta(cuenta, titular, OffsetDateTime.now(reloj),
				historial.movimientos(cuentaId, desde.map(d -> d.atStartOfDay(zona).toOffsetDateTime()),
						hasta.map(h -> h.plusDays(1).atStartOfDay(zona).toOffsetDateTime()), paginacion));
	}

}
