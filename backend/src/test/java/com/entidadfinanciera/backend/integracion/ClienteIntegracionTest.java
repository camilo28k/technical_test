package com.entidadfinanciera.backend.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.entidadfinanciera.backend.TestcontainersConfiguration;

/**
 * Recorrido HTTP completo contra PostgreSQL real: CA-01, CA-02, CA-03 (sin cuentas) y CA-13,
 * más las restricciones que protegen la base de datos ante escrituras directas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ClienteIntegracionTest {

	@Value("${local.server.port}")
	private int puerto;

	@Autowired
	private JdbcTemplate jdbc;

	private final HttpClient http = HttpClient.newHttpClient();

	@BeforeEach
	void limpiar() {
		jdbc.execute("DELETE FROM cuenta");
		jdbc.execute("DELETE FROM cliente");
	}

	private HttpResponse<String> enviar(String metodo, String ruta, String cuerpo) throws Exception {
		HttpRequest.Builder solicitud = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
			.header("Content-Type", "application/json");
		solicitud.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
				: HttpRequest.BodyPublishers.ofString(cuerpo));
		return http.send(solicitud.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static String cliente(String tipo, String numero, String correo, String nacimiento) {
		return """
				{"tipoIdentificacion":"%s","numeroIdentificacion":"%s","nombres":"Ana",
				 "apellido":"Gómez","correo":"%s","fechaNacimiento":"%s"}
				""".formatted(tipo, numero, correo, nacimiento);
	}

	private static long id(HttpResponse<String> respuesta) {
		return Long.parseLong(respuesta.body().replaceAll("(?s).*\"id\":(\\d+).*", "$1"));
	}

	@Test
	void cicloCompletoDeUnCliente() throws Exception {
		HttpResponse<String> alta = enviar("POST", "/api/v1/clientes", cliente("CC", "100", "ana@correo.com", "1990-01-01"));
		assertThat(alta.statusCode()).isEqualTo(201);
		assertThat(alta.body()).contains("\"fechaModificacion\":null").contains("-05:00");
		long id = id(alta);

		HttpResponse<String> cambio = enviar("PUT", "/api/v1/clientes/" + id,
				cliente("CC", "100", "nuevo@correo.com", "1990-01-01"));
		assertThat(cambio.statusCode()).isEqualTo(200);
		assertThat(cambio.body()).contains("nuevo@correo.com").doesNotContain("\"fechaModificacion\":null");

		assertThat(enviar("GET", "/api/v1/clientes?numeroIdentificacion=100", null).body())
			.contains("\"totalElementos\":1");

		assertThat(enviar("DELETE", "/api/v1/clientes/" + id, null).statusCode()).isEqualTo(204);
		assertThat(enviar("GET", "/api/v1/clientes/" + id, null).statusCode()).isEqualTo(404);
	}

	@Test
	void mismaIdentificacionSeRechazaPeroOtroTipoConMismoNumeroSeAcepta() throws Exception {
		assertThat(enviar("POST", "/api/v1/clientes", cliente("CC", "12345", "a@b.co", "1990-01-01")).statusCode())
			.isEqualTo(201);

		HttpResponse<String> repetido = enviar("POST", "/api/v1/clientes", cliente("CC", "12345", "c@d.co", "1990-01-01"));
		assertThat(repetido.statusCode()).isEqualTo(409);
		assertThat(repetido.body()).contains("IDENTIFICACION_DUPLICADA");

		assertThat(enviar("POST", "/api/v1/clientes", cliente("CE", "12345", "e@f.co", "1990-01-01")).statusCode())
			.isEqualTo(201);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM cliente WHERE numero_identificacion = '12345'", Long.class))
			.isEqualTo(2);
	}

	@Test
	void menorDeEdadSeRechazaPorApi() throws Exception {
		String haceDiezAnos = LocalDate.now(ZoneId.of("America/Bogota")).minusYears(10).toString();

		HttpResponse<String> respuesta = enviar("POST", "/api/v1/clientes", cliente("CC", "7", "m@n.co", haceDiezAnos));

		assertThat(respuesta.statusCode()).isEqualTo(409);
		assertThat(respuesta.body()).contains("CLIENTE_MENOR_DE_EDAD");
		assertThat(jdbc.queryForObject("SELECT count(*) FROM cliente", Long.class)).isZero();
	}

	@Test
	void laBaseDeDatosImpideInsertarUnMenorAunqueSeEviteLaApi() {
		String haceDiezAnos = LocalDate.now(ZoneId.of("America/Bogota")).minusYears(10).toString();

		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO cliente (tipo_identificacion, numero_identificacion, nombres, apellido, correo, fecha_nacimiento)
				VALUES ('CC', '8', 'Luis', 'Paz', 'l@p.co', ?::date)
				""", haceDiezAnos)).isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_cliente_mayor_de_edad");
	}

	@Test
	void laBaseDeDatosRechazaTiposDeIdentificacionNoAdmitidos() {
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO cliente (tipo_identificacion, numero_identificacion, nombres, apellido, correo, fecha_nacimiento)
				VALUES ('NIT', '9', 'Luis', 'Paz', 'l@p.co', DATE '1990-01-01')
				""")).isInstanceOf(DataIntegrityViolationException.class);
	}

}
