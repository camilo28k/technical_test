package com.entidadfinanciera.backend.infrastructure.transaction;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.port.in.GestionarClientesUseCase;
import com.entidadfinanciera.backend.domain.model.Cliente;
import com.entidadfinanciera.backend.domain.model.DatosCliente;

/**
 * Decorador que delimita la transacción de cada caso de uso y delega en el servicio de
 * aplicación. Mantiene {@code @Transactional} fuera del núcleo (patrón Decorator, documento 02).
 */
public class GestionarClientesTransaccional implements GestionarClientesUseCase {

	private final GestionarClientesUseCase delegado;

	public GestionarClientesTransaccional(GestionarClientesUseCase delegado) {
		this.delegado = delegado;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Cliente registrar(DatosCliente datos) {
		return delegado.registrar(datos);
	}

	@Override
	@Transactional(readOnly = true)
	public Cliente consultar(Long id) {
		return delegado.consultar(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Pagina<Cliente> listar(Optional<String> numeroIdentificacion, Paginacion paginacion) {
		return delegado.listar(numeroIdentificacion, paginacion);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Cliente actualizar(Long id, DatosCliente datos) {
		return delegado.actualizar(id, datos);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void eliminar(Long id) {
		delegado.eliminar(id);
	}

}
