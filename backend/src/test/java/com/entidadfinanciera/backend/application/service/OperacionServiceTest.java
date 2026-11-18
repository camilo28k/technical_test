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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.OperacionRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.CuentaNoActivaException;
import com.entidadfinanciera.backend.domain.exception.CuentaNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.DatoInvalidoException;
import com.entidadfinanciera.backend.domain.exception.FondosInsuficientesException;
import com.entidadfinanciera.backend.domain.exception.MismaCuentaException;
import com.entidadfinanciera.backend.domain.exception.OperacionNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.OperacionYaReversadaException;
import com.entidadfinanciera.backend.domain.exception.ReversoNoReversableException;
import com.entidadfinanciera.backend.domain.model.Movimiento;
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

	private static Cuenta cuenta(Long id, String numero, EstadoCuenta estado, String saldo) {
		return Cuenta.reconstruir(id, 1L, TipoCuenta.AHORROS, numero, estado, new BigDecimal(saldo), false,
				OffsetDateTime.now(RELOJ), null);
	}

	private void cuentasParaTransferir(Cuenta origen, Cuenta destino) {
		when(cuentas.buscarPorNumero(origen.getNumero())).thenReturn(Optional.of(origen));
		when(cuentas.buscarPorNumero(destino.getNumero())).thenReturn(Optional.of(destino));
		when(cuentas.buscarPorIdParaActualizar(origen.getId())).thenReturn(Optional.of(origen));
		when(cuentas.buscarPorIdParaActualizar(destino.getId())).thenReturn(Optional.of(destino));
	}

	@Test
	void transferirDebitaOrigenAcreditaDestinoYRegistraAmbosMovimientos() {
		cuentasParaTransferir(cuenta(10L, "5300000001", EstadoCuenta.ACTIVA, "100.00"),
				cuenta(11L, "3300000004", EstadoCuenta.ACTIVA, "20.00"));
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));
		when(operaciones.registrar(any())).thenAnswer(i -> i.getArgument(0));

		Operacion operacion = servicio.transferir("5300000001", "3300000004", new BigDecimal("30"));

		assertThat(operacion.clase()).isEqualTo(ClaseOperacion.TRANSFERENCIA);
		assertThat(operacion.movimientos()).extracting(m -> m.numeroCuenta() + ":" + m.sentido() + ":" + m.saldoResultante())
			.containsExactly("5300000001:DEBITO:70.00", "3300000004:CREDITO:50.00");
	}

	@Test
	void transferirBloqueaEnOrdenDeIdentificadorAunqueElOrigenSeaMayor() {
		cuentasParaTransferir(cuenta(11L, "3300000004", EstadoCuenta.ACTIVA, "100.00"),
				cuenta(10L, "5300000001", EstadoCuenta.ACTIVA, "0"));
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));
		when(operaciones.registrar(any())).thenAnswer(i -> i.getArgument(0));

		servicio.transferir("3300000004", "5300000001", BigDecimal.TEN);

		InOrder orden = Mockito.inOrder(cuentas);
		orden.verify(cuentas).buscarPorIdParaActualizar(10L);
		orden.verify(cuentas).buscarPorIdParaActualizar(11L);
	}

	@Test
	void transferirALaMismaCuentaSeRechazaSinConsultar() {
		assertThatThrownBy(() -> servicio.transferir("5300000001", " 5300000001 ", BigDecimal.TEN))
			.isInstanceOf(MismaCuentaException.class);
		verifyNoInteractions(cuentas, operaciones);
	}

	@Test
	void destinoInactivoNoGuardaNingunaCuenta() {
		cuentasParaTransferir(cuenta(10L, "5300000001", EstadoCuenta.ACTIVA, "100.00"),
				cuenta(11L, "3300000004", EstadoCuenta.INACTIVA, "0"));

		assertThatThrownBy(() -> servicio.transferir("5300000001", "3300000004", BigDecimal.TEN))
			.isInstanceOf(CuentaNoActivaException.class);
		verify(cuentas, never()).guardar(any());
		verifyNoInteractions(operaciones);
	}

	@Test
	void destinoInexistenteNoBloqueaNada() {
		when(cuentas.buscarPorNumero("5300000001")).thenReturn(Optional.of(cuenta(EstadoCuenta.ACTIVA, "100")));
		when(cuentas.buscarPorNumero("3399999999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.transferir("5300000001", "3399999999", BigDecimal.TEN))
			.isInstanceOf(CuentaNoEncontradaException.class);
		verify(cuentas, never()).buscarPorIdParaActualizar(any());
	}

	private static Operacion transferenciaOriginal() {
		return new Operacion(501L, ClaseOperacion.TRANSFERENCIA, new BigDecimal("30.00"), OffsetDateTime.now(RELOJ),
				null, List.of(new Movimiento(1L, 11L, "3300000004", Sentido.DEBITO, null),
						new Movimiento(2L, 10L, "5300000001", Sentido.CREDITO, null)));
	}

	@Test
	void reversarTransferenciaCompensaAmbasCuentasBloqueandoEnOrden() {
		when(operaciones.buscarPorId(501L)).thenReturn(Optional.of(transferenciaOriginal()));
		when(operaciones.existeReversoDe(501L)).thenReturn(false);
		when(cuentas.buscarPorIdParaActualizar(10L))
			.thenReturn(Optional.of(cuenta(10L, "5300000001", EstadoCuenta.ACTIVA, "30.00")));
		when(cuentas.buscarPorIdParaActualizar(11L))
			.thenReturn(Optional.of(cuenta(11L, "3300000004", EstadoCuenta.ACTIVA, "70.00")));
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));
		when(operaciones.registrar(any())).thenAnswer(i -> i.getArgument(0));

		Operacion reverso = servicio.reversar(501L);

		assertThat(reverso.clase()).isEqualTo(ClaseOperacion.REVERSO);
		assertThat(reverso.originalId()).isEqualTo(501L);
		assertThat(reverso.movimientos()).extracting(m -> m.numeroCuenta() + ":" + m.sentido() + ":" + m.saldoResultante())
			.containsExactly("3300000004:CREDITO:100.00", "5300000001:DEBITO:0.00");
		InOrder orden = Mockito.inOrder(cuentas, operaciones);
		orden.verify(cuentas).buscarPorIdParaActualizar(10L);
		orden.verify(cuentas).buscarPorIdParaActualizar(11L);
		orden.verify(operaciones).existeReversoDe(501L);
	}

	@Test
	void reversoSinFondosEnLaCuentaAcreditadaNoGuardaNada() {
		when(operaciones.buscarPorId(501L)).thenReturn(Optional.of(transferenciaOriginal()));
		when(cuentas.buscarPorIdParaActualizar(10L))
			.thenReturn(Optional.of(cuenta(10L, "5300000001", EstadoCuenta.ACTIVA, "29.99")));
		when(cuentas.buscarPorIdParaActualizar(11L))
			.thenReturn(Optional.of(cuenta(11L, "3300000004", EstadoCuenta.ACTIVA, "70.00")));

		assertThatThrownBy(() -> servicio.reversar(501L)).isInstanceOf(FondosInsuficientesException.class);
		verify(cuentas, never()).guardar(any());
		verify(operaciones, never()).registrar(any());
	}

	@Test
	void unaOperacionYaReversadaSeRechazaSinBloquear() {
		when(operaciones.buscarPorId(501L)).thenReturn(Optional.of(transferenciaOriginal()));
		when(operaciones.existeReversoDe(501L)).thenReturn(true);

		assertThatThrownBy(() -> servicio.reversar(501L)).isInstanceOf(OperacionYaReversadaException.class);
		verifyNoInteractions(cuentas);
	}

	@Test
	void unReversoNoPuedeReversarse() {
		when(operaciones.buscarPorId(600L)).thenReturn(Optional.of(new Operacion(600L, ClaseOperacion.REVERSO,
				BigDecimal.TEN, OffsetDateTime.now(RELOJ), 501L, List.of())));

		assertThatThrownBy(() -> servicio.reversar(600L)).isInstanceOf(ReversoNoReversableException.class);
		verifyNoInteractions(cuentas);
	}

	@Test
	void reversarOperacionInexistente() {
		when(operaciones.buscarPorId(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.reversar(9L)).isInstanceOf(OperacionNoEncontradaException.class);
	}

}
