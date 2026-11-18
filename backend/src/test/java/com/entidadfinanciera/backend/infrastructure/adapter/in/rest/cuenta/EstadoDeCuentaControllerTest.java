package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cuenta;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.consulta.EstadoDeCuenta;
import com.entidadfinanciera.backend.application.consulta.LineaEstadoDeCuenta;
import com.entidadfinanciera.backend.application.port.in.ConsultarOperacionesUseCase;
import com.entidadfinanciera.backend.domain.exception.DatoInvalidoException;
import com.entidadfinanciera.backend.domain.model.ClaseOperacion;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.Sentido;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;
import com.entidadfinanciera.backend.infrastructure.adapter.in.rest.comun.ManejadorDeErrores;

@WebMvcTest(EstadoDeCuentaController.class)
@Import({ ManejadorDeErrores.class, EstadoDeCuentaControllerTest.RelojDePrueba.class })
class EstadoDeCuentaControllerTest {

	@TestConfiguration
	static class RelojDePrueba {

		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-11-16T16:05:00Z"), ZoneId.of("America/Bogota"));
		}

	}

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConsultarOperacionesUseCase consultas;

	@Test
	void devuelveSaldoTitularEHistorialConElFormatoDelContrato() throws Exception {
		Cuenta cuenta = Cuenta.reconstruir(10L, 1L, TipoCuenta.AHORROS, "5300000001", EstadoCuenta.ACTIVA,
				new BigDecimal("120000.00"), false, OffsetDateTime.parse("2026-11-01T15:00:00Z"), null);
		LineaEstadoDeCuenta linea = new LineaEstadoDeCuenta(901L, 501L, ClaseOperacion.TRANSFERENCIA, Sentido.DEBITO,
				new BigDecimal("30000.00"), OffsetDateTime.parse("2026-11-16T16:00:00Z"), "3300000004", false);
		when(consultas.estadoDeCuenta(10L, Optional.of(LocalDate.of(2026, 11, 1)), Optional.of(LocalDate.of(2026, 11, 30)),
				new Paginacion(0, 20)))
			.thenReturn(new EstadoDeCuenta(cuenta, "Ana María Gómez", OffsetDateTime.parse("2026-11-16T16:05:00Z"),
					new Pagina<>(List.of(linea), 0, 20, 1, 1)));

		mvc.perform(get("/api/v1/cuentas/10/estado-de-cuenta").param("desde", "2026-11-01").param("hasta", "2026-11-30"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cuenta.numero").value("5300000001"))
			.andExpect(jsonPath("$.cuenta.titular").value("Ana María Gómez"))
			.andExpect(jsonPath("$.saldo").value("120000.00"))
			.andExpect(jsonPath("$.saldoDisponible").value("120000.00"))
			.andExpect(jsonPath("$.fechaConsulta").value("2026-11-16T11:05:00-05:00"))
			.andExpect(jsonPath("$.movimientos.contenido[0].operacionId").value(501))
			.andExpect(jsonPath("$.movimientos.contenido[0].fecha").value("2026-11-16T11:00:00-05:00"))
			.andExpect(jsonPath("$.movimientos.contenido[0].contraparte").value("3300000004"))
			.andExpect(jsonPath("$.movimientos.contenido[0].reversada").value(false))
			.andExpect(jsonPath("$.movimientos.totalElementos").value(1));
	}

	@Test
	void fechaMalFormadaORangoInvertidoDevuelve400() throws Exception {
		when(consultas.estadoDeCuenta(eq(10L), any(), any(), any()))
			.thenThrow(new DatoInvalidoException("desde", "no puede ser posterior a hasta"));

		mvc.perform(get("/api/v1/cuentas/10/estado-de-cuenta").param("desde", "16-11-2026"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		mvc.perform(get("/api/v1/cuentas/10/estado-de-cuenta").param("desde", "2026-11-30").param("hasta", "2026-11-01"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detalles[0].campo").value("desde"));
	}

}
