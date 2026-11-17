# Prueba técnica: gestión de clientes y productos financieros

Aplicación para administrar clientes, cuentas y movimientos financieros mediante una API REST. El proyecto utilizará Java, Spring Boot y PostgreSQL con arquitectura hexagonal. El alcance incluye un frontend en React con Vite y Docker Compose para ejecutar frontend, backend y PostgreSQL, desplegados en una instancia AWS EC2.

## Estado

Documentación aprobada e integrada en `develop`. Siguiente paso: etapa 1 del plan, base ejecutable con Spring Boot, PostgreSQL y Docker Compose.

## Documentación

1. [Requerimientos y reglas de negocio](docs/01-requerimientos-y-reglas.md).
2. [Arquitectura y decisiones técnicas](docs/02-arquitectura-y-decisiones.md).
3. [Modelo de datos](docs/03-modelo-de-datos.md).
4. [Plan de implementación por etapas](docs/04-plan-de-implementacion.md).
5. [Pruebas y versionamiento Git](docs/05-pruebas-y-versionamiento.md).
6. [Historias de usuario](docs/06-historias-de-usuario.md).
7. [Contrato de la API REST](docs/07-contrato-api-rest.md).
8. [Despliegue en AWS EC2](docs/08-despliegue-en-aws-ec2.md).

Registros de decisiones de arquitectura (ADR):

- [ADR-001 · Separar el dominio mediante puertos y adaptadores](docs/adr/001-puertos-y-adaptadores.md)
- [ADR-002 · Confirmar saldos y movimientos en una transacción](docs/adr/002-consistencia-de-movimientos.md)
