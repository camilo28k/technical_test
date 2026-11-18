package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.cuenta;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.domain.model.Cuenta;
import com.entidadfinanciera.backend.domain.model.EstadoCuenta;
import com.entidadfinanciera.backend.domain.model.TipoCuenta;

/** Implementa el puerto de cuentas con Spring Data JPA. */
@Component
class CuentaPersistenceAdapter implements CuentaRepositoryPort {

	private final CuentaJpaRepository repositorio;
	private final EntityManager entityManager;

	CuentaPersistenceAdapter(CuentaJpaRepository repositorio, EntityManager entityManager) {
		this.repositorio = repositorio;
		this.entityManager = entityManager;
	}

	@Override
	public String siguienteNumero(TipoCuenta tipo) {
		long sufijo = switch (tipo) {
			case AHORROS -> repositorio.siguienteSufijoAhorros();
			case CORRIENTE -> repositorio.siguienteSufijoCorriente();
		};
		return tipo.getPrefijo() + String.format("%08d", sufijo);
	}

	@Override
	public Cuenta guardar(Cuenta cuenta) {
		CuentaEntity entidad = cuenta.getId() == null
				? new CuentaEntity(cuenta.getClienteId(), cuenta.getTipo(), cuenta.getNumero(), cuenta.getFechaCreacion())
				: repositorio.findById(cuenta.getId()).orElseThrow();
		entidad.setEstado(cuenta.getEstado());
		entidad.setSaldo(cuenta.getSaldo());
		entidad.setExentaGmf(cuenta.isExentaGmf());
		entidad.setFechaModificacion(cuenta.getFechaModificacion());
		return aDominio(repositorio.saveAndFlush(entidad));
	}

	@Override
	public Optional<Cuenta> buscarPorId(Long id) {
		return repositorio.findById(id).map(CuentaPersistenceAdapter::aDominio);
	}

	@Override
	public Optional<Cuenta> buscarPorIdParaActualizar(Long id) {
		return repositorio.findById(id).map(this::bloquear);
	}

	@Override
	public Optional<Cuenta> buscarPorNumero(String numero) {
		return repositorio.findByNumero(numero).map(CuentaPersistenceAdapter::aDominio);
	}

	@Override
	public Optional<Cuenta> buscarPorNumeroParaActualizar(String numero) {
		return repositorio.findByNumero(numero).map(this::bloquear);
	}

	@Override
	public Pagina<Cuenta> listar(Optional<Long> clienteId, Optional<EstadoCuenta> estado, Paginacion paginacion) {
		Specification<CuentaEntity> filtro = Specification.allOf(
				clienteId.<Specification<CuentaEntity>>map(id -> (r, q, cb) -> cb.equal(r.get("clienteId"), id))
					.orElse(null),
				estado.<Specification<CuentaEntity>>map(e -> (r, q, cb) -> cb.equal(r.get("estado"), e)).orElse(null));
		Page<CuentaEntity> pagina = repositorio.findAll(filtro,
				PageRequest.of(paginacion.page(), paginacion.size(), Sort.by("id")));
		return new Pagina<>(pagina.getContent().stream().map(CuentaPersistenceAdapter::aDominio).toList(),
				pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
	}

	@Override
	public List<Cuenta> listarPorCliente(Long clienteId) {
		return repositorio.findByClienteIdOrderByIdAsc(clienteId).stream().map(CuentaPersistenceAdapter::aDominio).toList();
	}

	@Override
	public boolean existePorCliente(Long clienteId) {
		return repositorio.existsByClienteId(clienteId);
	}

	/**
	 * {@code SELECT ... FOR UPDATE} que además recarga la fila. Si la entidad ya estaba en el
	 * contexto de persistencia (por ejemplo, leída sin bloqueo para conocer su id), una consulta
	 * con bloqueo devolvería la instancia en caché con un saldo obsoleto y se perdería una
	 * actualización concurrente; {@code refresh} garantiza leer el valor confirmado tras el bloqueo.
	 */
	private Cuenta bloquear(CuentaEntity entidad) {
		entityManager.refresh(entidad, LockModeType.PESSIMISTIC_WRITE);
		return aDominio(entidad);
	}

	private static Cuenta aDominio(CuentaEntity e) {
		return Cuenta.reconstruir(e.getId(), e.getClienteId(), e.getTipo(), e.getNumero(), e.getEstado(), e.getSaldo(),
				e.isExentaGmf(), e.getFechaCreacion(), e.getFechaModificacion());
	}

}
