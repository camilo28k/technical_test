package com.entidadfinanciera.backend.application.port.in;

import java.util.Optional;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.domain.model.Cliente;
import com.entidadfinanciera.backend.domain.model.DatosCliente;

/** Puerto de entrada para la capacidad de gestión de clientes (HU-01 a HU-03). */
public interface GestionarClientesUseCase {

	Cliente registrar(DatosCliente datos);

	Cliente consultar(Long id);

	Pagina<Cliente> listar(Optional<String> numeroIdentificacion, Paginacion paginacion);

	Cliente actualizar(Long id, DatosCliente datos);

	void eliminar(Long id);

}
