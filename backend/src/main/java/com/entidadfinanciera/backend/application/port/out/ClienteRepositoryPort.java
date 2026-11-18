package com.entidadfinanciera.backend.application.port.out;

import java.util.Optional;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.domain.model.Cliente;
import com.entidadfinanciera.backend.domain.model.TipoIdentificacion;

/** Puerto de salida para persistir y consultar clientes. */
public interface ClienteRepositoryPort {

	/**
	 * Guarda el cliente y devuelve su versión persistida. Lanza
	 * {@code IdentificacionDuplicadaException} si la base de datos detecta la identificación repetida.
	 */
	Cliente guardar(Cliente cliente);

	Optional<Cliente> buscarPorId(Long id);

	boolean existePorIdentificacion(TipoIdentificacion tipo, String numero);

	Pagina<Cliente> listar(Optional<String> numeroIdentificacion, Paginacion paginacion);

	void eliminar(Long id);

}
