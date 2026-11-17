package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cliente;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.port.in.GestionarClientesUseCase;
import com.entidadfinanciera.backend.domain.exception.ClienteMenorDeEdadException;
import com.entidadfinanciera.backend.domain.exception.ClienteNoEncontradoException;
import com.entidadfinanciera.backend.domain.exception.IdentificacionDuplicadaException;
import com.entidadfinanciera.backend.domain.model.Cliente;
import com.entidadfinanciera.backend.domain.model.DatosCliente;
import com.entidadfinanciera.backend.domain.model.TipoIdentificacion;
import com.entidadfinanciera.backend.infrastructure.adapter.in.rest.comun.ManejadorDeErrores;

@WebMvcTest(ClienteController.class)
@Import({ ManejadorDeErrores.class, ClienteControllerTest.RelojDePrueba.class })
class ClienteControllerTest {

	private static final String CUERPO_VALIDO = """
			{"tipoIdentificacion":"CC","numeroIdentificacion":"1075123456","nombres":"Ana María",
			 "apellido":"Gómez","correo":"ana.gomez@correo.com","fechaNacimiento":"1995-04-12"}
			""";

	@TestConfiguration
	static class RelojDePrueba {

		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-11-16T15:30:00Z"), ZoneId.of("America/Bogota"));
		}

	}

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private GestionarClientesUseCase clientes;

	private static Cliente cliente() {
		return Cliente.reconstruir(1L,
				new DatosCliente(TipoIdentificacion.CC, "1075123456", "Ana María", "Gómez", "ana.gomez@correo.com",
						LocalDate.of(1995, 4, 12)),
				OffsetDateTime.parse("2026-11-16T15:30:00Z"), null);
	}

	@Test
	void registrarDevuelve201ConLocationYFechaEnHoraDeColombia() throws Exception {
		when(clientes.registrar(any())).thenReturn(cliente());

		mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/v1/clientes/1"))
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.tipoIdentificacion").value("CC"))
			.andExpect(jsonPath("$.fechaNacimiento").value("1995-04-12"))
			.andExpect(jsonPath("$.fechaCreacion").value("2026-11-16T10:30:00-05:00"))
			.andExpect(jsonPath("$.fechaModificacion").doesNotExist());
	}

	@Test
	void registrarConCamposInvalidosDevuelve400ConDetalles() throws Exception {
		String invalido = """
				{"tipoIdentificacion":"NIT","numeroIdentificacion":"1","nombres":"A",
				 "apellido":"Gómez","correo":"sin-arroba","fechaNacimiento":"1995-04-12"}
				""";

		mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(invalido))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.codigo").value("VALIDACION"))
			.andExpect(jsonPath("$.ruta").value("/api/v1/clientes"))
			.andExpect(jsonPath("$.detalles.length()").value(3));
		verify(clientes, never()).registrar(any());
	}

	@Test
	void jsonMalFormadoDevuelveCuerpoIlegible() throws Exception {
		mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content("{"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.codigo").value("CUERPO_ILEGIBLE"));
	}

	@Test
	void menorDeEdadDevuelve409() throws Exception {
		when(clientes.registrar(any())).thenThrow(new ClienteMenorDeEdadException());

		mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("CLIENTE_MENOR_DE_EDAD"));
	}

	@Test
	void identificacionDuplicadaDevuelve409() throws Exception {
		when(clientes.registrar(any()))
			.thenThrow(new IdentificacionDuplicadaException(TipoIdentificacion.CC, "1075123456"));

		mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.codigo").value("IDENTIFICACION_DUPLICADA"));
	}

	@Test
	void consultarInexistenteDevuelve404() throws Exception {
		when(clientes.consultar(9L)).thenThrow(new ClienteNoEncontradoException(9L));

		mvc.perform(get("/api/v1/clientes/9"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.codigo").value("CLIENTE_NO_ENCONTRADO"));
	}

	@Test
	void listarPasaFiltroYPaginacion() throws Exception {
		when(clientes.listar(Optional.of("1075123456"), new Paginacion(0, 5)))
			.thenReturn(new Pagina<>(List.of(cliente()), 0, 5, 1, 1));

		mvc.perform(get("/api/v1/clientes").param("numeroIdentificacion", "1075123456").param("size", "5"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contenido[0].numeroIdentificacion").value("1075123456"))
			.andExpect(jsonPath("$.totalElementos").value(1))
			.andExpect(jsonPath("$.size").value(5));
	}

	@Test
	void listarConTamanoMayorAlMaximoDevuelve400() throws Exception {
		mvc.perform(get("/api/v1/clientes").param("size", "101"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

	@Test
	void actualizarDevuelve200() throws Exception {
		when(clientes.actualizar(eq(1L), any())).thenReturn(cliente());

		mvc.perform(put("/api/v1/clientes/1").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void eliminarDevuelve204() throws Exception {
		mvc.perform(delete("/api/v1/clientes/1")).andExpect(status().isNoContent());
		verify(clientes).eliminar(1L);
	}

	@Test
	void eliminarInexistenteDevuelve404() throws Exception {
		doThrow(new ClienteNoEncontradoException(3L)).when(clientes).eliminar(3L);

		mvc.perform(delete("/api/v1/clientes/3")).andExpect(status().isNotFound());
	}

}
