package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.operacion;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.entidadfinanciera.backend.application.port.out.OperacionRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.OperacionYaReversadaException;
import com.entidadfinanciera.backend.domain.model.Movimiento;
import com.entidadfinanciera.backend.domain.model.Operacion;

/** Inserta y lee operaciones y movimientos. No ofrece actualización ni borrado (DA-01). */
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
		try {
			OperacionEntity fila = operaciones.save(new OperacionEntity(operacion.clase(), operacion.importe(),
					operacion.fecha(), operacion.originalId()));
			List<Movimiento> conId = operacion.movimientos()
				.stream()
				.map(m -> m.conId(movimientos.save(new MovimientoEntity(fila.getId(), m.cuentaId(), m.sentido())).getId()))
				.toList();
			operaciones.flush();
			return operacion.conIds(fila.getId(), conId);
		}
		catch (DataIntegrityViolationException ex) {
			// P-04: la restricción única protege contra un segundo reverso simultáneo.
			if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains("uk_operacion_original")) {
				throw new OperacionYaReversadaException(operacion.originalId());
			}
			throw ex;
		}
	}

	@Override
	public Optional<Operacion> buscarPorId(Long id) {
		return operaciones.findById(id).map(fila -> new Operacion(fila.getId(), fila.getClase(), fila.getImporte(),
				fila.getFecha(), fila.getOriginalId(), movimientos.buscarConNumeroDeCuenta(id).stream().map(par -> {
					MovimientoEntity m = (MovimientoEntity) par[0];
					return new Movimiento(m.getId(), m.getCuentaId(), (String) par[1], m.getSentido(), null);
				}).toList()));
	}

	@Override
	public boolean existeReversoDe(Long originalId) {
		return operaciones.existsByOriginalId(originalId);
	}

}
