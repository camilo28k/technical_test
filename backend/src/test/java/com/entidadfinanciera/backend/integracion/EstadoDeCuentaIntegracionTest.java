package com.entidadfinanciera.backend.integracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.entidadfinanciera.backend.TestcontainersConfiguration;

/** Consultas contra PostgreSQL real: CA-11 (etapa 7). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class EstadoDeCuentaIntegracionTest {

	@Value("${local.server.port}")
	private int puerto;

	@Autowired
	private JdbcTemplate jdbc;

	private final HttpClient http = HttpClient.newHttpClient();

	private long idA;

	private String numeroA;

	private String numeroB;

	@BeforeEach
	void preparar() throws Exception {
		jdbc.execute("DELETE FROM movimiento");
		jdbc.execute("DELETE FROM operacion");
		jdbc.execute("DELETE FROM cuenta");
		jdbc.execute("DELETE FROM cliente");
		long clienteId = id(enviar("POST", "/api/v1/clientes", """
				{"tipoIdentificacion":"CC","numeroIdentificacion":"321","nombres":"Ana María","apellido":"Gómez",
				 "correo":"ana@correo.com","fechaNacimiento":"1990-01-01"}
				"""));
		HttpResponse<String> a = enviar("POST", "/api/v1/cuentas", "{\"clienteId\":" + clienteId + ",\"tipoCuenta\":\"AHORROS\"}");
		HttpResponse<String> b = enviar("POST", "/api/v1/cuentas", "{\"clienteId\":" + clienteId + ",\"tipoCuenta\":\"CORRIENTE\"}");
		idA = id(a);
		numeroA = a.body().replaceAll("(?s).*\"numero\":\"(\\d+)\".*", "$1");
		numeroB = b.body().replaceAll("(?s).*\"numero\":\"(\\d+)\".*", "$1");
	}

	private HttpResponse<String> enviar(String metodo, String ruta, String cuerpo) throws Exception {
		HttpRequest.Builder solicitud = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
			.header("Content-Type", "application/json; charset=utf-8");
		solicitud.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
				: HttpRequest.BodyPublishers.ofString(cuerpo));
		return http.send(solicitud.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static long id(HttpResponse<String> respuesta) {
		return Long.parseLong(respuesta.body().replaceAll("(?s)^\\{\"id\":(\\d+).*", "$1"));
	}

	private HttpResponse<String> estado(String parametros) throws Exception {
		return enviar("GET", "/api/v1/cuentas/" + idA + "/estado-de-cuenta" + parametros, null);
	}

	@Test
	void unaCuentaRecienAbiertaTieneHistorialVacioYSaldoCero() throws Exception {
		HttpResponse<String> respuesta = estado("");

		assertThat(respuesta.statusCode()).isEqualTo(200);
		assertThat(respuesta.body()).contains("\"saldo\":\"0.00\"", "\"contenido\":[]", "\"totalElementos\":0",
				"\"titular\":\"Ana María Gómez\"", "\"numero\":\"" + numeroA + "\"");
	}

	@Test
	void historialConTransferenciaYReversoOrdenadoDelMasReciente() throws Exception {
		enviar("POST", "/api/v1/operaciones/consignaciones",
				"{\"numeroCuenta\":\"" + numeroA + "\",\"importe\":\"100.00\"}");
		long transferencia = id(enviar("POST", "/api/v1/operaciones/transferencias", "{\"numeroCuentaOrigen\":\""
				+ numeroA + "\",\"numeroCuentaDestino\":\"" + numeroB + "\",\"importe\":\"30.00\"}"));
		long reverso = id(enviar("POST", "/api/v1/operaciones/" + transferencia + "/reverso", null));

		String cuerpo = estado("").body();

		assertThat(cuerpo).contains("\"saldo\":\"100.00\"", "\"totalElementos\":3");
		assertThat(cuerpo.indexOf("\"operacionId\":" + reverso))
			.isLessThan(cuerpo.indexOf("\"operacionId\":" + transferencia));
		assertThat(cuerpo).containsPattern("\"operacionId\":" + reverso
				+ ",\"clase\":\"REVERSO\",\"sentido\":\"CREDITO\",\"importe\":\"30.00\",\"fecha\":\"[^\"]+-05:00\",\"contraparte\":\""
				+ numeroB + "\",\"reversada\":false");
		assertThat(cuerpo).containsPattern("\"operacionId\":" + transferencia
				+ ",\"clase\":\"TRANSFERENCIA\",\"sentido\":\"DEBITO\".*?\"contraparte\":\"" + numeroB
				+ "\",\"reversada\":true");
		assertThat(cuerpo).containsPattern("\"clase\":\"CONSIGNACION\",\"sentido\":\"CREDITO\".*?\"contraparte\":null");

		HttpResponse<String> operacion = enviar("GET", "/api/v1/operaciones/" + transferencia, null);
		assertThat(operacion.statusCode()).isEqualTo(200);
		assertThat(operacion.body()).contains("\"numeroCuenta\":\"" + numeroA + "\"",
				"\"numeroCuenta\":\"" + numeroB + "\"");
	}

	@Test
	void filtraPorFechasDeColombiaYPagina() throws Exception {
		for (String importe : new String[] { "1.00", "2.00", "3.00" }) {
			enviar("POST", "/api/v1/operaciones/consignaciones",
					"{\"numeroCuenta\":\"" + numeroA + "\",\"importe\":\"" + importe + "\"}");
		}
		LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));

		assertThat(estado("?desde=" + hoy + "&hasta=" + hoy).body()).contains("\"totalElementos\":3");
		assertThat(estado("?desde=" + hoy.plusDays(1)).body()).contains("\"contenido\":[]", "\"saldo\":\"6.00\"");
		assertThat(estado("?hasta=" + hoy.minusDays(1)).body()).contains("\"totalElementos\":0");

		String segunda = estado("?page=1&size=2").body();
		assertThat(segunda).contains("\"importe\":\"1.00\"", "\"totalPaginas\":2").doesNotContain("\"importe\":\"3.00\"");
		assertThat(estado("?desde=" + hoy + "&hasta=" + hoy.minusDays(1)).statusCode()).isEqualTo(400);
		assertThat(enviar("GET", "/api/v1/cuentas/999999/estado-de-cuenta", null).statusCode()).isEqualTo(404);
	}

}
