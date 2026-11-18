package com.entidadfinanciera.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.entidadfinanciera.backend.application.port.out.ClienteRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.ClienteConProductosException;
import com.entidadfinanciera.backend.domain.exception.ClienteMenorDeEdadException;
import com.entidadfinanciera.backend.domain.exception.ClienteNoEncontradoException;
import com.entidadfinanciera.backend.domain.exception.IdentificacionDuplicadaException;
import com.entidadfinanciera.backend.domain.model.Cliente;
import com.entidadfinanciera.backend.domain.model.DatosCliente;
import com.entidadfinanciera.backend.domain.model.TipoIdentificacion;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-03T13:00:00Z"),
			ZoneId.of("America/Bogota"));

	private static final DatosCliente DATOS = new DatosCliente(TipoIdentificacion.CC, "12345", "Ana", "Gómez",
			"ana@correo.com", LocalDate.of(1990, 5, 20));

	@Mock
	private ClienteRepositoryPort repositorio;

	@Mock
	private CuentaRepositoryPort cuentas;

	private ClienteService servicio;

	@BeforeEach
	void preparar() {
		servicio = new ClienteService(repositorio, cuentas, RELOJ);
	}

	private static Cliente persistido(Long id, DatosCliente datos) {
		return Cliente.reconstruir(id, datos, OffsetDateTime.now(RELOJ), null);
	}

	@Test
	void registraClienteConIdentificacionNueva() {
		when(repositorio.existePorIdentificacion(TipoIdentificacion.CC, "12345")).thenReturn(false);
		when(repositorio.guardar(any())).thenAnswer(i -> persistido(1L, DATOS));

		Cliente cliente = servicio.registrar(DATOS);

		assertThat(cliente.getId()).isEqualTo(1L);
		verify(repositorio).guardar(any());
	}

	@Test
	void rechazaIdentificacionDuplicadaSinGuardar() {
		when(repositorio.existePorIdentificacion(TipoIdentificacion.CC, "12345")).thenReturn(true);

		assertThatThrownBy(() -> servicio.registrar(DATOS)).isInstanceOf(IdentificacionDuplicadaException.class);
		verify(repositorio, never()).guardar(any());
	}

	@Test
	void rechazaMenorDeEdadSinConsultarNiGuardar() {
		DatosCliente menor = new DatosCliente(TipoIdentificacion.CC, "9", "Luis", "Paz", "l@p.co",
				LocalDate.of(2010, 1, 1));

		assertThatThrownBy(() -> servicio.registrar(menor)).isInstanceOf(ClienteMenorDeEdadException.class);
		verify(repositorio, never()).existePorIdentificacion(any(), any());
		verify(repositorio, never()).guardar(any());
	}

	@Test
	void consultarClienteInexistenteLanzaNoEncontrado() {
		when(repositorio.buscarPorId(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.consultar(99L)).isInstanceOf(ClienteNoEncontradoException.class);
	}

	@Test
	void actualizarSinCambiarIdentificacionNoVerificaDuplicados() {
		when(repositorio.buscarPorId(1L)).thenReturn(Optional.of(persistido(1L, DATOS)));
		when(repositorio.guardar(any())).thenAnswer(i -> i.getArgument(0));

		Cliente actualizado = servicio.actualizar(1L, new DatosCliente(TipoIdentificacion.CC, "12345", "Ana", "Gómez",
				"otro@correo.com", LocalDate.of(1990, 5, 20)));

		assertThat(actualizado.getCorreo()).isEqualTo("otro@correo.com");
		assertThat(actualizado.getFechaModificacion()).isEqualTo(OffsetDateTime.now(RELOJ));
		verify(repositorio, never()).existePorIdentificacion(any(), any());
	}

	@Test
	void actualizarAIdentificacionDeOtroClienteSeRechaza() {
		when(repositorio.buscarPorId(1L)).thenReturn(Optional.of(persistido(1L, DATOS)));
		when(repositorio.existePorIdentificacion(TipoIdentificacion.CE, "777")).thenReturn(true);

		assertThatThrownBy(() -> servicio.actualizar(1L, new DatosCliente(TipoIdentificacion.CE, "777", "Ana", "Gómez",
				"ana@correo.com", LocalDate.of(1990, 5, 20))))
			.isInstanceOf(IdentificacionDuplicadaException.class);
		verify(repositorio, never()).guardar(any());
	}

	@Test
	void eliminarClienteExistente() {
		when(repositorio.buscarPorId(1L)).thenReturn(Optional.of(persistido(1L, DATOS)));

		servicio.eliminar(1L);

		verify(repositorio).eliminar(1L);
	}

	@Test
	void eliminarClienteInexistenteNoBorraNada() {
		when(repositorio.buscarPorId(5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.eliminar(5L)).isInstanceOf(ClienteNoEncontradoException.class);
		verify(repositorio, never()).eliminar(any());
	}

	@Test
	void listarIgnoraFiltroVacio() {
		Paginacion paginacion = new Paginacion(0, 20);
		when(repositorio.listar(Optional.empty(), paginacion)).thenReturn(new Pagina<>(List.of(), 0, 20, 0, 0));

		assertThat(servicio.listar(Optional.of("  "), paginacion).totalElementos()).isZero();
	}

	@Test
	void noEliminaClienteConCuentasAunqueEstenCanceladas() {
		when(repositorio.buscarPorId(4L)).thenReturn(Optional.of(persistido(4L, DATOS)));
		when(cuentas.existePorCliente(4L)).thenReturn(true);

		assertThatThrownBy(() -> servicio.eliminar(4L)).isInstanceOf(ClienteConProductosException.class);
		verify(repositorio, never()).eliminar(any());
	}

}
