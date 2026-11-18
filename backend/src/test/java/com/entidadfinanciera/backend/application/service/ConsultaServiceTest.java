package com.entidadfinanciera.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.consulta.EstadoDeCuenta;
import com.entidadfinanciera.backend.application.port.out.ClienteRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.HistorialQueryPort;
import com.entidadfinanciera.backend.application.port.out.OperacionRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.CuentaNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.DatoInvalidoException;
import com.entidadfinanciera.backend.domain.exception.OperacionNoEncontradaException;
import com.entidadfinanciera.backend.domain.model.Cliente;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.DatosCliente;
import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoIdentificacion;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-18T16:00:00Z"), BOGOTA);

	@Mock
	private CuentaRepositoryPort cuentas;

	@Mock
	private ClienteRepositoryPort clientes;

	@Mock
	private OperacionRepositoryPort operaciones;

	@Mock
	private HistorialQueryPort historial;

	private ConsultaService servicio;

	@BeforeEach
	void preparar() {
		servicio = new ConsultaService(cuentas, clientes, operaciones, historial, RELOJ);
	}

	private void cuentaConTitular() {
		when(cuentas.buscarPorId(10L)).thenReturn(Optional.of(Cuenta.reconstruir(10L, 1L, TipoCuenta.AHORROS,
				"5300000001", EstadoCuenta.ACTIVA, new BigDecimal("70.00"), false, OffsetDateTime.now(RELOJ), null)));
		when(clientes.buscarPorId(1L)).thenReturn(Optional.of(Cliente.reconstruir(1L, new DatosCliente(
				TipoIdentificacion.CC, "1", "Ana María", "Gómez", "a@b.co", LocalDate.of(1990, 1, 1)),
				OffsetDateTime.now(RELOJ), null)));
	}

	@Test
	void convierteLasFechasCivilesDeColombiaEnUnRangoSemiabierto() {
		cuentaConTitular();
		OffsetDateTime inicio = OffsetDateTime.parse("2026-11-01T00:00:00-05:00");
		OffsetDateTime finExclusivo = OffsetDateTime.parse("2026-12-01T00:00:00-05:00");
		when(historial.movimientos(10L, Optional.of(inicio), Optional.of(finExclusivo), new Paginacion(0, 20)))
			.thenReturn(new Pagina<>(List.of(), 0, 20, 0, 0));

		EstadoDeCuenta estado = servicio.estadoDeCuenta(10L, Optional.of(LocalDate.of(2026, 11, 1)),
				Optional.of(LocalDate.of(2026, 11, 30)), new Paginacion(0, 20));

		assertThat(estado.titular()).isEqualTo("Ana María Gómez");
		assertThat(estado.cuenta().getSaldo()).isEqualByComparingTo("70.00");
		assertThat(estado.fechaConsulta()).isEqualTo(OffsetDateTime.now(RELOJ));
	}

	@Test
	void sinFechasConsultaTodoElHistorial() {
		cuentaConTitular();
		when(historial.movimientos(any(), any(), any(), any())).thenReturn(new Pagina<>(List.of(), 0, 20, 0, 0));

		servicio.estadoDeCuenta(10L, Optional.empty(), Optional.empty(), new Paginacion(0, 20));

		org.mockito.Mockito.verify(historial)
			.movimientos(10L, Optional.empty(), Optional.empty(), new Paginacion(0, 20));
	}

	@Test
	void desdePosteriorAHastaSeRechazaSinConsultar() {
		assertThatThrownBy(() -> servicio.estadoDeCuenta(10L, Optional.of(LocalDate.of(2026, 11, 30)),
				Optional.of(LocalDate.of(2026, 11, 1)), new Paginacion(0, 20)))
			.isInstanceOf(DatoInvalidoException.class);
		verifyNoInteractions(cuentas, historial);
	}

	@Test
	void cuentaUOperacionInexistentes() {
		when(cuentas.buscarPorId(99L)).thenReturn(Optional.empty());
		when(operaciones.buscarPorId(98L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.estadoDeCuenta(99L, Optional.empty(), Optional.empty(), new Paginacion(0, 20)))
			.isInstanceOf(CuentaNoEncontradaException.class);
		assertThatThrownBy(() -> servicio.consultarOperacion(98L)).isInstanceOf(OperacionNoEncontradaException.class);
	}

}
