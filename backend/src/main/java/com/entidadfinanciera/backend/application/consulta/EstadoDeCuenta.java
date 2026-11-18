package com.entidadfinanciera.backend.application.consulta;

import java.time.OffsetDateTime;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.domain.model.Cuenta;

/** Saldo e historial de una cuenta leídos desde la misma instantánea (CA-11). */
public record EstadoDeCuenta(Cuenta cuenta, String titular, OffsetDateTime fechaConsulta,
		Pagina<LineaEstadoDeCuenta> movimientos) {
}
