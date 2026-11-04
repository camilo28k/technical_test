# ADR-001 · Separar el dominio mediante puertos y adaptadores

**Estado** — Arquitectura hexagonal aprobada (DA-06).

**Contexto.** La edad mínima, la cancelación de cuentas y los fondos suficientes deben comprobarse también cuando una prueba invoca el caso de uso directamente. El PDF admite MVC y pide responsabilidades de controlador, servicio, entidad y repositorio.

**Decisión.** El servicio de aplicación coordina el caso de uso y delega las reglas en el dominio. REST invoca un puerto de entrada y la persistencia implementa puertos de salida definidos por la aplicación. Así puede probarse un retiro con cuentas en memoria y verificar después, por separado, que el adaptador PostgreSQL cumple su contrato.

**Alternativa considerada.** Un servicio MVC que use directamente repositorios Spring Data reduce el número de interfaces. A cambio, introduce tipos del framework en los contratos que necesitan las pruebas del caso de uso.

**Consecuencia.** El equipo mantiene contratos y mapeos entre HTTP, dominio y persistencia. Para que ese coste sea proporcional, los puertos se definen por capacidad de negocio. [Arquitectura y correspondencia con las capas del PDF](../02-arquitectura-y-decisiones.md)
