package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.operacion;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.entidadfinanciera.backend.application.port.in.RegistrarOperacionesUseCase;

import jakarta.validation.Valid;

/**
 * Adaptador REST de operaciones (contrato REST, sección 4). No hay PUT ni DELETE: las
 * operaciones son inmutables (DA-01). El 201 se emite después del commit.
 */
@RestController
@RequestMapping("/api/v1/operaciones")
public class OperacionController {

	private final RegistrarOperacionesUseCase operaciones;
	private final ZoneId zona;

	public OperacionController(RegistrarOperacionesUseCase operaciones, Clock reloj) {
		this.operaciones = operaciones;
		this.zona = reloj.getZone();
	}

	@PostMapping("/consignaciones")
	@ResponseStatus(HttpStatus.CREATED)
	public OperacionResponse consignar(@Valid @RequestBody MovimientoCuentaRequest solicitud) {
		return OperacionResponse.desde(operaciones.consignar(solicitud.numeroCuenta(), solicitud.importe()), zona);
	}

	@PostMapping("/retiros")
	@ResponseStatus(HttpStatus.CREATED)
	public OperacionResponse retirar(@Valid @RequestBody MovimientoCuentaRequest solicitud) {
		return OperacionResponse.desde(operaciones.retirar(solicitud.numeroCuenta(), solicitud.importe()), zona);
	}

	@PostMapping("/transferencias")
	@ResponseStatus(HttpStatus.CREATED)
	public OperacionResponse transferir(@Valid @RequestBody TransferenciaRequest solicitud) {
		return OperacionResponse.desde(operaciones.transferir(solicitud.numeroCuentaOrigen(),
				solicitud.numeroCuentaDestino(), solicitud.importe()), zona);
	}

	@PostMapping("/{id}/reverso")
	@ResponseStatus(HttpStatus.CREATED)
	public OperacionResponse reversar(@PathVariable Long id) {
		return OperacionResponse.desde(operaciones.reversar(id), zona);
	}

}
