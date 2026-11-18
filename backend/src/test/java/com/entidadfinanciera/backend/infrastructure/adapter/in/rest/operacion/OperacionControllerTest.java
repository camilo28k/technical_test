package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.operacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.entidadfinanciera.backend.application.port.in.ConsultarOperacionesUseCase;
import com.entidadfinanciera.backend.application.port.in.RegistrarOperacionesUseCase;
import com.entidadfinanciera.backend.domain.exception.CuentaNoActivaException;
import com.entidadfinanciera.backend.domain.exception.CuentaNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.FondosInsuficientesException;
import com.entidadfinanciera.backend.domain.exception.MismaCuentaException;
import com.entidadfinanciera.backend.domain.exception.OperacionNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.OperacionYaReversadaException;
import com.entidadfinanciera.backend.domain.exception.ReversoNoReversableException;
import com.entidadfinanciera.backend.domain.model.ClaseOperacion;
import com.entidadfinanciera.backend.domain.model.Movimiento;
import com.entidadfinanciera.backend.domain.model.Operacion;
import com.entidadfinanciera.backend.domain.model.Sentido;
import com.entidadfinanciera.backend.infrastructure.adapter.in.rest.comun.ManejadorDeErrores;

@WebMvcTest(OperacionController.class)
@Import({ ManejadorDeErrores.class, OperacionControllerTest.RelojDePrueba.class })
class OperacionControllerTest {

	private static final String CUERPO = "{\"numeroCuenta\":\"5300000001\",\"importe\":\"150000.00\"}";

	@TestConfiguration
	static class RelojDePrueba {

		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-11-16T16:00:00Z"), ZoneId.of("America/Bogota"));
		}

	}

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RegistrarOperacionesUseCase operaciones;

	@MockitoBean
	private ConsultarOperacionesUseCase consultas;

	private static Operacion operacion(ClaseOperacion clase, Sentido sentido, String saldo) {
		return new Operacion(501L, clase, new BigDecimal("150000.00"), OffsetDateTime.parse("2026-11-16T16:00:00Z"),
				null, List.of(new Movimiento(901L, 10L, "5300000001", sentido, new BigDecimal(saldo))));
	}

	@Test
	void consignarDevuelve201ConMovimientoYSaldoResultante() throws Exception {
		when(operaciones.consignar(eq("5300000001"), any()))
			.thenReturn(operacion(ClaseOperacion.CONSIGNACION, Sentido.CREDITO, "150000.00"));

		mvc.perform(post("/api/v1/operaciones/consignaciones").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(501))
			.andExpect(jsonPath("$.clase").value("CONSIGNACION"))
			.andExpect(jsonPath("$.importe").value("150000.00"))
			.andExpect(jsonPath("$.fecha").value("2026-11-16T11:00:00-05:00"))
			.andExpect(jsonPath("$.operacionOriginalId").doesNotExist())
			.andExpect(jsonPath("$.movimientos[0].sentido").value("CREDITO"))
			.andExpect(jsonPath("$.movimientos[0].saldoResultante").value("150000.00"));
	}

	@Test
	void importeNoPositivoOConTresDecimalesDevuelve400() throws Exception {
		for (String importe : List.of("0", "-1", "10.001")) {
			mvc.perform(post("/api/v1/operaciones/retiros").contentType(MediaType.APPLICATION_JSON)
				.content("{\"numeroCuenta\":\"5300000001\",\"importe\":\"" + importe + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.detalles[0].campo").value("importe"));
		}
		verify(operaciones, never()).retirar(any(), any());
	}

	@Test
	void retiroSinFondosDevuelve409() throws Exception {
		when(operaciones.retirar(eq("5300000001"), any()))
			.thenThrow(new FondosInsuficientesException("5300000001", new BigDecimal("150000.00")));

		mvc.perform(post("/api/v1/operaciones/retiros").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("FONDOS_INSUFICIENTES"));
	}

	@Test
	void cuentaNoActivaDevuelve409() throws Exception {
		when(operaciones.consignar(eq("5300000001"), any()))
			.thenThrow(new CuentaNoActivaException("5300000001", "INACTIVA"));

		mvc.perform(post("/api/v1/operaciones/consignaciones").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("CUENTA_NO_ACTIVA"));
	}

	@Test
	void cuentaInexistenteDevuelve404() throws Exception {
		when(operaciones.consignar(eq("5300000001"), any()))
			.thenThrow(CuentaNoEncontradaException.porNumero("5300000001"));

		mvc.perform(post("/api/v1/operaciones/consignaciones").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.codigo").value("CUENTA_NO_ENCONTRADA"));
	}

	@Test
	void conflictoDeBloqueoDevuelve409Reintentable() throws Exception {
		when(operaciones.retirar(eq("5300000001"), any())).thenThrow(new CannotAcquireLockException("timeout"));

		mvc.perform(post("/api/v1/operaciones/retiros").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("CONFLICTO_CONCURRENCIA"));
	}

	private static final String TRANSFERENCIA = """
			{"numeroCuentaOrigen":"5300000001","numeroCuentaDestino":"3300000004","importe":"30000.00"}
			""";

	@Test
	void transferirDevuelve201ConDebitoYCredito() throws Exception {
		when(operaciones.transferir(eq("5300000001"), eq("3300000004"), any()))
			.thenReturn(new Operacion(501L, ClaseOperacion.TRANSFERENCIA, new BigDecimal("30000.00"),
					OffsetDateTime.parse("2026-11-16T16:00:00Z"), null,
					List.of(new Movimiento(901L, 10L, "5300000001", Sentido.DEBITO, new BigDecimal("120000.00")),
							new Movimiento(902L, 11L, "3300000004", Sentido.CREDITO, new BigDecimal("30000.00")))));

		mvc.perform(post("/api/v1/operaciones/transferencias").contentType(MediaType.APPLICATION_JSON)
			.content(TRANSFERENCIA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.clase").value("TRANSFERENCIA"))
			.andExpect(jsonPath("$.movimientos.length()").value(2))
			.andExpect(jsonPath("$.movimientos[0].sentido").value("DEBITO"))
			.andExpect(jsonPath("$.movimientos[0].saldoResultante").value("120000.00"))
			.andExpect(jsonPath("$.movimientos[1].sentido").value("CREDITO"));
	}

	@Test
	void transferirALaMismaCuentaDevuelve409() throws Exception {
		when(operaciones.transferir(any(), any(), any())).thenThrow(new MismaCuentaException("5300000001"));

		mvc.perform(post("/api/v1/operaciones/transferencias").contentType(MediaType.APPLICATION_JSON)
			.content(TRANSFERENCIA))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("MISMA_CUENTA"));
	}

	@Test
	void transferirSinDestinoDevuelve400() throws Exception {
		mvc.perform(post("/api/v1/operaciones/transferencias").contentType(MediaType.APPLICATION_JSON)
			.content("{\"numeroCuentaOrigen\":\"5300000001\",\"importe\":\"10.00\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detalles[0].campo").value("numeroCuentaDestino"));
		verify(operaciones, never()).transferir(any(), any(), any());
	}

	@Test
	void reversarDevuelve201VinculadoAlOriginal() throws Exception {
		when(operaciones.reversar(501L)).thenReturn(new Operacion(600L, ClaseOperacion.REVERSO,
				new BigDecimal("150000.00"), OffsetDateTime.parse("2026-11-16T16:00:00Z"), 501L,
				List.of(new Movimiento(950L, 10L, "5300000001", Sentido.DEBITO, new BigDecimal("0.00")))));

		mvc.perform(post("/api/v1/operaciones/501/reverso"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.clase").value("REVERSO"))
			.andExpect(jsonPath("$.operacionOriginalId").value(501))
			.andExpect(jsonPath("$.movimientos[0].sentido").value("DEBITO"));
	}

	@Test
	void reversarDevuelveLosCodigosDelContrato() throws Exception {
		when(operaciones.reversar(1L)).thenThrow(new OperacionNoEncontradaException(1L));
		when(operaciones.reversar(2L)).thenThrow(new OperacionYaReversadaException(2L));
		when(operaciones.reversar(3L)).thenThrow(new ReversoNoReversableException(3L));

		mvc.perform(post("/api/v1/operaciones/1/reverso"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.codigo").value("OPERACION_NO_ENCONTRADA"));
		mvc.perform(post("/api/v1/operaciones/2/reverso"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("OPERACION_YA_REVERSADA"));
		mvc.perform(post("/api/v1/operaciones/3/reverso"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("REVERSO_NO_REVERSABLE"));
	}

	@Test
	void consultarOperacionDevuelveSusMovimientosSinSaldoResultante() throws Exception {
		when(consultas.consultarOperacion(501L)).thenReturn(new Operacion(501L, ClaseOperacion.TRANSFERENCIA,
				new BigDecimal("30.00"), OffsetDateTime.parse("2026-11-16T16:00:00Z"), null,
				List.of(new Movimiento(901L, 10L, "5300000001", Sentido.DEBITO, null),
						new Movimiento(902L, 11L, "3300000004", Sentido.CREDITO, null))));

		mvc.perform(get("/api/v1/operaciones/501"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.clase").value("TRANSFERENCIA"))
			.andExpect(jsonPath("$.movimientos[1].numeroCuenta").value("3300000004"))
			.andExpect(jsonPath("$.movimientos[0].saldoResultante").doesNotExist());
	}

	@Test
	void consultarOperacionInexistenteDevuelve404() throws Exception {
		when(consultas.consultarOperacion(9L)).thenThrow(new OperacionNoEncontradaException(9L));

		mvc.perform(get("/api/v1/operaciones/9"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.codigo").value("OPERACION_NO_ENCONTRADA"));
	}

}
