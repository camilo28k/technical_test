# Pruebas y versionamiento Git

Estrategia para revisión. Cada incremento del [plan de implementación](04-plan-de-implementacion.md) entrega la funcionalidad y sus pruebas desde la misma rama hija de `develop`. Los criterios que se citan a continuación están definidos en [requerimientos y reglas](01-requerimientos-y-reglas.md).

## Qué probar

| Nivel | Casos prioritarios | Evidencia esperada |
| --- | --- | --- |
| Dominio y servicios | Edad con reloj fijo de Colombia, combinación tipo/número, transiciones de cuenta, importes, fondos y condiciones de reverso. | JUnit 5 prueba el dominio directamente. En servicios, Mockito sustituye los puertos de salida y permite verificar que un rechazo evita escrituras. |
| Controladores REST | Solicitud válida, formato inválido, recurso inexistente y conflicto de negocio. | Pruebas de controlador con el puerto de entrada simulado. Comprobar estado HTTP, respuesta y conversión de datos según el contrato. |
| Integración con PostgreSQL | Migraciones desde una base vacía, claves foráneas, unicidad, precisión monetaria, consultas, rollback y concurrencia. | Ejecutar contra PostgreSQL real en un entorno aislado. Se propone Testcontainers conforme a la [arquitectura](02-arquitectura-y-decisiones.md). |
| Recorrido funcional | Alta de cliente, apertura, consignación, transferencia, reverso y consulta del historial desde el frontend. | Con el backend y PostgreSQL en Compose, registrar datos de prueba, saldos obtenidos y resultado del recorrido (CA-15). |

## Casos que requieren la base de datos

| Escenario | Cómo verificarlo |
| --- | --- |
| Transferencia interrumpida (CA-07) | Provocar un fallo de escritura después del débito. Al terminar, consultar desde otra transacción y comprobar que ambos saldos y el historial coinciden con el estado previo. |
| Dos retiros sobre la misma cuenta (CA-06) | Con saldo de 100,00 COP, lanzar dos retiros de 80,00 en transacciones independientes coordinadas para competir por la cuenta. Debe confirmarse uno, quedar 20,00 y registrarse un solo débito. |
| Reversos concurrentes (CA-09) | Solicitar dos reversos del mismo original. Verificar un único reverso total vinculado al original, las dos compensaciones si era una transferencia y la conservación del historial original. |
| Integridad y consulta (CA-03, CA-11, CA-13) | Intentar repetir una identificación y eliminar un propietario con cuenta vinculada. Consultar además una cuenta vacía y otra con movimientos para contrastar saldo e historial. |

Preparar datos independientes por caso y controlar el inicio de operaciones concurrentes con sincronización explícita. Los saldos, el número de movimientos y los vínculos al original se verifican después de la confirmación o rollback. El fundamento está en el [modelo de datos](03-modelo-de-datos.md) y el [ADR de consistencia](adr/002-consistencia-de-movimientos.md).

## Evidencia para revisión

Cada PR de funcionalidad identifica el commit probado, los criterios cubiertos, el comando ejecutado, el entorno y el resultado real. Un fallo incluye el escenario reproducible y su efecto. Si se mide cobertura, se informa la medición para servicios y controladores junto con los comportamientos comprobados. Los cambios solo documentales se validan mediante diff, enlaces, formato y coherencia de las referencias.

## Flujo de ramas y promoción

| Destino | Procedimiento | Condición de integración |
| --- | --- | --- |
| `develop` | Partir de su referencia remota actualizada. Trabajar en una rama hija y ejecutar pruebas con cada funcionalidad (CA-16). Mostrar diff y mensaje previsto, obtener autorización para commit y push y abrir el PR hacia `develop`. | Presentar evidencias y obtener autorización de merge. |
| `qa` | Partir de `origin/qa` actualizado. Seleccionar los commits ya integrados en `develop`, incluidas pruebas y dependencias. Aplicar `git cherry-pick -x` en orden en la rama hija. | Repetir pruebas unitarias y de integración, validar aceptación y regresión y adjuntar resultados al PR hacia `qa`. Ante un fallo, detener la promoción y originar la corrección en una rama hija de `develop`. |
| `main` | Partir de `origin/main` actualizado en `release/...`. Seleccionar los commits de la versión validada en QA, comprobar equivalencias y dependencias y aplicar `git cherry-pick -x` en orden. | Ejecutar las validaciones finales, preparar el PR hacia `main` y obtener autorización para integrar o publicar. |

**Nombre de la rama hija de QA.** El flujo original solicita `qa/...`. La rama permanente `qa` ya ocupa `refs/heads/qa`, lo que impide crear una referencia hija con ese prefijo. Se propone `qa-promocion/<entrega>` desde `origin/qa`, con PR hacia `qa`. Este ajuste de nombre queda para aprobación antes de la primera promoción.

**Trazabilidad.** En el PR de promoción registrar, por cada cambio, el SHA integrado en origen, su PR y el SHA aplicado en destino. Si hubo squash, usar el commit resultante. Si hubo merge convencional, seleccionar los commits de trabajo necesarios. Contrastar el contenido con destino antes de aplicar para evitar duplicados. Las ramas permanentes reciben cambios mediante sus PR, con autorización de merge. El flujo excluye merges directos entre ramas permanentes y push forzado. Eliminar ramas requiere autorización.
