package com.entidadfinanciera.backend.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

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

	@Test
	void elReversoUsaLasMismasCuentasEImporteConSentidosOpuestos() {
		Clock reloj = Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), ZoneId.of("America/Bogota"));
		Cuenta a = Cuenta.reconstruir(10L, 1L, TipoCuenta.AHORROS, "5300000001", EstadoCuenta.ACTIVA,
				new BigDecimal("100.00"), false, OffsetDateTime.now(reloj), null);
		Cuenta b = Cuenta.reconstruir(11L, 1L, TipoCuenta.CORRIENTE, "3300000004", EstadoCuenta.ACTIVA,
				new BigDecimal("20.00"), false, OffsetDateTime.now(reloj), null);
		Operacion transferencia = new Operacion(501L, ClaseOperacion.TRANSFERENCIA, new BigDecimal("30.00"),
				OffsetDateTime.now(reloj), null, List.of(new Movimiento(1L, 10L, "5300000001", Sentido.DEBITO, null),
						new Movimiento(2L, 11L, "3300000004", Sentido.CREDITO, null)));

		Operacion reverso = Operacion.reverso(transferencia, Map.of(10L, a, 11L, b), reloj);

		assertThat(reverso.clase()).isEqualTo(ClaseOperacion.REVERSO);
		assertThat(reverso.originalId()).isEqualTo(501L);
		assertThat(reverso.importe()).isEqualByComparingTo("30.00");
		assertThat(reverso.movimientos()).extracting(m -> m.numeroCuenta() + ":" + m.sentido())
			.containsExactly("5300000001:CREDITO", "3300000004:DEBITO");
	}

}
