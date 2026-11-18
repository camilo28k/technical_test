package com.entidadfinanciera.backend.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.entidadfinanciera.backend.TestcontainersConfiguration;
import com.entidadfinanciera.backend.application.port.out.OperacionRepositoryPort;
import com.entidadfinanciera.backend.domain.model.ClaseOperacion;

/**
 * Transferencias contra PostgreSQL real: CA-07, CA-08 y el fallo provocado después del débito
 * que exige el plan (etapa 5) para demostrar la atomicidad.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TransferenciaIntegracionTest {

	@Value("${local.server.port}")
	private int puerto;

	@Autowired
	private JdbcTemplate jdbc;

	/** Espía del historial: se comporta como el real salvo cuando una prueba le ordena fallar. */
	@MockitoSpyBean
	private OperacionRepositoryPort historial;

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
				{"tipoIdentificacion":"CC","numeroIdentificacion":"888","nombres":"Ana","apellido":"Gómez",
				 "correo":"ana@correo.com","fechaNacimiento":"1990-01-01"}
				"""));
		HttpResponse<String> a = enviar("POST", "/api/v1/cuentas", "{\"clienteId\":" + clienteId + ",\"tipoCuenta\":\"AHORROS\"}");
		HttpResponse<String> b = enviar("POST", "/api/v1/cuentas", "{\"clienteId\":" + clienteId + ",\"tipoCuenta\":\"CORRIENTE\"}");
		idA = id(a);
		idB = id(b);
		numeroA = numero(a);
		numeroB = numero(b);
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

	private static String numero(HttpResponse<String> respuesta) {
		return respuesta.body().replaceAll("(?s).*\"numero\":\"(\\d+)\".*", "$1");
	}

	private void consignar(String numero, String importe) throws Exception {
		enviar("POST", "/api/v1/operaciones/consignaciones",
				"{\"numeroCuenta\":\"" + numero + "\",\"importe\":\"" + importe + "\"}");
	}

	private HttpResponse<String> transferir(String origen, String destino, String importe) throws Exception {
		return enviar("POST", "/api/v1/operaciones/transferencias", "{\"numeroCuentaOrigen\":\"" + origen
				+ "\",\"numeroCuentaDestino\":\"" + destino + "\",\"importe\":\"" + importe + "\"}");
	}

	private BigDecimal saldo(long id) {
		return jdbc.queryForObject("SELECT saldo FROM cuenta WHERE id = ?", BigDecimal.class, id);
	}

	private long movimientos() {
		return jdbc.queryForObject("SELECT count(*) FROM movimiento", Long.class);
	}

	@Test
	void transferenciaDebitaOrigenAcreditaDestinoEnUnaOperacion() throws Exception {
		HttpResponse<String> respuesta = transferir(numeroA, numeroB, "30.00");

		assertThat(respuesta.statusCode()).isEqualTo(201);
		assertThat(respuesta.body()).contains("\"clase\":\"TRANSFERENCIA\"", "\"saldoResultante\":\"70.00\"",
				"\"saldoResultante\":\"50.00\"");
		assertThat(saldo(idA)).isEqualByComparingTo("70.00");
		assertThat(saldo(idB)).isEqualByComparingTo("50.00");
		assertThat(jdbc.queryForList("""
				SELECT m.sentido FROM movimiento m JOIN operacion o ON o.id = m.operacion_id
				WHERE o.clase = 'TRANSFERENCIA' ORDER BY m.cuenta_id
				""", String.class)).containsExactly("DEBITO", "CREDITO");
	}

	@Test
	void unFalloDespuesDelDebitoNoDejaEfectosParciales() throws Exception {
		doThrow(new IllegalStateException("fallo simulado al escribir el historial")).when(historial)
			.registrar(argThat(o -> o.clase() == ClaseOperacion.TRANSFERENCIA));
		long antes = movimientos();

		HttpResponse<String> respuesta = transferir(numeroA, numeroB, "30.00");

		assertThat(respuesta.statusCode()).isEqualTo(500);
		assertThat(respuesta.body()).contains("ERROR_INTERNO").doesNotContain("fallo simulado");
		assertThat(saldo(idA)).isEqualByComparingTo("100.00");
		assertThat(saldo(idB)).isEqualByComparingTo("20.00");
		assertThat(movimientos()).isEqualTo(antes);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM operacion WHERE clase = 'TRANSFERENCIA'", Long.class))
			.isZero();
	}

	@Test
	void transferenciaALaMismaCuentaOSinFondosSeRechazaSinEfectos() throws Exception {
		assertThat(transferir(numeroA, numeroA, "1.00").body()).contains("MISMA_CUENTA");
		assertThat(transferir(numeroA, numeroB, "100.01").body()).contains("FONDOS_INSUFICIENTES");

		assertThat(saldo(idA)).isEqualByComparingTo("100.00");
		assertThat(saldo(idB)).isEqualByComparingTo("20.00");
		assertThat(movimientos()).isEqualTo(2);
	}

	@Test
	void destinoUOrigenInactivoBloqueanLaTransferencia() throws Exception {
		enviar("POST", "/api/v1/cuentas/" + idB + "/inactivar", null);

		HttpResponse<String> haciaInactiva = transferir(numeroA, numeroB, "10.00");
		HttpResponse<String> desdeInactiva = transferir(numeroB, numeroA, "10.00");

		assertThat(haciaInactiva.statusCode()).isEqualTo(409);
		assertThat(haciaInactiva.body()).contains("CUENTA_NO_ACTIVA");
		assertThat(desdeInactiva.body()).contains("CUENTA_NO_ACTIVA");
		assertThat(saldo(idA)).isEqualByComparingTo("100.00");
		assertThat(saldo(idB)).isEqualByComparingTo("20.00");
	}

	@Test
	void transferenciasCruzadasSimultaneasNoSeInterbloquean() throws Exception {
		CountDownLatch salida = new CountDownLatch(1);
		List<HttpResponse<String>> respuestas;
		try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
			List<CompletableFuture<HttpResponse<String>>> tareas = List
				.of(new String[] { numeroA, numeroB }, new String[] { numeroB, numeroA })
				.stream()
				.map(par -> CompletableFuture.supplyAsync(() -> {
					try {
						salida.await();
						return transferir(par[0], par[1], "10.00");
					}
					catch (Exception ex) {
						throw new IllegalStateException(ex);
					}
				}, hilos))
				.toList();
			salida.countDown();
			respuestas = tareas.stream().map(CompletableFuture::join).toList();
		}

		assertThat(respuestas).extracting(HttpResponse::statusCode).containsOnly(201);
		assertThat(saldo(idA)).isEqualByComparingTo("100.00");
		assertThat(saldo(idB)).isEqualByComparingTo("20.00");
		assertThat(movimientos()).isEqualTo(6);
	}

}
