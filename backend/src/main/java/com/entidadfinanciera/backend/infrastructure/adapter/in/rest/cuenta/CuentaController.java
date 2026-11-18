package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cuenta;

import java.net.URI;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.port.in.GestionarCuentasUseCase;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;
import com.entidadfinanciera.backend.infrastructure.adapter.in.rest.comun.PaginaResponse;

import jakarta.validation.Valid;

/** Adaptador REST de cuentas (contrato REST, sección 3). No existe DELETE: se cancela. */
@RestController
@RequestMapping("/api/v1")
public class CuentaController {

	private final GestionarCuentasUseCase cuentas;
	private final ZoneId zona;

	public CuentaController(GestionarCuentasUseCase cuentas, Clock reloj) {
		this.cuentas = cuentas;
		this.zona = reloj.getZone();
	}

	@PostMapping("/cuentas")
	public ResponseEntity<CuentaResponse> abrir(@Valid @RequestBody AperturaCuentaRequest solicitud) {
		Cuenta cuenta = cuentas.abrir(solicitud.clienteId(), TipoCuenta.valueOf(solicitud.tipoCuenta()));
		return ResponseEntity.created(URI.create("/api/v1/cuentas/" + cuenta.getId())).body(respuesta(cuenta));
	}

	@GetMapping("/cuentas")
	public PaginaResponse<CuentaResponse> listar(@RequestParam Optional<Long> clienteId,
			@RequestParam Optional<EstadoCuenta> estado, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PaginaResponse.desde(cuentas.listar(clienteId, estado, new Paginacion(page, size)).map(this::respuesta));
	}

	@GetMapping("/cuentas/{id}")
	public CuentaResponse consultar(@PathVariable Long id) {
		return respuesta(cuentas.consultar(id));
	}

	@GetMapping("/cuentas/numero/{numero}")
	public CuentaResponse consultarPorNumero(@PathVariable String numero) {
		return respuesta(cuentas.consultarPorNumero(numero));
	}

	@GetMapping("/clientes/{clienteId}/cuentas")
	public List<CuentaResponse> listarPorCliente(@PathVariable Long clienteId) {
		return cuentas.listarPorCliente(clienteId).stream().map(this::respuesta).toList();
	}

	@PatchMapping("/cuentas/{id}")
	public CuentaResponse cambiar(@PathVariable Long id, @Valid @RequestBody CambioCuentaRequest solicitud) {
		return respuesta(cuentas.cambiarExencionGmf(id, solicitud.exentaGmf()));
	}

	@PostMapping("/cuentas/{id}/activar")
	public CuentaResponse activar(@PathVariable Long id) {
		return respuesta(cuentas.activar(id));
	}

	@PostMapping("/cuentas/{id}/inactivar")
	public CuentaResponse inactivar(@PathVariable Long id) {
		return respuesta(cuentas.inactivar(id));
	}

	@PostMapping("/cuentas/{id}/cancelar")
	public CuentaResponse cancelar(@PathVariable Long id) {
		return respuesta(cuentas.cancelar(id));
	}

	private CuentaResponse respuesta(Cuenta cuenta) {
		return CuentaResponse.desde(cuenta, zona);
	}

}
