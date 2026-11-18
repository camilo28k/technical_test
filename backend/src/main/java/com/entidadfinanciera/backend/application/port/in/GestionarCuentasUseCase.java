package com.entidadfinanciera.backend.application.port.in;

import java.util.List;
import java.util.Optional;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;

/** Puerto de entrada: apertura, consulta, cambios permitidos y estados de cuentas. */
public interface GestionarCuentasUseCase {

	Cuenta abrir(Long clienteId, TipoCuenta tipo);

	Cuenta consultar(Long id);

	Cuenta consultarPorNumero(String numero);

	Pagina<Cuenta> listar(Optional<Long> clienteId, Optional<EstadoCuenta> estado, Paginacion paginacion);

	/** Todas las cuentas del cliente, incluidas las canceladas. */
	List<Cuenta> listarPorCliente(Long clienteId);

	Cuenta cambiarExencionGmf(Long id, boolean exenta);

	Cuenta activar(Long id);

	Cuenta inactivar(Long id);

	Cuenta cancelar(Long id);

}
