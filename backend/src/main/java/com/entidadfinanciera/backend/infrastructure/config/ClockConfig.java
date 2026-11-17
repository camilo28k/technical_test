package com.entidadfinanciera.backend.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj del sistema en la zona horaria de Colombia. Se inyecta en los casos de uso para
 * calcular la edad y las fechas automáticas, y las pruebas pueden sustituirlo por un reloj fijo.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

	@Bean
	Clock clock(@Value("${app.zona-horaria}") String zonaHoraria) {
		return Clock.system(ZoneId.of(zonaHoraria));
	}

}
