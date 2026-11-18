package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.operacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import com.entidadfinanciera.backend.application.port.in.RegistrarOperacionesUseCase;
import com.entidadfinanciera.backend.domain.exception.CuentaNoActivaException;
import com.entidadfinanciera.backend.domain.exception.CuentaNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.FondosInsuficientesException;
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

}
