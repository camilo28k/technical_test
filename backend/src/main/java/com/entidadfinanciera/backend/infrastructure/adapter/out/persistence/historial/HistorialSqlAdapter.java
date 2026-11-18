package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.historial;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.consulta.LineaEstadoDeCuenta;
import com.entidadfinanciera.backend.application.port.out.HistorialQueryPort;
import com.entidadfinanciera.backend.domain.model.ClaseOperacion;
import com.entidadfinanciera.backend.domain.model.Sentido;

/**
 * Lee el estado de cuenta con SQL: une {@code movimiento} y {@code operacion}, resuelve la
 * contraparte y si la operación fue reversada. Usa el índice {@code (cuenta_id, operacion_id)}.
 */
@Component
class HistorialSqlAdapter implements HistorialQueryPort {

	private static final String COLUMNAS = """
			SELECT m.id AS movimiento_id, o.id AS operacion_id, o.clase, m.sentido, o.importe, o.fecha,
			       (SELECT c.numero FROM movimiento otro JOIN cuenta c ON c.id = otro.cuenta_id
			         WHERE otro.operacion_id = o.id AND otro.cuenta_id <> m.cuenta_id) AS contraparte,
			       EXISTS (SELECT 1 FROM operacion r WHERE r.original_id = o.id) AS reversada
			""";

	private final JdbcTemplate jdbc;

	HistorialSqlAdapter(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Pagina<LineaEstadoDeCuenta> movimientos(Long cuentaId, Optional<OffsetDateTime> desde,
			Optional<OffsetDateTime> hastaExclusivo, Paginacion paginacion) {
		StringBuilder filtro = new StringBuilder(
				" FROM movimiento m JOIN operacion o ON o.id = m.operacion_id WHERE m.cuenta_id = ?");
		List<Object> parametros = new ArrayList<>(List.of(cuentaId));
		desde.ifPresent(d -> {
			filtro.append(" AND o.fecha >= ?");
			parametros.add(d);
		});
		hastaExclusivo.ifPresent(h -> {
			filtro.append(" AND o.fecha < ?");
			parametros.add(h);
		});

		long total = jdbc.queryForObject("SELECT count(*)" + filtro, Long.class, parametros.toArray());
		List<Object> paginados = new ArrayList<>(parametros);
		paginados.add(paginacion.size());
		paginados.add((long) paginacion.page() * paginacion.size());
		List<LineaEstadoDeCuenta> lineas = jdbc.query(
				COLUMNAS + filtro + " ORDER BY o.fecha DESC, m.id DESC LIMIT ? OFFSET ?", HistorialSqlAdapter::linea,
				paginados.toArray());
		int paginas = (int) ((total + paginacion.size() - 1) / paginacion.size());
		return new Pagina<>(lineas, paginacion.page(), paginacion.size(), total, paginas);
	}

	private static LineaEstadoDeCuenta linea(ResultSet rs, int fila) throws SQLException {
		return new LineaEstadoDeCuenta(rs.getLong("movimiento_id"), rs.getLong("operacion_id"),
				ClaseOperacion.valueOf(rs.getString("clase")), Sentido.valueOf(rs.getString("sentido")),
				rs.getBigDecimal("importe"), rs.getObject("fecha", OffsetDateTime.class), rs.getString("contraparte"),
				rs.getBoolean("reversada"));
	}

}
