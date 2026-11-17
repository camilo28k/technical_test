package com.entidadfinanciera.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.ZoneId;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba de cierre de la etapa 1: la aplicación arranca contra PostgreSQL real,
 * Flyway se ejecuta y el endpoint de salud responde.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BackendApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Value("${local.server.port}")
	private int puerto;

	@Autowired
	private Clock clock;

	@Test
	void conectaConPostgresql() throws Exception {
		try (var conexion = dataSource.getConnection()) {
			assertThat(conexion.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
		}
		assertThat(new JdbcTemplate(dataSource).queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
	}

	@Test
	void flywayRegistraSuHistorial() {
		Integer tablas = new JdbcTemplate(dataSource).queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_name = 'flyway_schema_history'",
				Integer.class);
		assertThat(tablas).isEqualTo(1);
	}

	@Test
	void endpointDeSaludRespondeUp() throws Exception {
		HttpRequest solicitud = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/actuator/health")).build();
		HttpResponse<String> respuesta = HttpClient.newHttpClient().send(solicitud, HttpResponse.BodyHandlers.ofString());
		assertThat(respuesta.statusCode()).isEqualTo(200);
		assertThat(respuesta.body()).contains("\"status\":\"UP\"");
	}

	@Test
	void relojUsaZonaHorariaDeColombia() {
		assertThat(clock.getZone()).isEqualTo(ZoneId.of("America/Bogota"));
	}

}
