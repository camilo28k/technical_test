package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.cliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.entidadfinanciera.backend.domain.model.TipoIdentificacion;

/** Repositorio Spring Data. Solo lo usa {@link ClientePersistenceAdapter}. */
interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {

	boolean existsByTipoIdentificacionAndNumeroIdentificacion(TipoIdentificacion tipo, String numero);

	Page<ClienteEntity> findByNumeroIdentificacion(String numero, Pageable pageable);

}
