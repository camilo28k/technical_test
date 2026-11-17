package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cliente;

import java.net.URI;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.port.in.GestionarClientesUseCase;
import com.entidadfinanciera.backend.domain.model.Cliente;
import com.entidadfinanciera.backend.infrastructure.adapter.in.rest.comun.PaginaResponse;

import jakarta.validation.Valid;

/** Adaptador REST de clientes (contrato REST, sección 2). */
@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

	private final GestionarClientesUseCase clientes;
	private final ZoneId zona;

	public ClienteController(GestionarClientesUseCase clientes, Clock reloj) {
		this.clientes = clientes;
		this.zona = reloj.getZone();
	}

	@PostMapping
	public ResponseEntity<ClienteResponse> registrar(@Valid @RequestBody ClienteRequest solicitud) {
		Cliente cliente = clientes.registrar(solicitud.aDatos());
		return ResponseEntity.created(URI.create("/api/v1/clientes/" + cliente.getId())).body(respuesta(cliente));
	}

	@GetMapping
	public PaginaResponse<ClienteResponse> listar(@RequestParam Optional<String> numeroIdentificacion,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return PaginaResponse.desde(clientes.listar(numeroIdentificacion, new Paginacion(page, size)).map(this::respuesta));
	}

	@GetMapping("/{id}")
	public ClienteResponse consultar(@PathVariable Long id) {
		return respuesta(clientes.consultar(id));
	}

	@PutMapping("/{id}")
	public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest solicitud) {
		return respuesta(clientes.actualizar(id, solicitud.aDatos()));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		clientes.eliminar(id);
		return ResponseEntity.noContent().build();
	}

	private ClienteResponse respuesta(Cliente cliente) {
		return ClienteResponse.desde(cliente, zona);
	}

}
