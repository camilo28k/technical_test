package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cuenta;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.port.in.ConsultarOperacionesUseCase;

/** Estado de cuenta: saldo e historial paginado, del más reciente al más antiguo (RF-07). */
@RestController
public class EstadoDeCuentaController {

	private final ConsultarOperacionesUseCase consultas;
	private final ZoneId zona;

	public EstadoDeCuentaController(ConsultarOperacionesUseCase consultas, Clock reloj) {
		this.consultas = consultas;
		this.zona = reloj.getZone();
	}

	@GetMapping("/api/v1/cuentas/{id}/estado-de-cuenta")
	public EstadoDeCuentaResponse consultar(@PathVariable Long id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> desde,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> hasta,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return EstadoDeCuentaResponse.desde(consultas.estadoDeCuenta(id, desde, hasta, new Paginacion(page, size)),
				zona);
	}

}
