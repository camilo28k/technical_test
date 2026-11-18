package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.operacion;

import java.util.List;

import org.springframework.stereotype.Component;

import com.entidadfinanciera.backend.application.port.out.OperacionRepositoryPort;
import com.entidadfinanciera.backend.domain.model.Movimiento;
import com.entidadfinanciera.backend.domain.model.Operacion;

/** Inserta operaciones y movimientos. No ofrece actualización ni borrado (DA-01). */
@Component
class OperacionPersistenceAdapter implements OperacionRepositoryPort {

	private final OperacionJpaRepository operaciones;
	private final MovimientoJpaRepository movimientos;

	OperacionPersistenceAdapter(OperacionJpaRepository operaciones, MovimientoJpaRepository movimientos) {
		this.operaciones = operaciones;
		this.movimientos = movimientos;
	}

	@Override
	public Operacion registrar(Operacion operacion) {
		OperacionEntity fila = operaciones.save(new OperacionEntity(operacion.clase(), operacion.importe(),
				operacion.fecha(), operacion.originalId()));
		List<Movimiento> conId = operacion.movimientos()
			.stream()
			.map(m -> m.conId(movimientos.save(new MovimientoEntity(fila.getId(), m.cuentaId(), m.sentido())).getId()))
			.toList();
		operaciones.flush();
		return operacion.conIds(fila.getId(), conId);
	}

}
