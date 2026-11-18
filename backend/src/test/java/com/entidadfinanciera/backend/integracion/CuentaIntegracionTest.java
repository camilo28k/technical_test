package com.entidadfinanciera.backend.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
 * Cuentas contra PostgreSQL real: CA-03 a CA-05, CA-10, CA-12 y la apertura de CA-14, más las
 * restricciones que protegen la tabla ante escrituras directas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CuentaIntegracionTest {

	@Value("${local.server.port}")
	private int puerto;

	@Autowired
	private JdbcTemplate jdbc;

	private final HttpClient http = HttpClient.newHttpClient();

	private long clienteId;

	@BeforeEach
	void preparar() throws Exception {
		jdbc.execute("DELETE FROM movimiento");
		jdbc.execute("DELETE FROM operacion");
		jdbc.execute("DELETE FROM cuenta");
		jdbc.execute("DELETE FROM cliente");
		clienteId = id(enviar("POST", "/api/v1/clientes", """
				{"tipoIdentificacion":"CC","numeroIdentificacion":"555","nombres":"Ana","apellido":"Gómez",
				 "correo":"ana@correo.com","fechaNacimiento":"1990-01-01"}
				"""));
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

	private static String campo(HttpResponse<String> respuesta, String nombre) {
		return respuesta.body().replaceAll("(?s).*\"" + nombre + "\":\"?([^\",}]*)\"?.*", "$1");
	}

	private HttpResponse<String> abrir(String tipo) throws Exception {
		return enviar("POST", "/api/v1/cuentas", "{\"clienteId\":" + clienteId + ",\"tipoCuenta\":\"" + tipo + "\"}");
	}

	@Test
	void abreCuentasConPrefijoNumeroUnicoYSaldoCero() throws Exception {
		HttpResponse<String> ahorros = abrir("AHORROS");
		HttpResponse<String> otraAhorros = abrir("AHORROS");
		HttpResponse<String> corriente = abrir("CORRIENTE");

		assertThat(ahorros.statusCode()).isEqualTo(201);
		assertThat(campo(ahorros, "numero")).matches("53\\d{8}");
		assertThat(campo(otraAhorros, "numero")).matches("53\\d{8}").isNotEqualTo(campo(ahorros, "numero"));
		assertThat(campo(corriente, "numero")).matches("33\\d{8}");
		assertThat(ahorros.body()).contains("\"estado\":\"ACTIVA\"", "\"saldo\":\"0.00\"", "\"exentaGmf\":false",
				"-05:00");

		HttpResponse<String> porNumero = enviar("GET", "/api/v1/cuentas/numero/" + campo(corriente, "numero"), null);
		assertThat(porNumero.statusCode()).isEqualTo(200);
		assertThat(campo(porNumero, "tipoCuenta")).isEqualTo("CORRIENTE");
	}

	@Test
	void noAbreCuentaParaClienteInexistente() throws Exception {
		HttpResponse<String> respuesta = enviar("POST", "/api/v1/cuentas", "{\"clienteId\":999999,\"tipoCuenta\":\"AHORROS\"}");

		assertThat(respuesta.statusCode()).isEqualTo(404);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM cuenta", Long.class)).isZero();
	}

	@Test
	void recorreLasTransicionesYLaCanceladaEsDefinitiva() throws Exception {
		long id = id(abrir("AHORROS"));

		assertThat(campo(enviar("POST", "/api/v1/cuentas/" + id + "/inactivar", null), "estado")).isEqualTo("INACTIVA");
		assertThat(campo(enviar("POST", "/api/v1/cuentas/" + id + "/activar", null), "estado")).isEqualTo("ACTIVA");
		assertThat(campo(enviar("POST", "/api/v1/cuentas/" + id + "/cancelar", null), "estado")).isEqualTo("CANCELADA");

		HttpResponse<String> reactivar = enviar("POST", "/api/v1/cuentas/" + id + "/activar", null);
		assertThat(reactivar.statusCode()).isEqualTo(409);
		assertThat(reactivar.body()).contains("CUENTA_CANCELADA");
		assertThat(jdbc.queryForObject("SELECT estado FROM cuenta WHERE id = ?", String.class, id))
			.isEqualTo("CANCELADA");
	}

	@Test
	void actualizarGmfConservaElValorYMarcaLaModificacion() throws Exception {
		long id = id(abrir("CORRIENTE"));

		HttpResponse<String> cambio = enviar("PATCH", "/api/v1/cuentas/" + id, "{\"exentaGmf\":true}");

		assertThat(cambio.statusCode()).isEqualTo(200);
		assertThat(cambio.body()).contains("\"exentaGmf\":true").doesNotContain("\"fechaModificacion\":null");
		assertThat(jdbc.queryForObject("SELECT exenta_gmf FROM cuenta WHERE id = ?", Boolean.class, id)).isTrue();
	}

	@Test
	void clienteConCuentaCanceladaSigueProtegido() throws Exception {
		long id = id(abrir("AHORROS"));
		enviar("POST", "/api/v1/cuentas/" + id + "/cancelar", null);

		HttpResponse<String> eliminar = enviar("DELETE", "/api/v1/clientes/" + clienteId, null);

		assertThat(eliminar.statusCode()).isEqualTo(409);
		assertThat(eliminar.body()).contains("CLIENTE_CON_PRODUCTOS");
		assertThat(enviar("GET", "/api/v1/clientes/" + clienteId + "/cuentas", null).body()).contains("CANCELADA");
	}

	@Test
	void laBaseDeDatosProtegeLasReglasDeLaCuenta() {
		String insertar = "INSERT INTO cuenta (cliente_id, tipo, numero, estado, saldo) VALUES (?, ?, ?, ?, ?)";

		// Prefijo que no corresponde al tipo (RN-06).
		assertThatThrownBy(() -> jdbc.update(insertar, clienteId, "AHORROS", "3300000001", "ACTIVA", 0))
			.isInstanceOf(DataIntegrityViolationException.class);
		// Saldo negativo (RN-05).
		assertThatThrownBy(() -> jdbc.update(insertar, clienteId, "AHORROS", "5390000001", "ACTIVA", -1))
			.isInstanceOf(DataIntegrityViolationException.class);
		// Cancelada con saldo (RN-08).
		assertThatThrownBy(() -> jdbc.update(insertar, clienteId, "AHORROS", "5390000002", "CANCELADA", 10))
			.isInstanceOf(DataIntegrityViolationException.class);
		// Propietario inexistente (RN-04).
		assertThatThrownBy(() -> jdbc.update(insertar, 999999L, "AHORROS", "5390000003", "ACTIVA", 0))
			.isInstanceOf(DataIntegrityViolationException.class);
		// Eliminación directa del propietario (RN-02).
		jdbc.update(insertar, clienteId, "AHORROS", "5390000004", "ACTIVA", 0);
		assertThatThrownBy(() -> jdbc.update("DELETE FROM cliente WHERE id = ?", clienteId))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("fk_cuenta_cliente");
	}

}
