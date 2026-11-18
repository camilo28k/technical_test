package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.cuenta;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** Repositorio Spring Data. Solo lo usa {@link CuentaPersistenceAdapter}. */
interface CuentaJpaRepository extends JpaRepository<CuentaEntity, Long>, JpaSpecificationExecutor<CuentaEntity> {

	Optional<CuentaEntity> findByNumero(String numero);

	List<CuentaEntity> findByClienteIdOrderByIdAsc(Long clienteId);

	boolean existsByClienteId(Long clienteId);

	/** {@code SELECT ... FOR UPDATE}: serializa los cambios concurrentes sobre la misma cuenta. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from CuentaEntity c where c.id = :id")
	Optional<CuentaEntity> buscarParaActualizar(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from CuentaEntity c where c.numero = :numero")
	Optional<CuentaEntity> buscarPorNumeroParaActualizar(@Param("numero") String numero);

	@Query(value = "SELECT nextval('cuenta_ahorros_seq')", nativeQuery = true)
	long siguienteSufijoAhorros();

	@Query(value = "SELECT nextval('cuenta_corriente_seq')", nativeQuery = true)
	long siguienteSufijoCorriente();

}
