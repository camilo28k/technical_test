package com.entidadfinanciera.backend.domain.model;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import com.entidadfinanciera.backend.domain.exception.DatoInvalidoException;

/**
 * Operación financiera inmutable (DA-01) con sus movimientos. El importe y la fecha son comunes
 * a todos sus movimientos.
 */
public record Operacion(Long id, ClaseOperacion clase, BigDecimal importe, OffsetDateTime fecha, Long originalId,
		List<Movimiento> movimientos) {

	public Operacion {
		Objects.requireNonNull(clase, "clase");
		Objects.requireNonNull(fecha, "fecha");
		movimientos = List.copyOf(movimientos);
	}

	/** Consignación: un crédito en la cuenta, que ya debe tener aplicado el importe. */
	public static Operacion consignacion(BigDecimal importe, Cuenta cuenta, Clock reloj) {
		return new Operacion(null, ClaseOperacion.CONSIGNACION, importe, OffsetDateTime.now(reloj), null,
				List.of(movimiento(cuenta, Sentido.CREDITO)));
	}

	/** Retiro: un débito en la cuenta, que ya debe tener aplicado el importe. */
	public static Operacion retiro(BigDecimal importe, Cuenta cuenta, Clock reloj) {
		return new Operacion(null, ClaseOperacion.RETIRO, importe, OffsetDateTime.now(reloj), null,
				List.of(movimiento(cuenta, Sentido.DEBITO)));
	}

	private static Movimiento movimiento(Cuenta cuenta, Sentido sentido) {
		return new Movimiento(null, cuenta.getId(), cuenta.getNumero(), sentido, cuenta.getSaldo());
	}

	/**
	 * Importe en COP: positivo y con dos decimales como máximo (DA-05, P-03). Devuelve el valor
	 * normalizado a escala 2.
	 */
	public static BigDecimal validarImporte(BigDecimal importe) {
		if (importe == null || importe.signum() <= 0) {
			throw new DatoInvalidoException("importe", "debe ser mayor que cero");
		}
		if (importe.stripTrailingZeros().scale() > 2) {
			throw new DatoInvalidoException("importe", "admite como máximo dos decimales");
		}
		return importe.setScale(2);
	}

	public Operacion conIds(Long nuevoId, List<Movimiento> movimientosConId) {
		return new Operacion(nuevoId, clase, importe, fecha, originalId, movimientosConId);
	}

}
