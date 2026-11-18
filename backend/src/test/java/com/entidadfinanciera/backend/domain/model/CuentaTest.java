package com.entidadfinanciera.backend.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.entidadfinanciera.backend.domain.exception.CuentaCanceladaException;
import com.entidadfinanciera.backend.domain.exception.SaldoDistintoDeCeroException;

class CuentaTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	private static final Clock APERTURA = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), BOGOTA);
	private static final Clock DESPUES = Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), BOGOTA);

	private static Cuenta conEstado(EstadoCuenta estado, String saldo) {
		return Cuenta.reconstruir(1L, 7L, TipoCuenta.AHORROS, "5300000001", estado, new BigDecimal(saldo), false,
				OffsetDateTime.now(APERTURA), null);
	}

	@Test
	void abreActivaConSaldoCeroSinGmfYFechaAutomatica() {
		Cuenta cuenta = Cuenta.abrir(7L, TipoCuenta.CORRIENTE, "3300000004", APERTURA);

		assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
		assertThat(cuenta.getSaldo()).isEqualByComparingTo("0").hasToString("0.00");
		assertThat(cuenta.getSaldoDisponible()).isEqualTo(cuenta.getSaldo());
		assertThat(cuenta.isExentaGmf()).isFalse();
		assertThat(cuenta.getFechaCreacion()).isEqualTo(OffsetDateTime.now(APERTURA));
		assertThat(cuenta.getFechaModificacion()).isNull();
	}

	@Test
	void rechazaNumeroConPrefijoQueNoCorrespondeAlTipo() {
		assertThatThrownBy(() -> Cuenta.abrir(7L, TipoCuenta.AHORROS, "3300000004", APERTURA))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Cuenta.abrir(7L, TipoCuenta.AHORROS, "530000001", APERTURA))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void inactivaYReactivaMarcandoLaModificacion() {
		Cuenta cuenta = conEstado(EstadoCuenta.ACTIVA, "0");

		cuenta.inactivar(DESPUES);
		assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.INACTIVA);
		assertThat(cuenta.getFechaModificacion()).isEqualTo(OffsetDateTime.now(DESPUES));

		cuenta.activar(DESPUES);
		assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
	}

	@Test
	void activarUnaCuentaActivaEsIdempotente() {
		Cuenta cuenta = conEstado(EstadoCuenta.ACTIVA, "0");

		cuenta.activar(DESPUES);

		assertThat(cuenta.getFechaModificacion()).isNull();
	}

	@Test
	void cancelaConSaldoCeroDesdeActivaOInactiva() {
		Cuenta activa = conEstado(EstadoCuenta.ACTIVA, "0");
		Cuenta inactiva = conEstado(EstadoCuenta.INACTIVA, "0.00");

		activa.cancelar(DESPUES);
		inactiva.cancelar(DESPUES);

		assertThat(activa.getEstado()).isEqualTo(EstadoCuenta.CANCELADA);
		assertThat(inactiva.getEstado()).isEqualTo(EstadoCuenta.CANCELADA);
	}

	@Test
	void noCancelaConSaldoPositivoYNoCambiaNada() {
		Cuenta cuenta = conEstado(EstadoCuenta.ACTIVA, "0.01");

		assertThatThrownBy(() -> cuenta.cancelar(DESPUES)).isInstanceOf(SaldoDistintoDeCeroException.class)
			.hasMessageContaining("0.01");
		assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
		assertThat(cuenta.getFechaModificacion()).isNull();
	}

	@Test
	void unaCuentaCanceladaNoAdmiteCambios() {
		Cuenta cuenta = conEstado(EstadoCuenta.CANCELADA, "0");

		assertThatThrownBy(() -> cuenta.activar(DESPUES)).isInstanceOf(CuentaCanceladaException.class);
		assertThatThrownBy(() -> cuenta.inactivar(DESPUES)).isInstanceOf(CuentaCanceladaException.class);
		assertThatThrownBy(() -> cuenta.cancelar(DESPUES)).isInstanceOf(CuentaCanceladaException.class);
		assertThatThrownBy(() -> cuenta.cambiarExencionGmf(true, DESPUES))
			.isInstanceOf(CuentaCanceladaException.class);
		assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.CANCELADA);
	}

	@Test
	void cambiarGmfConservaElValorYActualizaLaModificacion() {
		Cuenta cuenta = conEstado(EstadoCuenta.INACTIVA, "0");

		cuenta.cambiarExencionGmf(true, DESPUES);

		assertThat(cuenta.isExentaGmf()).isTrue();
		assertThat(cuenta.getFechaModificacion()).isEqualTo(OffsetDateTime.now(DESPUES));
	}

}
