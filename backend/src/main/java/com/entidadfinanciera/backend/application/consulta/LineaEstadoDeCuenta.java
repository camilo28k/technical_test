package com.entidadfinanciera.backend.application.consulta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.entidadfinanciera.backend.domain.model.ClaseOperacion;
import com.entidadfinanciera.backend.domain.model.Sentido;

/**
 * Movimiento tal como aparece en el estado de cuenta (contrato REST, sección 5).
 * {@code contraparte} es el número de la otra cuenta de una transferencia o de su reverso.
 */
public record LineaEstadoDeCuenta(Long movimientoId, Long operacionId, ClaseOperacion clase, Sentido sentido,
		BigDecimal importe, OffsetDateTime fecha, String contraparte, boolean reversada) {
}
