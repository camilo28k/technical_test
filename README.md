# Prueba técnica: gestión de clientes y productos financieros

Aplicación para administrar clientes, cuentas y movimientos financieros mediante una API REST. El proyecto utilizará Java, Spring Boot y PostgreSQL con arquitectura hexagonal. El alcance incluye un frontend funcional y Docker Compose para ejecutar el backend y PostgreSQL.

## Estado

Requerimientos y arquitectura integrados en `develop`. Modelo de datos en revisión.

## Documentación

1. [Requerimientos y reglas de negocio](docs/01-requerimientos-y-reglas.md).
2. [Arquitectura y decisiones técnicas](docs/02-arquitectura-y-decisiones.md).
3. [Modelo de datos](docs/03-modelo-de-datos.md).

Registros de decisiones de arquitectura (ADR):

- [ADR-001 · Separar el dominio mediante puertos y adaptadores](docs/adr/001-puertos-y-adaptadores.md)
- [ADR-002 · Confirmar saldos y movimientos en una transacción](docs/adr/002-consistencia-de-movimientos.md)

Los próximos avances documentarán el plan de implementación y la estrategia de pruebas y versionamiento.
