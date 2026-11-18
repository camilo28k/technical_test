package com.entidadfinanciera.backend.infrastructure.adapter.out.persistence.operacion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio Spring Data. Solo lo usa {@link OperacionPersistenceAdapter}. */
interface MovimientoJpaRepository extends JpaRepository<MovimientoEntity, Long> {

	/** Movimientos de una operación con el número de su cuenta: {@code [MovimientoEntity, String]}. */
	@Query("""
			select m, c.numero from MovimientoEntity m, CuentaEntity c
			where c.id = m.cuentaId and m.operacionId = :operacionId order by m.id""")
	List<Object[]> buscarConNumeroDeCuenta(@Param("operacionId") Long operacionId);

}
