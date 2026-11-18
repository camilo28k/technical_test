package com.entidadfinanciera.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.OperacionRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.CuentaNoActivaException;
import com.entidadfinanciera.backend.domain.exception.CuentaNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.DatoInvalidoException;
import com.entidadfinanciera.backend.domain.exception.FondosInsuficientesException;
import com.entidadfinanciera.backend.domain.model.ClaseOperacion;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.Operacion;
import com.entidadfinanciera.backend.domain.model.Sentido;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;

@ExtendWith(MockitoExtension.class)
class OperacionServiceTest {

	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T16:00:00Z"),
			ZoneId.of("America/Bogota"));

	@Mock
	private CuentaRepositoryPort cuentas;

	@Mock
	private OperacionRepositoryPort operaciones;

	private OperacionService servicio;

	@BeforeEach
	void preparar() {
		servicio = new OperacionService(cuentas, operaciones, RELOJ);
	}

	private static Cuenta cuenta(EstadoCuenta estado, String saldo) {
		return Cuenta.reconstruir(10L, 1L, TipoCuenta.AHORROS, "5300000001", estado, new BigDecimal(saldo), false,
				OffsetDateTime.now(RELOJ), null);
	}

	private void guardadosDevuelvenLoRecibido() {
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));
		when(operaciones.registrar(any())).thenAnswer(i -> ((Operacion) i.getArgument(0)).conIds(500L, List.of()));
	}

	@Test
	void consignarBloqueaAcreditaYRegistraUnCredito() {
		when(cuentas.buscarPorNumeroParaActualizar("5300000001")).thenReturn(Optional.of(cuenta(EstadoCuenta.ACTIVA, "0")));
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));
		when(operaciones.registrar(any())).thenAnswer(i -> i.getArgument(0));

		Operacion operacion = servicio.consignar(" 5300000001 ", new BigDecimal("150000"));

		assertThat(operacion.clase()).isEqualTo(ClaseOperacion.CONSIGNACION);
		assertThat(operacion.importe()).hasToString("150000.00");
		assertThat(operacion.movimientos()).singleElement().satisfies(m -> {
			assertThat(m.sentido()).isEqualTo(Sentido.CREDITO);
			assertThat(m.saldoResultante()).isEqualByComparingTo("150000.00");
		});
		verify(cuentas, never()).buscarPorNumero(any());
	}

	@Test
	void retirarRegistraUnDebitoConElSaldoResultante() {
		when(cuentas.buscarPorNumeroParaActualizar("5300000001"))
			.thenReturn(Optional.of(cuenta(EstadoCuenta.ACTIVA, "100.00")));
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));
		when(operaciones.registrar(any())).thenAnswer(i -> i.getArgument(0));

		Operacion operacion = servicio.retirar("5300000001", new BigDecimal("80.00"));

		assertThat(operacion.clase()).isEqualTo(ClaseOperacion.RETIRO);
		assertThat(operacion.movimientos().get(0).sentido()).isEqualTo(Sentido.DEBITO);
		assertThat(operacion.movimientos().get(0).saldoResultante()).isEqualByComparingTo("20.00");
	}

	@Test
	void retiroSinFondosNoGuardaNada() {
		when(cuentas.buscarPorNumeroParaActualizar("5300000001"))
			.thenReturn(Optional.of(cuenta(EstadoCuenta.ACTIVA, "20.00")));

		assertThatThrownBy(() -> servicio.retirar("5300000001", new BigDecimal("80.00")))
			.isInstanceOf(FondosInsuficientesException.class);
		verify(cuentas, never()).guardar(any());
		verifyNoInteractions(operaciones);
	}

	@Test
	void consignacionEnCuentaInactivaNoGuardaNada() {
		when(cuentas.buscarPorNumeroParaActualizar("5300000001"))
			.thenReturn(Optional.of(cuenta(EstadoCuenta.INACTIVA, "0")));

		assertThatThrownBy(() -> servicio.consignar("5300000001", BigDecimal.TEN))
			.isInstanceOf(CuentaNoActivaException.class);
		verifyNoInteractions(operaciones);
	}

	@Test
	void importeInvalidoSeRechazaAntesDeBloquear() {
		assertThatThrownBy(() -> servicio.consignar("5300000001", BigDecimal.ZERO))
			.isInstanceOf(DatoInvalidoException.class);
		verifyNoInteractions(cuentas, operaciones);
	}

	@Test
	void cuentaInexistente() {
		when(cuentas.buscarPorNumeroParaActualizar("5399999999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.retirar("5399999999", BigDecimal.ONE))
			.isInstanceOf(CuentaNoEncontradaException.class);
		verifyNoInteractions(operaciones);
	}

}
