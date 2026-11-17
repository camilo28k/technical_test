package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.cliente;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.entidadfinanciera.backend.domain.model.TipoIdentificacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Representación JPA de la tabla {@code cliente}. No sale del adaptador de persistencia. */
@Entity
@Table(name = "cliente")
public class ClienteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_identificacion", nullable = false, length = 10)
	private TipoIdentificacion tipoIdentificacion;

	@Column(name = "numero_identificacion", nullable = false, length = 20)
	private String numeroIdentificacion;

	@Column(nullable = false, length = 100)
	private String nombres;

	@Column(nullable = false, length = 100)
	private String apellido;

	@Column(nullable = false, length = 254)
	private String correo;

	@Column(name = "fecha_nacimiento", nullable = false)
	private LocalDate fechaNacimiento;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private OffsetDateTime fechaCreacion;

	@Column(name = "fecha_modificacion")
	private OffsetDateTime fechaModificacion;

	protected ClienteEntity() {
	}

	public Long getId() {
		return id;
	}

	public TipoIdentificacion getTipoIdentificacion() {
		return tipoIdentificacion;
	}

	public void setTipoIdentificacion(TipoIdentificacion tipoIdentificacion) {
		this.tipoIdentificacion = tipoIdentificacion;
	}

	public String getNumeroIdentificacion() {
		return numeroIdentificacion;
	}

	public void setNumeroIdentificacion(String numeroIdentificacion) {
		this.numeroIdentificacion = numeroIdentificacion;
	}

	public String getNombres() {
		return nombres;
	}

	public void setNombres(String nombres) {
		this.nombres = nombres;
	}

	public String getApellido() {
		return apellido;
	}

	public void setApellido(String apellido) {
		this.apellido = apellido;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public LocalDate getFechaNacimiento() {
		return fechaNacimiento;
	}

	public void setFechaNacimiento(LocalDate fechaNacimiento) {
		this.fechaNacimiento = fechaNacimiento;
	}

	public OffsetDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(OffsetDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public OffsetDateTime getFechaModificacion() {
		return fechaModificacion;
	}

	public void setFechaModificacion(OffsetDateTime fechaModificacion) {
		this.fechaModificacion = fechaModificacion;
	}

}
