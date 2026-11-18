package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.cuenta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Representación JPA de la tabla {@code cuenta}. No sale del adaptador de persistencia. */
@Entity
@Table(name = "cuenta")
public class CuentaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "cliente_id", nullable = false, updatable = false)
	private Long clienteId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10, updatable = false)
	private TipoCuenta tipo;

	@Column(nullable = false, length = 10, updatable = false)
	private String numero;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private EstadoCuenta estado;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal saldo;

	@Column(name = "exenta_gmf", nullable = false)
	private boolean exentaGmf;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private OffsetDateTime fechaCreacion;

	@Column(name = "fecha_modificacion")
	private OffsetDateTime fechaModificacion;

	protected CuentaEntity() {
	}

	CuentaEntity(Long clienteId, TipoCuenta tipo, String numero, OffsetDateTime fechaCreacion) {
		this.clienteId = clienteId;
		this.tipo = tipo;
		this.numero = numero;
		this.fechaCreacion = fechaCreacion;
	}

	public Long getId() {
		return id;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public TipoCuenta getTipo() {
		return tipo;
	}

	public String getNumero() {
		return numero;
	}

	public EstadoCuenta getEstado() {
		return estado;
	}

	public void setEstado(EstadoCuenta estado) {
		this.estado = estado;
	}

	public BigDecimal getSaldo() {
		return saldo;
	}

	public void setSaldo(BigDecimal saldo) {
		this.saldo = saldo;
	}

	public boolean isExentaGmf() {
		return exentaGmf;
	}

	public void setExentaGmf(boolean exentaGmf) {
		this.exentaGmf = exentaGmf;
	}

	public OffsetDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public OffsetDateTime getFechaModificacion() {
		return fechaModificacion;
	}

	public void setFechaModificacion(OffsetDateTime fechaModificacion) {
		this.fechaModificacion = fechaModificacion;
	}

}
