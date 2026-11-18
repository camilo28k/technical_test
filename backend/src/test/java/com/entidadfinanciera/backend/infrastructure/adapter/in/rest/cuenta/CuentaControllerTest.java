package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cuenta;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.port.in.GestionarCuentasUseCase;
import com.entidadfinanciera.backend.domain.exception.ClienteNoEncontradoException;
import com.entidadfinanciera.backend.domain.exception.CuentaCanceladaException;
import com.entidadfinanciera.backend.domain.exception.CuentaNoEncontradaException;
import com.entidadfinanciera.backend.domain.exception.SaldoDistintoDeCeroException;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;
import com.entidadfinanciera.backend.infrastructure.adapter.in.rest.comun.ManejadorDeErrores;

@WebMvcTest(CuentaController.class)
@Import({ ManejadorDeErrores.class, CuentaControllerTest.RelojDePrueba.class })
class CuentaControllerTest {

	@TestConfiguration
	static class RelojDePrueba {

		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-11-16T15:35:00Z"), ZoneId.of("America/Bogota"));
		}

	}

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private GestionarCuentasUseCase cuentas;

	private static Cuenta cuenta(EstadoCuenta estado) {
		return Cuenta.reconstruir(10L, 1L, TipoCuenta.AHORROS, "5300000001", estado, BigDecimal.ZERO, false,
				OffsetDateTime.parse("2026-11-16T15:35:00Z"), null);
	}

	@Test
	void abrirDevuelve201ConLocationYSaldoComoTexto() throws Exception {
		when(cuentas.abrir(1L, TipoCuenta.AHORROS)).thenReturn(cuenta(EstadoCuenta.ACTIVA));

		mvc.perform(post("/api/v1/cuentas").contentType(MediaType.APPLICATION_JSON)
			.content("{\"clienteId\":1,\"tipoCuenta\":\"AHORROS\"}"))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/v1/cuentas/10"))
			.andExpect(jsonPath("$.numero").value("5300000001"))
			.andExpect(jsonPath("$.estado").value("ACTIVA"))
			.andExpect(jsonPath("$.saldo").value("0.00"))
			.andExpect(jsonPath("$.saldoDisponible").value("0.00"))
			.andExpect(jsonPath("$.exentaGmf").value(false))
			.andExpect(jsonPath("$.fechaCreacion").value("2026-11-16T10:35:00-05:00"));
	}

	@Test
	void abrirConTipoNoAdmitidoDevuelve400() throws Exception {
		mvc.perform(post("/api/v1/cuentas").contentType(MediaType.APPLICATION_JSON)
			.content("{\"clienteId\":1,\"tipoCuenta\":\"CDT\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.codigo").value("VALIDACION"))
			.andExpect(jsonPath("$.detalles[0].campo").value("tipoCuenta"));
		verify(cuentas, never()).abrir(any(), any());
	}

	@Test
	void abrirParaClienteInexistenteDevuelve404() throws Exception {
		when(cuentas.abrir(9L, TipoCuenta.CORRIENTE)).thenThrow(new ClienteNoEncontradoException(9L));

		mvc.perform(post("/api/v1/cuentas").contentType(MediaType.APPLICATION_JSON)
			.content("{\"clienteId\":9,\"tipoCuenta\":\"CORRIENTE\"}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.codigo").value("CLIENTE_NO_ENCONTRADO"));
	}

	@Test
	void listarConFiltros() throws Exception {
		when(cuentas.listar(Optional.of(1L), Optional.of(EstadoCuenta.ACTIVA), new Paginacion(0, 20)))
			.thenReturn(new Pagina<>(List.of(cuenta(EstadoCuenta.ACTIVA)), 0, 20, 1, 1));

		mvc.perform(get("/api/v1/cuentas").param("clienteId", "1").param("estado", "ACTIVA"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contenido[0].id").value(10))
			.andExpect(jsonPath("$.totalElementos").value(1));
	}

	@Test
	void listarConEstadoDesconocidoDevuelve400() throws Exception {
		mvc.perform(get("/api/v1/cuentas").param("estado", "BLOQUEADA"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

	@Test
	void consultarPorNumeroInexistenteDevuelve404() throws Exception {
		when(cuentas.consultarPorNumero("5399999999")).thenThrow(CuentaNoEncontradaException.porNumero("5399999999"));

		mvc.perform(get("/api/v1/cuentas/numero/5399999999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.codigo").value("CUENTA_NO_ENCONTRADA"));
	}

	@Test
	void listarCuentasDeUnCliente() throws Exception {
		when(cuentas.listarPorCliente(1L)).thenReturn(List.of(cuenta(EstadoCuenta.CANCELADA)));

		mvc.perform(get("/api/v1/clientes/1/cuentas"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].estado").value("CANCELADA"));
	}

	@Test
	void patchSinExentaGmfDevuelve400() throws Exception {
		mvc.perform(patch("/api/v1/cuentas/10").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detalles[0].campo").value("exentaGmf"));
	}

	@Test
	void patchSobreCanceladaDevuelve409() throws Exception {
		when(cuentas.cambiarExencionGmf(10L, true)).thenThrow(new CuentaCanceladaException("5300000001"));

		mvc.perform(patch("/api/v1/cuentas/10").contentType(MediaType.APPLICATION_JSON).content("{\"exentaGmf\":true}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("CUENTA_CANCELADA"));
	}

	@Test
	void inactivarDevuelve200() throws Exception {
		when(cuentas.inactivar(10L)).thenReturn(cuenta(EstadoCuenta.INACTIVA));

		mvc.perform(post("/api/v1/cuentas/10/inactivar"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("INACTIVA"));
	}

	@Test
	void cancelarConSaldoDevuelve409() throws Exception {
		when(cuentas.cancelar(10L)).thenThrow(new SaldoDistintoDeCeroException("5300000001", new BigDecimal("5.00")));

		mvc.perform(post("/api/v1/cuentas/10/cancelar"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("SALDO_DISTINTO_DE_CERO"));
	}

}
