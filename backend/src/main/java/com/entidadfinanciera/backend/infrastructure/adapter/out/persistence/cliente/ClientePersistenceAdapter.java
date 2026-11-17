package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.cliente;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.port.out.ClienteRepositoryPort;
import com.entidadfinanciera.backend.domain.exception.ClienteMenorDeEdadException;
import com.entidadfinanciera.backend.domain.exception.IdentificacionDuplicadaException;
import com.entidadfinanciera.backend.domain.model.Cliente;
import com.entidadfinanciera.backend.domain.model.DatosCliente;
import com.entidadfinanciera.backend.domain.model.TipoIdentificacion;

/**
 * Implementa el puerto de clientes con Spring Data JPA y traduce las violaciones de
 * restricciones de PostgreSQL a excepciones del dominio.
 */
@Component
class ClientePersistenceAdapter implements ClienteRepositoryPort {

	private final ClienteJpaRepository repositorio;

	ClientePersistenceAdapter(ClienteJpaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public Cliente guardar(Cliente cliente) {
		ClienteEntity entidad = cliente.getId() == null ? new ClienteEntity()
				: repositorio.findById(cliente.getId()).orElseThrow();
		copiar(cliente, entidad);
		try {
			return aDominio(repositorio.saveAndFlush(entidad));
		}
		catch (DataIntegrityViolationException ex) {
			throw traducir(ex, cliente);
		}
	}

	@Override
	public Optional<Cliente> buscarPorId(Long id) {
		return repositorio.findById(id).map(ClientePersistenceAdapter::aDominio);
	}

	@Override
	public boolean existePorIdentificacion(TipoIdentificacion tipo, String numero) {
		return repositorio.existsByTipoIdentificacionAndNumeroIdentificacion(tipo, numero);
	}

	@Override
	public Pagina<Cliente> listar(Optional<String> numeroIdentificacion, Paginacion paginacion) {
		PageRequest solicitud = PageRequest.of(paginacion.page(), paginacion.size(), Sort.by("id"));
		Page<ClienteEntity> pagina = numeroIdentificacion
			.map(numero -> repositorio.findByNumeroIdentificacion(numero, solicitud))
			.orElseGet(() -> repositorio.findAll(solicitud));
		return new Pagina<>(pagina.getContent().stream().map(ClientePersistenceAdapter::aDominio).toList(),
				pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
	}

	@Override
	public void eliminar(Long id) {
		repositorio.deleteById(id);
		repositorio.flush();
	}

	private static RuntimeException traducir(DataIntegrityViolationException ex, Cliente cliente) {
		String detalle = String.valueOf(ex.getMostSpecificCause().getMessage());
		if (detalle.contains("uk_cliente_identificacion")) {
			return new IdentificacionDuplicadaException(cliente.getTipoIdentificacion(),
					cliente.getNumeroIdentificacion());
		}
		if (detalle.contains("ck_cliente_mayor_de_edad")) {
			return new ClienteMenorDeEdadException();
		}
		return ex;
	}

	private static void copiar(Cliente origen, ClienteEntity destino) {
		destino.setTipoIdentificacion(origen.getTipoIdentificacion());
		destino.setNumeroIdentificacion(origen.getNumeroIdentificacion());
		destino.setNombres(origen.getNombres());
		destino.setApellido(origen.getApellido());
		destino.setCorreo(origen.getCorreo());
		destino.setFechaNacimiento(origen.getFechaNacimiento());
		if (destino.getFechaCreacion() == null) {
			destino.setFechaCreacion(origen.getFechaCreacion());
		}
		destino.setFechaModificacion(origen.getFechaModificacion());
	}

	private static Cliente aDominio(ClienteEntity e) {
		DatosCliente datos = new DatosCliente(e.getTipoIdentificacion(), e.getNumeroIdentificacion(), e.getNombres(),
				e.getApellido(), e.getCorreo(), e.getFechaNacimiento());
		return Cliente.reconstruir(e.getId(), datos, e.getFechaCreacion(), e.getFechaModificacion());
	}

}
