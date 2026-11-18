package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.operacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.entidadfinanciera.backend.domain.model.ClaseOperacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fila inmutable de {@code operacion}: ninguna columna es actualizable. */
@Entity
@Table(name = "operacion")
public class OperacionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15, updatable = false)
	private ClaseOperacion clase;

	@Column(nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal importe;

	@Column(nullable = false, updatable = false)
	private OffsetDateTime fecha;

	@Column(name = "original_id", updatable = false)
	private Long originalId;

	protected OperacionEntity() {
	}

	OperacionEntity(ClaseOperacion clase, BigDecimal importe, OffsetDateTime fecha, Long originalId) {
		this.clase = clase;
		this.importe = importe;
		this.fecha = fecha;
		this.originalId = originalId;
	}

	public Long getId() {
		return id;
	}

	public ClaseOperacion getClase() {
		return clase;
	}

	public BigDecimal getImporte() {
		return importe;
	}

	public OffsetDateTime getFecha() {
		return fecha;
	}

	public Long getOriginalId() {
		return originalId;
	}

}
