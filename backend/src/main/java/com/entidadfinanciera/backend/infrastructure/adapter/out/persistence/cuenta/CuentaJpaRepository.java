package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.cuenta;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/** Repositorio Spring Data. Solo lo usa {@link CuentaPersistenceAdapter}. */
interface CuentaJpaRepository extends JpaRepository<CuentaEntity, Long>, JpaSpecificationExecutor<CuentaEntity> {

	Optional<CuentaEntity> findByNumero(String numero);

	List<CuentaEntity> findByClienteIdOrderByIdAsc(Long clienteId);

	boolean existsByClienteId(Long clienteId);

	@Query(value = "SELECT nextval('cuenta_ahorros_seq')", nativeQuery = true)
	long siguienteSufijoAhorros();

	@Query(value = "SELECT nextval('cuenta_corriente_seq')", nativeQuery = true)
	long siguienteSufijoCorriente();

}
