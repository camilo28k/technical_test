package com.entidadfinanciera.backend.domain.exception;

import java.math.BigDecimal;

/** RN-08: solo se cancelan cuentas con saldo cero. */
public class SaldoDistintoDeCeroException extends ReglaDeNegocioException {

	public SaldoDistintoDeCeroException(String numero, BigDecimal saldo) {
		super("SALDO_DISTINTO_DE_CERO",
				"La cuenta " + numero + " tiene saldo " + saldo.toPlainString() + " COP; debe ser cero para cancelarla.");
	}

}
