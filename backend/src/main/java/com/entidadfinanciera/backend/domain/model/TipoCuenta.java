package com.entidadfinanciera.backend.domain.model;

/** Clases de cuenta admitidas (RF-03) con el prefijo de su número (RN-06). */
public enum TipoCuenta {

	AHORROS("53"), CORRIENTE("33");

	private final String prefijo;

	TipoCuenta(String prefijo) {
		this.prefijo = prefijo;
	}

	public String getPrefijo() {
		return prefijo;
	}

}
