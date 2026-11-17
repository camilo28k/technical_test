package com.entidadfinanciera.backend.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.entidadfinanciera.backend.application.port.in.GestionarClientesUseCase;
import com.entidadfinanciera.backend.application.port.out.ClienteRepositoryPort;
import com.entidadfinanciera.backend.application.service.ClienteService;
import com.entidadfinanciera.backend.infrastructure.transaction.GestionarClientesTransaccional;

/** Ensambla los casos de uso: servicio Java puro envuelto en su decorador transaccional. */
@Configuration(proxyBeanMethods = false)
public class CasosDeUsoConfig {

	@Bean
	GestionarClientesUseCase gestionarClientes(ClienteRepositoryPort clientes, Clock reloj) {
		return new GestionarClientesTransaccional(new ClienteService(clientes, reloj));
	}

}
