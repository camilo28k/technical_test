package com.entidadfinanciera.backend.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.entidadfinanciera.backend.application.port.in.GestionarClientesUseCase;
import com.entidadfinanciera.backend.application.port.in.GestionarCuentasUseCase;
import com.entidadfinanciera.backend.application.port.in.RegistrarOperacionesUseCase;
import com.entidadfinanciera.backend.application.port.out.ClienteRepositoryPort;
import com.entidadfinanciera.backend.application.port.out.OperacionRepositoryPort;
import com.entidadfinanciera.backend.application.service.OperacionService;
import com.entidadfinanciera.backend.infrastructure.transaction.RegistrarOperacionesTransaccional;
import com.entidadfinanciera.backend.application.port.out.CuentaRepositoryPort;
import com.entidadfinanciera.backend.application.service.ClienteService;
import com.entidadfinanciera.backend.application.service.CuentaService;
import com.entidadfinanciera.backend.infrastructure.transaction.GestionarClientesTransaccional;
import com.entidadfinanciera.backend.infrastructure.transaction.GestionarCuentasTransaccional;

/** Ensambla los casos de uso: servicio Java puro envuelto en su decorador transaccional. */
@Configuration(proxyBeanMethods = false)
public class CasosDeUsoConfig {

	@Bean
	GestionarClientesUseCase gestionarClientes(ClienteRepositoryPort clientes, CuentaRepositoryPort cuentas,
			Clock reloj) {
		return new GestionarClientesTransaccional(new ClienteService(clientes, cuentas, reloj));
	}

	@Bean
	GestionarCuentasUseCase gestionarCuentas(CuentaRepositoryPort cuentas, ClienteRepositoryPort clientes,
			Clock reloj) {
		return new GestionarCuentasTransaccional(new CuentaService(cuentas, clientes, reloj));
	}

	@Bean
	RegistrarOperacionesUseCase registrarOperaciones(CuentaRepositoryPort cuentas,
			OperacionRepositoryPort operaciones, Clock reloj) {
		return new RegistrarOperacionesTransaccional(new OperacionService(cuentas, operaciones, reloj));
	}

}
