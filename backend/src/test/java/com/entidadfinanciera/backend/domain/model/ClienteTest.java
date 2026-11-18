package com.entidadfinanciera.backend.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.entidadfinanciera.backend.domain.exception.ClienteMenorDeEdadException;
import com.entidadfinanciera.backend.domain.exception.DatoInvalidoException;

class ClienteTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

	/** 3 de noviembre de 2026, 08:00 en Colombia (HU-01). */
	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-03T13:00:00Z"), BOGOTA);

	static DatosCliente datos(LocalDate nacimiento) {
		return new DatosCliente(TipoIdentificacion.CC, " 12345 ", " Ana María ", "Gómez", "Ana.Gomez@Correo.com ",
				nacimiento);
	}

	@Test
	void registraElDiaQueCumpleDieciochoAnos() {
		Cliente cliente = Cliente.registrar(datos(LocalDate.of(2008, 11, 3)), RELOJ);

		assertThat(cliente.getFechaCreacion()).isEqualTo(OffsetDateTime.now(RELOJ));
		assertThat(cliente.getFechaModificacion()).isNull();
		assertThat(cliente.getNumeroIdentificacion()).isEqualTo("12345");
		assertThat(cliente.getNombres()).isEqualTo("Ana María");
		assertThat(cliente.getCorreo()).isEqualTo("ana.gomez@correo.com");
	}

	@Test
	void rechazaAlMenorUnDiaAntesDeCumplirDieciocho() {
		assertThatThrownBy(() -> Cliente.registrar(datos(LocalDate.of(2008, 11, 4)), RELOJ))
			.isInstanceOf(ClienteMenorDeEdadException.class);
	}

	@Test
	void usaLaFechaDeColombiaYNoLaDeUtc() {
		// 4 de noviembre a las 02:00 UTC sigue siendo 3 de noviembre en Colombia.
		Clock nocheEnColombia = Clock.fixed(Instant.parse("2026-11-04T02:00:00Z"), BOGOTA);

		assertThatThrownBy(() -> Cliente.registrar(datos(LocalDate.of(2008, 11, 4)), nocheEnColombia))
			.isInstanceOf(ClienteMenorDeEdadException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "sin-arroba.com", "ana@dominio", "ana @correo.com", "@correo.com" })
	void rechazaCorreoConFormatoInvalido(String correo) {
		DatosCliente invalido = new DatosCliente(TipoIdentificacion.CC, "1", "Ana", "Gómez", correo,
				LocalDate.of(1990, 1, 1));

		assertThatThrownBy(() -> Cliente.registrar(invalido, RELOJ)).isInstanceOf(DatoInvalidoException.class)
			.extracting("campo")
			.isEqualTo("correo");
	}

	@Test
	void rechazaNombreDeUnCaracter() {
		DatosCliente invalido = new DatosCliente(TipoIdentificacion.CC, "1", " A ", "Gómez", "a@b.co",
				LocalDate.of(1990, 1, 1));

		assertThatThrownBy(() -> Cliente.registrar(invalido, RELOJ)).isInstanceOf(DatoInvalidoException.class)
			.extracting("campo")
			.isEqualTo("nombres");
	}

	@Test
	void actualizarRegistraLaFechaDeModificacionYConservaLaDeCreacion() {
		Cliente cliente = Cliente.registrar(datos(LocalDate.of(1990, 1, 1)), RELOJ);
		Clock despues = Clock.offset(RELOJ, java.time.Duration.ofDays(2));

		cliente.actualizar(new DatosCliente(TipoIdentificacion.CC, "12345", "Ana", "Gómez", "nuevo@correo.com",
				LocalDate.of(1990, 1, 1)), despues);

		assertThat(cliente.getCorreo()).isEqualTo("nuevo@correo.com");
		assertThat(cliente.getFechaCreacion()).isEqualTo(OffsetDateTime.now(RELOJ));
		assertThat(cliente.getFechaModificacion()).isEqualTo(OffsetDateTime.now(despues));
	}

	@Test
	void actualizarANacimientoDeMenorSeRechazaYConservaLosDatos() {
		Cliente cliente = Cliente.registrar(datos(LocalDate.of(1990, 1, 1)), RELOJ);

		assertThatThrownBy(() -> cliente.actualizar(datos(LocalDate.of(2015, 1, 1)), RELOJ))
			.isInstanceOf(ClienteMenorDeEdadException.class);
		assertThat(cliente.getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 1, 1));
		assertThat(cliente.getFechaModificacion()).isNull();
	}

}
