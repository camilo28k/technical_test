package com.entidadfinanciera.backend.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.entidadfinanciera.backend.domain.exception.DatoInvalidoException;

class OperacionTest {

	@Test
	void normalizaElImporteADosDecimales() {
		assertThat(Operacion.validarImporte(new BigDecimal("150000"))).hasToString("150000.00");
		assertThat(Operacion.validarImporte(new BigDecimal("0.010"))).hasToString("0.01");
	}

	@Test
	void rechazaImportesNoPositivosOConMasDeDosDecimales() {
		assertThatThrownBy(() -> Operacion.validarImporte(BigDecimal.ZERO)).isInstanceOf(DatoInvalidoException.class);
		assertThatThrownBy(() -> Operacion.validarImporte(new BigDecimal("-5"))).isInstanceOf(DatoInvalidoException.class);
		assertThatThrownBy(() -> Operacion.validarImporte(new BigDecimal("1.001")))
			.isInstanceOf(DatoInvalidoException.class);
		assertThatThrownBy(() -> Operacion.validarImporte(null)).isInstanceOf(DatoInvalidoException.class);
	}

}
