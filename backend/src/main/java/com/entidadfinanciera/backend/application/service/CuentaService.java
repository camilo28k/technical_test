package com.entidadfinanciera.backend.application.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.port.in.GestionarCuentasUseCase;
import com.entidadfinanciera.backend.application.port.out.ClienteRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.ClienteNoEncontradoException;
import com.entidadfinanciera.backend.domain.exception.CuentaNoEncontradaException;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;

/**
 * Casos de uso de cuentas. Java puro; la transacción la aporta un decorador de
 * infraestructura. Los cambios de estado bloquean la fila de la cuenta, la misma disciplina
 * que usarán los movimientos (documento 03).
 */
public class CuentaService implements GestionarCuentasUseCase {

	private final CuentaRepositoryPort cuentas;
	private final ClienteRepositoryPort clientes;
	private final Clock reloj;

	public CuentaService(CuentaRepositoryPort cuentas, ClienteRepositoryPort clientes, Clock reloj) {
		this.cuentas = cuentas;
		this.clientes = clientes;
		this.reloj = reloj;
	}

	@Override
	public Cuenta abrir(Long clienteId, TipoCuenta tipo) {
		exigirCliente(clienteId);
		return cuentas.guardar(Cuenta.abrir(clienteId, tipo, cuentas.siguienteNumero(tipo), reloj));
	}

	@Override
	public Cuenta consultar(Long id) {
		return cuentas.buscarPorId(id).orElseThrow(() -> CuentaNoEncontradaException.porId(id));
	}

	@Override
	public Cuenta consultarPorNumero(String numero) {
		return cuentas.buscarPorNumero(numero.trim()).orElseThrow(() -> CuentaNoEncontradaException.porNumero(numero));
	}

	@Override
	public Pagina<Cuenta> listar(Optional<Long> clienteId, Optional<EstadoCuenta> estado, Paginacion paginacion) {
		return cuentas.listar(clienteId, estado, paginacion);
	}

	@Override
	public List<Cuenta> listarPorCliente(Long clienteId) {
		exigirCliente(clienteId);
		return cuentas.listarPorCliente(clienteId);
	}

	@Override
	public Cuenta cambiarExencionGmf(Long id, boolean exenta) {
		return modificar(id, c -> c.cambiarExencionGmf(exenta, reloj));
	}

	@Override
	public Cuenta activar(Long id) {
		return modificar(id, c -> c.activar(reloj));
	}

	@Override
	public Cuenta inactivar(Long id) {
		return modificar(id, c -> c.inactivar(reloj));
	}

	@Override
	public Cuenta cancelar(Long id) {
		return modificar(id, c -> c.cancelar(reloj));
	}

	private Cuenta modificar(Long id, Consumer<Cuenta> cambio) {
		Cuenta cuenta = cuentas.buscarPorIdParaActualizar(id).orElseThrow(() -> CuentaNoEncontradaException.porId(id));
		cambio.accept(cuenta);
		return cuentas.guardar(cuenta);
	}

	private void exigirCliente(Long clienteId) {
		if (clientes.buscarPorId(clienteId).isEmpty()) {
			throw new ClienteNoEncontradoException(clienteId);
		}
	}

}
