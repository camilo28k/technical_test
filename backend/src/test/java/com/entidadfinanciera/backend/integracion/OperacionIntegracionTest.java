package com.entidadfinanciera.backend.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * Consignaciones y retiros contra PostgreSQL real: CA-06, CA-08, CA-14 y la serialización de
 * retiros concurrentes del plan (etapa 4).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OperacionIntegracionTest {

	@Value("${local.server.port}")
	private int puerto;

	@Autowired
	private JdbcTemplate jdbc;

	private final HttpClient http = HttpClient.newHttpClient();

	private long cuentaId;

	private String numero;

	@BeforeEach
	void preparar() throws Exception {
		jdbc.execute("DELETE FROM movimiento");
		jdbc.execute("DELETE FROM operacion");
		jdbc.execute("DELETE FROM cuenta");
		jdbc.execute("DELETE FROM cliente");
		long clienteId = id(enviar("POST", "/api/v1/clientes", """
				{"tipoIdentificacion":"CC","numeroIdentificacion":"777","nombres":"Ana","apellido":"Gómez",
				 "correo":"ana@correo.com","fechaNacimiento":"1990-01-01"}
				"""));
		HttpResponse<String> cuenta = enviar("POST", "/api/v1/cuentas",
				"{\"clienteId\":" + clienteId + ",\"tipoCuenta\":\"AHORROS\"}");
		cuentaId = id(cuenta);
		numero = cuenta.body().replaceAll("(?s).*\"numero\":\"(\\d+)\".*", "$1");
	}

	private HttpResponse<String> enviar(String metodo, String ruta, String cuerpo) throws Exception {
		HttpRequest.Builder solicitud = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
			.header("Content-Type", "application/json");
		solicitud.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
				: HttpRequest.BodyPublishers.ofString(cuerpo));
		return http.send(solicitud.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static long id(HttpResponse<String> respuesta) {
		return Long.parseLong(respuesta.body().replaceAll("(?s)^\\{\"id\":(\\d+).*", "$1"));
	}

	private HttpResponse<String> operar(String tipo, String importe) throws Exception {
		return enviar("POST", "/api/v1/operaciones/" + tipo,
				"{\"numeroCuenta\":\"" + numero + "\",\"importe\":\"" + importe + "\"}");
	}

	private BigDecimal saldo() {
		return jdbc.queryForObject("SELECT saldo FROM cuenta WHERE id = ?", BigDecimal.class, cuentaId);
	}

	private long movimientos() {
		return jdbc.queryForObject("SELECT count(*) FROM movimiento WHERE cuenta_id = ?", Long.class, cuentaId);
	}

	@Test
	void consignacionPosteriorALaAperturaCreaSuPropioMovimiento() throws Exception {
		assertThat(movimientos()).isZero();

		HttpResponse<String> consignacion = operar("consignaciones", "150000");

		assertThat(consignacion.statusCode()).isEqualTo(201);
		assertThat(consignacion.body()).contains("\"clase\":\"CONSIGNACION\"", "\"importe\":\"150000.00\"",
				"\"sentido\":\"CREDITO\"", "\"saldoResultante\":\"150000.00\"");
		assertThat(saldo()).isEqualByComparingTo("150000.00");
		assertThat(movimientos()).isEqualTo(1);
	}

	@Test
	void retiroReduceElSaldoYSinFondosNoAlteraNada() throws Exception {
		operar("consignaciones", "100.00");

		assertThat(operar("retiros", "30.50").body()).contains("\"saldoResultante\":\"69.50\"");

		HttpResponse<String> sinFondos = operar("retiros", "69.51");
		assertThat(sinFondos.statusCode()).isEqualTo(409);
		assertThat(sinFondos.body()).contains("FONDOS_INSUFICIENTES");
		assertThat(saldo()).isEqualByComparingTo("69.50");
		assertThat(movimientos()).isEqualTo(2);
	}

	@Test
	void cuentaInactivaNoEmiteNiRecibeMovimientos() throws Exception {
		operar("consignaciones", "50.00");
		enviar("POST", "/api/v1/cuentas/" + cuentaId + "/inactivar", null);

		assertThat(operar("consignaciones", "10.00").body()).contains("CUENTA_NO_ACTIVA");
		assertThat(operar("retiros", "10.00").body()).contains("CUENTA_NO_ACTIVA");
		assertThat(saldo()).isEqualByComparingTo("50.00");
		assertThat(movimientos()).isEqualTo(1);
	}

	@Test
	void importeInvalidoSeRechazaSinEfectos() throws Exception {
		assertThat(operar("consignaciones", "0").statusCode()).isEqualTo(400);
		assertThat(operar("consignaciones", "1.999").statusCode()).isEqualTo(400);
		assertThat(movimientos()).isZero();
	}

	@Test
	void noSeCancelaUnaCuentaConSaldo() throws Exception {
		operar("consignaciones", "1.00");

		HttpResponse<String> cancelar = enviar("POST", "/api/v1/cuentas/" + cuentaId + "/cancelar", null);

		assertThat(cancelar.statusCode()).isEqualTo(409);
		assertThat(cancelar.body()).contains("SALDO_DISTINTO_DE_CERO");
		assertThat(jdbc.queryForObject("SELECT estado FROM cuenta WHERE id = ?", String.class, cuentaId))
			.isEqualTo("ACTIVA");
	}

	@Test
	void dosRetirosConcurrentesDe80SobreSaldo100SoloConfirmanUno() throws Exception {
		operar("consignaciones", "100.00");
		CountDownLatch salida = new CountDownLatch(1);
		List<HttpResponse<String>> respuestas;
		try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
			List<CompletableFuture<HttpResponse<String>>> tareas = List.of(1, 2)
				.stream()
				.map(i -> CompletableFuture.supplyAsync(() -> {
					try {
						salida.await();
						return operar("retiros", "80.00");
					}
					catch (Exception ex) {
						throw new IllegalStateException(ex);
					}
				}, hilos))
				.toList();
			salida.countDown();
			respuestas = tareas.stream().map(CompletableFuture::join).toList();
		}

		assertThat(respuestas).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(201, 409);
		assertThat(respuestas).filteredOn(r -> r.statusCode() == 409)
			.singleElement()
			.satisfies(r -> assertThat(r.body()).contains("FONDOS_INSUFICIENTES"));
		assertThat(saldo()).isEqualByComparingTo("20.00");
		assertThat(movimientos()).isEqualTo(2);
	}

	@Test
	void laBaseDeDatosProtegeElHistorial() {
		assertThatThrownBy(() -> jdbc.update("INSERT INTO operacion (clase, importe) VALUES ('CONSIGNACION', 0)"))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("INSERT INTO operacion (clase, importe) VALUES ('REVERSO', 5)"))
			.isInstanceOf(DataIntegrityViolationException.class);
		long op = jdbc.queryForObject(
				"INSERT INTO operacion (clase, importe) VALUES ('CONSIGNACION', 5) RETURNING id", Long.class);
		jdbc.update("INSERT INTO movimiento (operacion_id, cuenta_id, sentido) VALUES (?, ?, 'CREDITO')", op, cuentaId);
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO movimiento (operacion_id, cuenta_id, sentido) VALUES (?, ?, 'DEBITO')", op, cuentaId))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("DELETE FROM cuenta WHERE id = ?", cuentaId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
