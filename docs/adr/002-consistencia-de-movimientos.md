# ADR-002 · Confirmar saldos y movimientos en una transacción

**Estado** — Atomicidad e inmutabilidad aprobadas (P-04). Mecanismo de bloqueo propuesto para revisión.

**Contexto.** Si dos retiros de 80,00 COP leen simultáneamente un saldo de 100,00 COP, ambos podrían superar la validación de fondos. Además, una transferencia debe conservar el débito y el crédito juntos, incluso si falla la segunda escritura.

**Decisión propuesta.** Una transacción PostgreSQL abarca la lectura protegida de las cuentas, la validación de fondos y estado, el cambio de saldos y el registro del historial. Las escrituras sobre una cuenta comparten la misma disciplina de bloqueo. Cuando hay dos cuentas se bloquean por identificador en un orden estable. El reverso registra una operación compensatoria vinculada al original y una restricción única evita duplicados.

**Alternativa considerada.** El control optimista mediante versiones permitiría detectar una escritura concurrente y reintentar la operación completa. Para esta prueba se prefiere que el segundo retiro espere y valide con el saldo ya confirmado.

**Consecuencia.** Las operaciones sobre una cuenta muy activa pueden esperar, por lo que la transacción debe ser corta. La futura prueba de integración deberá verificar que solo uno de los retiros se confirma y que un fallo tras el débito conserva ambos saldos y el historial previo. [Modelo de datos y escenarios de concurrencia](../03-modelo-de-datos.md)
