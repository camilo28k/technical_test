package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.operacion;

import com.entidadfinanciera.backend.domain.model.Sentido;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fila inmutable de {@code movimiento}. */
@Entity
@Table(name = "movimiento")
public class MovimientoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "operacion_id", nullable = false, updatable = false)
	private Long operacionId;

	@Column(name = "cuenta_id", nullable = false, updatable = false)
	private Long cuentaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 7, updatable = false)
	private Sentido sentido;

	protected MovimientoEntity() {
	}

	MovimientoEntity(Long operacionId, Long cuentaId, Sentido sentido) {
		this.operacionId = operacionId;
		this.cuentaId = cuentaId;
		this.sentido = sentido;
	}

	public Long getId() {
		return id;
	}

	public Long getOperacionId() {
		return operacionId;
	}

	public Long getCuentaId() {
		return cuentaId;
	}

	public Sentido getSentido() {
		return sentido;
	}

}
