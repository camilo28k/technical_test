package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.operacion;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data. Solo lo usa {@link OperacionPersistenceAdapter}. */
interface OperacionJpaRepository extends JpaRepository<OperacionEntity, Long> {
}
