package com.entidadfinanciera.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.entidadfinanciera.backend.application.port.out.ClienteRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.ClienteNoEncontradoException;
import com.entidadfinanciera.backend.domain.exception.CuentaCanceladaException;
import com.entidadfinanciera.backend.domain.exception.CuentaNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.SaldoDistintoDeCeroException;
import com.entidadfinanciera.backend.domain.model.Cliente;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.DatosCliente;
import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoIdentificacion;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"),
			ZoneId.of("America/Bogota"));

	@Mock
	private CuentaRepositoryPort cuentas;

	@Mock
	private ClienteRepositoryPort clientes;

	private CuentaService servicio;

	@BeforeEach
	void preparar() {
		servicio = new CuentaService(cuentas, clientes, RELOJ);
	}

	private static Cliente cliente() {
		return Cliente.reconstruir(7L, new DatosCliente(TipoIdentificacion.CC, "1", "Ana", "Gómez", "a@b.co",
				LocalDate.of(1990, 1, 1)), OffsetDateTime.now(RELOJ), null);
	}

	private static Cuenta cuenta(EstadoCuenta estado, String saldo) {
		return Cuenta.reconstruir(10L, 7L, TipoCuenta.AHORROS, "5300000001", estado, new BigDecimal(saldo), false,
				OffsetDateTime.now(RELOJ), null);
	}

	@Test
	void abreCuentaConElNumeroGeneradoParaSuTipo() {
		when(clientes.buscarPorId(7L)).thenReturn(Optional.of(cliente()));
		when(cuentas.siguienteNumero(TipoCuenta.CORRIENTE)).thenReturn("3300000004");
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));

		Cuenta abierta = servicio.abrir(7L, TipoCuenta.CORRIENTE);

		assertThat(abierta.getNumero()).isEqualTo("3300000004");
		assertThat(abierta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
		assertThat(abierta.getClienteId()).isEqualTo(7L);
	}

	@Test
	void noAbreCuentaParaClienteInexistenteNiConsumeNumero() {
		when(clientes.buscarPorId(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.abrir(9L, TipoCuenta.AHORROS))
			.isInstanceOf(ClienteNoEncontradoException.class);
		verify(cuentas, never()).siguienteNumero(any());
		verify(cuentas, never()).guardar(any());
	}

	@Test
	void consultarCuentaInexistenteLanzaNoEncontrada() {
		when(cuentas.buscarPorNumero("5399999999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.consultarPorNumero(" 5399999999 "))
			.isInstanceOf(CuentaNoEncontradaException.class);
	}

	@Test
	void cancelarBloqueaLaCuentaYGuardaElNuevoEstado() {
		when(cuentas.buscarPorIdParaActualizar(10L)).thenReturn(Optional.of(cuenta(EstadoCuenta.INACTIVA, "0")));
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));

		Cuenta cancelada = servicio.cancelar(10L);

		assertThat(cancelada.getEstado()).isEqualTo(EstadoCuenta.CANCELADA);
		verify(cuentas, never()).buscarPorId(any());
	}

	@Test
	void cancelarConSaldoNoGuarda() {
		when(cuentas.buscarPorIdParaActualizar(10L)).thenReturn(Optional.of(cuenta(EstadoCuenta.ACTIVA, "50.00")));

		assertThatThrownBy(() -> servicio.cancelar(10L)).isInstanceOf(SaldoDistintoDeCeroException.class);
		verify(cuentas, never()).guardar(any());
	}

	@Test
	void cambioSobreCuentaCanceladaNoGuarda() {
		when(cuentas.buscarPorIdParaActualizar(10L)).thenReturn(Optional.of(cuenta(EstadoCuenta.CANCELADA, "0")));

		assertThatThrownBy(() -> servicio.cambiarExencionGmf(10L, true)).isInstanceOf(CuentaCanceladaException.class);
		verify(cuentas, never()).guardar(any());
	}

	@Test
	void cambiaExencionGmf() {
		when(cuentas.buscarPorIdParaActualizar(10L)).thenReturn(Optional.of(cuenta(EstadoCuenta.ACTIVA, "0")));
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));

		servicio.cambiarExencionGmf(10L, true);

		ArgumentCaptor<Cuenta> guardada = ArgumentCaptor.forClass(Cuenta.class);
		verify(cuentas).guardar(guardada.capture());
		assertThat(guardada.getValue().isExentaGmf()).isTrue();
	}

	@Test
	void modificarCuentaInexistenteLanzaNoEncontrada() {
		when(cuentas.buscarPorIdParaActualizar(3L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.activar(3L)).isInstanceOf(CuentaNoEncontradaException.class);
	}

	@Test
	void listarPorClienteExigeClienteExistente() {
		when(clientes.buscarPorId(7L)).thenReturn(Optional.of(cliente()));
		when(cuentas.listarPorCliente(7L)).thenReturn(List.of(cuenta(EstadoCuenta.CANCELADA, "0")));

		assertThat(servicio.listarPorCliente(7L)).hasSize(1);

		when(clientes.buscarPorId(8L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> servicio.listarPorCliente(8L)).isInstanceOf(ClienteNoEncontradoException.class);
	}

}
