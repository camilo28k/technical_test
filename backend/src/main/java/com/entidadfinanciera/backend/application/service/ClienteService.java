package com.entidadfinanciera.backend.application.service;

import java.time.Clock;
import java.util.Optional;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.port.in.GestionarClientesUseCase;
import com.entidadfinanciera.backend.application.port.out.ClienteRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.ClienteConProductosException;
import com.entidadfinanciera.backend.domain.exception.ClienteNoEncontradoException;
import com.entidadfinanciera.backend.domain.exception.IdentificacionDuplicadaException;
import com.entidadfinanciera.backend.domain.model.Cliente;
import com.entidadfinanciera.backend.domain.model.DatosCliente;

/**
 * Casos de uso de clientes. Java puro: sin Spring ni JPA; la transacción la aporta un
 * decorador de infraestructura (ADR-001, documento 02).
 */
public class ClienteService implements GestionarClientesUseCase {

	private final ClienteRepositoryPort clientes;
	private final CuentaRepositoryPort cuentas;
	private final Clock reloj;

	public ClienteService(ClienteRepositoryPort clientes, CuentaRepositoryPort cuentas, Clock reloj) {
		this.clientes = clientes;
		this.cuentas = cuentas;
		this.reloj = reloj;
	}

	@Override
	public Cliente registrar(DatosCliente datos) {
		Cliente cliente = Cliente.registrar(datos, reloj);
		verificarIdentificacionLibre(cliente);
		return clientes.guardar(cliente);
	}

	@Override
	public Cliente consultar(Long id) {
		return clientes.buscarPorId(id).orElseThrow(() -> new ClienteNoEncontradoException(id));
	}

	@Override
	public Pagina<Cliente> listar(Optional<String> numeroIdentificacion, Paginacion paginacion) {
		return clientes.listar(numeroIdentificacion.map(String::trim).filter(n -> !n.isEmpty()), paginacion);
	}

	@Override
	public Cliente actualizar(Long id, DatosCliente datos) {
		Cliente cliente = consultar(id);
		boolean cambiaIdentificacion = !cliente.tieneIdentificacion(datos.tipoIdentificacion(),
				datos.numeroIdentificacion());
		cliente.actualizar(datos, reloj);
		if (cambiaIdentificacion) {
			verificarIdentificacionLibre(cliente);
		}
		return clientes.guardar(cliente);
	}

	@Override
	public void eliminar(Long id) {
		consultar(id);
		// RN-02: cualquier cuenta, incluso cancelada, protege al propietario.
		if (cuentas.existePorCliente(id)) {
			throw new ClienteConProductosException(id);
		}
		clientes.eliminar(id);
	}

	private void verificarIdentificacionLibre(Cliente cliente) {
		if (clientes.existePorIdentificacion(cliente.getTipoIdentificacion(), cliente.getNumeroIdentificacion())) {
			throw new IdentificacionDuplicadaException(cliente.getTipoIdentificacion(),
					cliente.getNumeroIdentificacion());
		}
	}

}
