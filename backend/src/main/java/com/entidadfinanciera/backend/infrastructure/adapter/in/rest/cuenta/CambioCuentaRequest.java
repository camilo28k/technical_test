package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.cuenta;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code PATCH /cuentas/{id}}: único atributo modificable (P-05, DA-04). */
public record CambioCuentaRequest(@NotNull(message = "es obligatorio") Boolean exentaGmf) {
}
