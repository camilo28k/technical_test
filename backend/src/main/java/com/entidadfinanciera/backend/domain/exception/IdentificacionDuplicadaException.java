package com.entidadfinanciera.backend.domain.exception;

import com.entidadfinanciera.backend.domain.model.TipoIdentificacion;

/** P-02: la combinación de tipo y número de identificación es única. */
public class IdentificacionDuplicadaException extends ReglaDeNegocioException {

	public IdentificacionDuplicadaException(TipoIdentificacion tipo, String numero) {
		super("IDENTIFICACION_DUPLICADA",
				"Ya existe un cliente con identificación " + tipo + " " + numero + ".");
	}

}
