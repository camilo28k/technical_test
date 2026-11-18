package com.entidadfinanciera.backend.infrastructure.transaction;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.port.in.GestionarCuentasUseCase;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;

/** Decorador transaccional de los casos de uso de cuentas (patrón Decorator, documento 02). */
public class GestionarCuentasTransaccional implements GestionarCuentasUseCase {

	private final GestionarCuentasUseCase delegado;

	public GestionarCuentasTransaccional(GestionarCuentasUseCase delegado) {
		this.delegado = delegado;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Cuenta abrir(Long clienteId, TipoCuenta tipo) {
		return delegado.abrir(clienteId, tipo);
	}

	@Override
	@Transactional(readOnly = true)
	public Cuenta consultar(Long id) {
		return delegado.consultar(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Cuenta consultarPorNumero(String numero) {
		return delegado.consultarPorNumero(numero);
	}

	@Override
	@Transactional(readOnly = true)
	public Pagina<Cuenta> listar(Optional<Long> clienteId, Optional<EstadoCuenta> estado, Paginacion paginacion) {
		return delegado.listar(clienteId, estado, paginacion);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Cuenta> listarPorCliente(Long clienteId) {
		return delegado.listarPorCliente(clienteId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Cuenta cambiarExencionGmf(Long id, boolean exenta) {
		return delegado.cambiarExencionGmf(id, exenta);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Cuenta activar(Long id) {
		return delegado.activar(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Cuenta inactivar(Long id) {
		return delegado.inactivar(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Cuenta cancelar(Long id) {
		return delegado.cancelar(id);
	}

}
