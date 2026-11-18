# Prueba técnica: gestión de clientes y productos financieros

Aplicación para administrar clientes, cuentas y movimientos financieros mediante una API REST. El proyecto utilizará Java, Spring Boot y PostgreSQL con arquitectura hexagonal. El alcance incluye un frontend en React con Vite y Docker Compose para ejecutar frontend, backend y PostgreSQL, desplegados en una instancia AWS EC2.

## Estado

Backend (etapas 1 a 7) y frontend (etapa 8) implementados. Pendiente: despliegue en AWS EC2.

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

## Ejecución local

Requisitos: Docker Desktop. Para compilar y probar fuera de Docker, JDK 21 y Node.js 22.

```bash
cp .env.example .env
docker compose up -d --build
curl http://localhost/actuator/health
```

- Aplicación web: <http://localhost> (puerto configurable con `FRONTEND_PORT`).
- Swagger UI: <http://localhost/swagger-ui/index.html>.

Solo Nginx publica un puerto: sirve el frontend y redirige `/api`, la salud y Swagger al backend, que queda en la red interna de Compose junto con PostgreSQL.

Pruebas del backend (usan Testcontainers, Docker debe estar en ejecución):

```bash
cd backend
./mvnw test
```

Frontend en desarrollo (el proxy de Vite envía `/api` a `http://localhost:8080`, o a `BACKEND_URL`) y sus pruebas con Vitest:

```bash
cd frontend
npm install
npm run dev
npm test
```

Stack: Java 21, Spring Boot 4.1, Maven Wrapper, PostgreSQL 16, Flyway, React 18, Vite 6, TypeScript y Nginx.
