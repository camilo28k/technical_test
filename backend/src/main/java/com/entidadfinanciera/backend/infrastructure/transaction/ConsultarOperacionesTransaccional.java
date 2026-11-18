package com.entidadfinanciera.backend.infrastructure.transaction;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.consulta.EstadoDeCuenta;
import com.entidadfinanciera.backend.application.port.in.ConsultarOperacionesUseCase;
import com.entidadfinanciera.backend.domain.model.Operacion;

/**
 * Decorador de las consultas. {@code REPEATABLE_READ} hace que saldo, conteo e historial se
 * lean de la misma instantánea de PostgreSQL aunque haya movimientos concurrentes.
 */
public class ConsultarOperacionesTransaccional implements ConsultarOperacionesUseCase {

	private final ConsultarOperacionesUseCase delegado;

	public ConsultarOperacionesTransaccional(ConsultarOperacionesUseCase delegado) {
		this.delegado = delegado;
	}

	@Override
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public Operacion consultarOperacion(Long id) {
		return delegado.consultarOperacion(id);
	}

	@Override
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public EstadoDeCuenta estadoDeCuenta(Long cuentaId, Optional<LocalDate> desde, Optional<LocalDate> hasta,
			Paginacion paginacion) {
		return delegado.estadoDeCuenta(cuentaId, desde, hasta, paginacion);
	}

}
