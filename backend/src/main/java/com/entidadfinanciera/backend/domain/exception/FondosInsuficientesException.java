package com.entidadfinanciera.backend.domain.exception;

import java.math.BigDecimal;

/** RN-05 y DA-02: no hay sobregiros en ningún tipo de cuenta. */
public class FondosInsuficientesException extends ReglaDeNegocioException {

	public FondosInsuficientesException(String numero, BigDecimal importe) {
		super("FONDOS_INSUFICIENTES", "La cuenta " + numero + " no tiene fondos suficientes para debitar "
				+ importe.toPlainString() + " COP.");
	}

}
