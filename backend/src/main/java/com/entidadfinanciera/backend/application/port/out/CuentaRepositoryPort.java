package com.entidadfinanciera.backend.application.port.out;

import java.util.List;
import java.util.Optional;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;

/** Puerto de salida para persistir y consultar cuentas. */
public interface CuentaRepositoryPort {

	/** Siguiente número libre de diez dígitos con el prefijo del tipo (RN-06). */
	String siguienteNumero(TipoCuenta tipo);

	Cuenta guardar(Cuenta cuenta);

	Optional<Cuenta> buscarPorId(Long id);

	/** Igual que {@link #buscarPorId} pero bloquea la fila hasta el fin de la transacción. */
	Optional<Cuenta> buscarPorIdParaActualizar(Long id);

	Optional<Cuenta> buscarPorNumero(String numero);

	/** Igual que {@link #buscarPorNumero} pero bloquea la fila hasta el fin de la transacción. */
	Optional<Cuenta> buscarPorNumeroParaActualizar(String numero);

	Pagina<Cuenta> listar(Optional<Long> clienteId, Optional<EstadoCuenta> estado, Paginacion paginacion);

	List<Cuenta> listarPorCliente(Long clienteId);

	boolean existePorCliente(Long clienteId);

}
