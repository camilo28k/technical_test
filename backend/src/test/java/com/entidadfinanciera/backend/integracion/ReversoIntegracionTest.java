package com.entidadfinanciera.backend.integracion;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.jdbc.core.JdbcTemplate;

import com.entidadfinanciera.backend.TestcontainersConfiguration;

/** Reversos contra PostgreSQL real: CA-09 y la unicidad ante solicitudes simultáneas (etapa 6). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ReversoIntegracionTest {

	@Value("${local.server.port}")
	private int puerto;

	@Autowired
	private JdbcTemplate jdbc;

	private final HttpClient http = HttpClient.newHttpClient();

	private long idA;

	private long idB;

	private String numeroA;

	private String numeroB;

	@BeforeEach
	void preparar() throws Exception {
		jdbc.execute("DELETE FROM movimiento");
		jdbc.execute("DELETE FROM operacion");
		jdbc.execute("DELETE FROM cuenta");
		jdbc.execute("DELETE FROM cliente");
		long clienteId = id(enviar("POST", "/api/v1/clientes", """
				{"tipoIdentificacion":"CC","numeroIdentificacion":"999","nombres":"Ana","apellido":"Gómez",
				 "correo":"ana@correo.com","fechaNacimiento":"1990-01-01"}
				"""));
		HttpResponse<String> a = enviar("POST", "/api/v1/cuentas", "{\"clienteId\":" + clienteId + ",\"tipoCuenta\":\"AHORROS\"}");
		HttpResponse<String> b = enviar("POST", "/api/v1/cuentas", "{\"clienteId\":" + clienteId + ",\"tipoCuenta\":\"CORRIENTE\"}");
		idA = id(a);
		idB = id(b);
		numeroA = a.body().replaceAll("(?s).*\"numero\":\"(\\d+)\".*", "$1");
		numeroB = b.body().replaceAll("(?s).*\"numero\":\"(\\d+)\".*", "$1");
		consignar(numeroA, "100.00");
		consignar(numeroB, "20.00");
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

	private HttpResponse<String> consignar(String numero, String importe) throws Exception {
		return enviar("POST", "/api/v1/operaciones/consignaciones",
				"{\"numeroCuenta\":\"" + numero + "\",\"importe\":\"" + importe + "\"}");
	}

	private long transferir(String origen, String destino, String importe) throws Exception {
		return id(enviar("POST", "/api/v1/operaciones/transferencias", "{\"numeroCuentaOrigen\":\"" + origen
				+ "\",\"numeroCuentaDestino\":\"" + destino + "\",\"importe\":\"" + importe + "\"}"));
	}

	private HttpResponse<String> reversar(long operacionId) throws Exception {
		return enviar("POST", "/api/v1/operaciones/" + operacionId + "/reverso", null);
	}

	private BigDecimal saldo(long id) {
		return jdbc.queryForObject("SELECT saldo FROM cuenta WHERE id = ?", BigDecimal.class, id);
	}

	private long contar(String sql) {
		return jdbc.queryForObject(sql, Long.class);
	}

	/** El saldo materializado coincide con créditos menos débitos (documento 03). */
	private void saldosCoincidenConElHistorial() {
		for (long cuenta : List.of(idA, idB)) {
			BigDecimal historial = jdbc.queryForObject("""
					SELECT coalesce(sum(CASE m.sentido WHEN 'CREDITO' THEN o.importe ELSE -o.importe END), 0)
					FROM movimiento m JOIN operacion o ON o.id = m.operacion_id WHERE m.cuenta_id = ?
					""", BigDecimal.class, cuenta);
			assertThat(saldo(cuenta)).isEqualByComparingTo(historial);
		}
	}

	@Test
	void reversarUnaTransferenciaRestauraAmbosSaldosYConservaElOriginal() throws Exception {
		long transferencia = transferir(numeroA, numeroB, "30.00");

		HttpResponse<String> reverso = reversar(transferencia);

		assertThat(reverso.statusCode()).isEqualTo(201);
		assertThat(reverso.body()).contains("\"clase\":\"REVERSO\"", "\"operacionOriginalId\":" + transferencia,
				"\"importe\":\"30.00\"");
		assertThat(saldo(idA)).isEqualByComparingTo("100.00");
		assertThat(saldo(idB)).isEqualByComparingTo("20.00");
		assertThat(contar("SELECT count(*) FROM operacion WHERE id = " + transferencia)).isEqualTo(1);
		assertThat(contar("SELECT count(*) FROM movimiento WHERE operacion_id = " + transferencia)).isEqualTo(2);
		saldosCoincidenConElHistorial();
	}

	@Test
	void segundoReversoYReversoDeUnReversoSeRechazan() throws Exception {
		long consignacion = id(consignar(numeroA, "5.00"));
		long reverso = id(reversar(consignacion));

		HttpResponse<String> repetido = reversar(consignacion);
		HttpResponse<String> deReverso = reversar(reverso);

		assertThat(repetido.statusCode()).isEqualTo(409);
		assertThat(repetido.body()).contains("OPERACION_YA_REVERSADA");
		assertThat(deReverso.statusCode()).isEqualTo(409);
		assertThat(deReverso.body()).contains("REVERSO_NO_REVERSABLE");
		assertThat(reversar(999999).statusCode()).isEqualTo(404);
		assertThat(saldo(idA)).isEqualByComparingTo("100.00");
	}

	@Test
	void reversoRechazadoNoAlteraSaldosNiHistorial() throws Exception {
		long transferencia = transferir(numeroA, numeroB, "30.00");
		enviar("POST", "/api/v1/operaciones/retiros", "{\"numeroCuenta\":\"" + numeroB + "\",\"importe\":\"50.00\"}");
		long movimientos = contar("SELECT count(*) FROM movimiento");

		HttpResponse<String> sinFondos = reversar(transferencia);
		enviar("POST", "/api/v1/cuentas/" + idA + "/inactivar", null);
		consignar(numeroB, "30.00");
		HttpResponse<String> inactiva = reversar(transferencia);

		assertThat(sinFondos.body()).contains("FONDOS_INSUFICIENTES");
		assertThat(inactiva.body()).contains("CUENTA_NO_ACTIVA");
		assertThat(saldo(idA)).isEqualByComparingTo("70.00");
		assertThat(saldo(idB)).isEqualByComparingTo("30.00");
		assertThat(contar("SELECT count(*) FROM movimiento")).isEqualTo(movimientos + 1);
		assertThat(contar("SELECT count(*) FROM operacion WHERE clase = 'REVERSO'")).isZero();
		saldosCoincidenConElHistorial();
	}

	@Test
	void dosReversosSimultaneosDelMismoOriginalProducenComoMaximoUno() throws Exception {
		long transferencia = transferir(numeroA, numeroB, "30.00");
		CountDownLatch salida = new CountDownLatch(1);
		List<HttpResponse<String>> respuestas;
		try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
			List<CompletableFuture<HttpResponse<String>>> tareas = List.of(1, 2)
				.stream()
				.map(i -> CompletableFuture.supplyAsync(() -> {
					try {
						salida.await();
						return reversar(transferencia);
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
			.satisfies(r -> assertThat(r.body()).contains("OPERACION_YA_REVERSADA"));
		assertThat(contar("SELECT count(*) FROM operacion WHERE clase = 'REVERSO'")).isEqualTo(1);
		assertThat(saldo(idA)).isEqualByComparingTo("100.00");
		assertThat(saldo(idB)).isEqualByComparingTo("20.00");
		saldosCoincidenConElHistorial();
	}

}
