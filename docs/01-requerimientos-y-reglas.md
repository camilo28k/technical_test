# Requerimientos y reglas de negocio

Estado: borrador para revisión. Las decisiones iniciales indicadas como aprobadas fueron acordadas con el responsable del proyecto.

## 1. Objetivo y alcance

Desarrollar una aplicación para trabajadores de una entidad financiera que permita administrar clientes y sus cuentas, registrar movimientos financieros y consultar estados de cuenta mediante una API REST.

Fuente: `Prueba Tecnica.pdf`, páginas 1 a 3, proporcionado para esta prueba. Las referencias al PDF identifican requisitos originales; las decisiones aprobadas complementan o precisan ese alcance. Los puntos P-01 a P-07 fueron resueltos por aprobación expresa del responsable del proyecto y se registran en la sección 7.

El backend utilizará **Java, Spring Boot, PostgreSQL y arquitectura hexagonal**, por decisión aprobada. Este avance documenta el comportamiento esperado; no incluye código, configuración del proyecto ni diseño detallado de persistencia.

## 2. Requerimientos funcionales del PDF

| ID | Requerimiento original | Fuente |
| --- | --- | --- |
| RF-01 | Exponer servicios REST para el CRUD de clientes, productos y transacciones. La modificación y eliminación de transacciones se precisan en DA-01. | p. 1 |
| RF-02 | Crear clientes con, al menos: id, tipo y número de identificación, nombres, apellido, correo electrónico, fecha de nacimiento, fecha de creación y fecha de modificación. Permitir modificar y eliminar clientes. | p. 1 |
| RF-03 | Crear únicamente cuentas de ahorro o corrientes, vinculadas a un cliente existente. | p. 2 |
| RF-04 | Registrar en cada cuenta, al menos: id, tipo, número, estado (activa, inactiva o cancelada), saldo, indicador de exención GMF, fechas de creación y modificación y propietario. | p. 2 |
| RF-05 | Permitir activar, inactivar y cancelar cuentas bajo las restricciones de negocio. | pp. 2-3 |
| RF-06 | Registrar consignaciones, retiros y transferencias entre cuentas existentes; actualizar saldos y registrar débito en origen y crédito en destino para las transferencias. | p. 3 |
| RF-07 | Consultar estados de cuenta. El PDF menciona esta capacidad sin definir campos, filtros ni formato. | p. 1 |

## 3. Reglas y validaciones originales

| ID | Regla del PDF | Fuente |
| --- | --- | --- |
| RN-01 | Un cliente menor de edad no puede crearse ni existir en la base de datos. La restricción también debe conservarse al modificar su fecha de nacimiento. | p. 1 |
| RN-02 | No se puede eliminar un cliente con productos vinculados. El PDF no establece excepciones por estado del producto. | p. 1 |
| RN-03 | La fecha de creación del cliente se calcula automáticamente al registrarlo; la fecha de modificación se actualiza automáticamente con la última modificación. | p. 1 |
| RN-04 | Una cuenta solo puede existir vinculada a un cliente de la entidad. | p. 2 |
| RN-05 | Una cuenta de ahorro no puede tener saldo negativo. | p. 2 |
| RN-06 | Los números de cuenta son únicos, automáticos y de 10 dígitos: prefijo `53` para ahorros y `33` para corrientes. | p. 2 |
| RN-07 | Las cuentas de ahorro se crean activas. | p. 3 |
| RN-08 | Solo se pueden cancelar cuentas cuyo saldo sea cero. | p. 3 |
| RN-09 | La fecha de creación de una cuenta se calcula automáticamente al registrarla. | p. 3 |
| RN-10 | Cada transacción exitosa actualiza el saldo y el saldo disponible. Una transferencia genera un débito en la cuenta emisora y un crédito en la receptora, ambas existentes. | p. 3 |

El PDF permite activar o inactivar cuentas en cualquier momento. La decisión DA-03 precisa que una cuenta cancelada es definitiva y no puede reactivarse.

El formato válido de correo electrónico y la longitud mínima de dos caracteres para nombres y apellido aparecen como **opcionales en el PDF** (p. 1). Su implementación queda incluida por DA-05.

## 4. Decisiones iniciales aprobadas

| ID | Decisión acordada | Relación con el PDF |
| --- | --- | --- |
| DA-01 | Las transacciones exitosas son inmutables: no se modifican ni eliminan directamente. Los errores se corrigen mediante movimientos de reverso, conservando historial y trazabilidad. | Precisa el CRUD genérico solicitado. El reverso es una operación compensatoria total vinculada al original, sin duplicados y sujeta a fondos suficientes y cuentas activas. Las transferencias y sus reversos son atómicos (P-04). |
| DA-02 | No habrá sobregiros en ninguna cuenta. Retiros y transferencias requieren fondos suficientes. El saldo disponible será igual al saldo actual mientras no existan bloqueos o retenciones. | Extiende a cuentas corrientes la restricción de fondos; no se incluye gestión de bloqueos o retenciones en el alcance inicial. |
| DA-03 | Ambas clases de cuenta se crean activas. Las inactivas y canceladas no pueden emitir ni recibir movimientos financieros. Las inactivas pueden reactivarse; las canceladas no. | Amplía el estado inicial a cuentas corrientes y precisa operaciones según estado. Se mantiene la condición de saldo cero para cancelar. |
| DA-04 | Se almacenará `exentaGmf`, inicialmente `false` y modificable. Las modificaciones actualizarán automáticamente la fecha de modificación de la cuenta. No se implementará cálculo ni cobro de GMF (P-06). | El PDF exige el indicador, pero no define liquidación del impuesto. |
| DA-05 | Se rechazarán identificaciones duplicadas, importes menores o iguales a cero y transferencias a la misma cuenta. Se validarán el formato del correo y la longitud mínima de dos caracteres para nombres y apellido. | Agrega validaciones y adopta las dos validaciones opcionales del PDF. La identificación es única por tipo y número; se admiten cédula de ciudadanía, cédula de extranjería y pasaporte (P-02). |
| DA-06 | Se utilizarán Java, Spring Boot, PostgreSQL y arquitectura hexagonal. | Java es obligatorio; PostgreSQL es una opción permitida y la arquitectura hexagonal es preferida por el PDF. Spring Boot y la selección definitiva del stack son decisiones del proyecto. |

## 5. Requerimientos no funcionales y condiciones de entrega

| ID | Condición | Origen y alcance |
| --- | --- | --- |
| RNF-01 | Desarrollar al menos un backend en Java con una base de datos relacional y estructuras de persistencia suficientes para los requisitos obligatorios. | PDF, p. 3. PostgreSQL seleccionado en DA-06. |
| RNF-02 | Organizar el backend con responsabilidades de `entity`, `service`, `controller` y `repository`. | PDF, p. 3. Su correspondencia con puertos y adaptadores se explicará en el documento de arquitectura. |
| RNF-03 | Implementar pruebas unitarias con JUnit con cobertura de Service y Controller. | PDF, p. 3. No se exige un porcentaje numérico. |
| RNF-04 | Usar Git y un único repositorio en GitHub; evidenciar avances mediante commits y push y una administración organizada de ramas. | PDF, pp. 1 y 3. Cada commit y push requiere autorización previa del responsable del proyecto. |
| RNF-05 | Entregar la prueba dockerizada y explicar arquitectura, patrones utilizados, SOLID, ACID y DML/DDL. | Lista inicial del PDF, p. 1. Las decisiones y evidencias se desarrollarán en avances posteriores. |
| RNF-06 | El PDF menciona “prueba con componentes”, “Front” y servicios de nube utilizados. | PDF, p. 1. P-07 concreta el alcance aprobado: frontend funcional, pruebas unitarias y de integración, Docker Compose y nube opcional. |

El PDF no establece metas cuantitativas de rendimiento, disponibilidad o cobertura, ni define autenticación y autorización. No se presentan como compromisos aprobados.

## 6. Criterios de aceptación iniciales

Estos criterios describen resultados observables e incorporan las decisiones aprobadas P-01 a P-07.

| ID | Escenario y resultado esperado | Reglas relacionadas |
| --- | --- | --- |
| CA-01 | Un cliente con datos válidos y edad permitida puede registrarse y consultarse; la creación se fecha automáticamente. Una modificación válida actualiza su información y fecha de modificación. | RF-01, RF-02, RN-01, RN-03 |
| CA-02 | Se rechaza crear o modificar un cliente si tiene menos de 18 años cumplidos según la fecha actual en `America/Bogota`; al cumplir 18 años satisface la regla de edad; también se rechazan identificación duplicada, correo inválido o nombres/apellido de menos de dos caracteres. | RN-01, DA-05, P-01, P-02 |
| CA-03 | Un cliente sin productos vinculados puede eliminarse; con productos vinculados, la eliminación se rechaza, incluso si están cancelados. | RN-02 |
| CA-04 | Al crear una cuenta para un cliente existente, queda activa, con saldo COP 0.00, fecha automática y número único de 10 dígitos con el prefijo correspondiente. Un tipo no permitido o un propietario inexistente impiden su creación. | RF-03, RN-04, RN-06, RN-09, DA-03, P-03 |
| CA-05 | Una cuenta activa puede inactivarse y una inactiva reactivarse. Cancelar exige saldo cero; una cuenta cancelada no puede reactivarse. | RF-05, RN-08, DA-03 |
| CA-06 | Una consignación válida incrementa saldo actual y disponible; un retiro válido los reduce. Los importes no positivos o fondos insuficientes se rechazan sin alterar saldos. | RN-10, DA-02, DA-05 |
| CA-07 | Una transferencia válida entre cuentas distintas, existentes y activas registra débito y crédito por el importe y actualiza ambos saldos. Si no puede completarse, no debe dejar efectos parciales. | RN-10, DA-02, DA-03, DA-05, RNF-05 (ACID) |
| CA-08 | Se rechaza cualquier movimiento que involucre una cuenta inactiva o cancelada, tanto como origen como destino. | DA-03 |
| CA-09 | No se permite editar o eliminar una transacción exitosa. Su corrección conserva el original y registra un único reverso total vinculado a él. Se rechazan reversos duplicados, parciales, sin fondos suficientes o con cuentas inactivas/canceladas; el rechazo no altera saldos. El reverso de una transferencia compensa ambas cuentas de manera atómica. | DA-01, DA-02, DA-03, P-04 |
| CA-10 | La cuenta se crea con `exentaGmf=false`; actualizarlo conserva el nuevo valor y actualiza automáticamente la fecha de modificación. Los movimientos no calculan ni descuentan GMF. | DA-04, P-06 |
| CA-11 | Se pueden consultar clientes y cuentas registrados. Los estados de cuenta incluyen saldo e historial de movimientos. | RF-01, RF-07, P-05 |
| CA-12 | Se rechaza modificar propietario, tipo o número de cuenta. No se elimina físicamente una cuenta; su cancelación conserva el historial y requiere saldo cero. | P-05, RN-08 |
| CA-13 | Se rechaza repetir una combinación de tipo y número de identificación y usar un tipo no admitido. El mismo número con un tipo admitido distinto no se rechaza por duplicidad. | P-02 |
| CA-14 | Los importes se manejan en COP con `BigDecimal` y dos decimales. Abrir una cuenta deja saldo cero; una consignación posterior genera un movimiento independiente. | P-03 |
| CA-15 | El frontend permite gestionar clientes y cuentas y operar y consultar transacciones conforme a estas reglas. Docker Compose permite ejecutar backend y PostgreSQL; desplegar en nube no es condición de aceptación. | P-07 |
| CA-16 | Cada funcionalidad incorpora sus pruebas en la rama hija de `develop`: unitarias con JUnit 5 y Mockito e integración cuando corresponda. Se presentan resultados reales antes de solicitar su integración. | RNF-03, P-07, sección 8 |

## 7. Resolución aprobada de P-01 a P-07

Todos los puntos de esta sección están **resueltos y aprobados**. Se conservan sus identificadores para mantener la trazabilidad de la revisión.

| ID | Decisión aprobada |
| --- | --- |
| P-01 | Edad mínima de 18 años cumplidos, calculada con la fecha actual de Colombia (`America/Bogota`), tanto al crear como al modificar un cliente. |
| P-02 | Identificación única por combinación de tipo y número. Tipos admitidos: cédula de ciudadanía, cédula de extranjería y pasaporte. |
| P-03 | Moneda COP, importes representados con `BigDecimal` y dos decimales. Las cuentas comienzan con saldo cero; las consignaciones son transacciones independientes de la apertura. |
| P-04 | Movimientos inmutables y reversos únicamente totales, vinculados al original y sin reversos duplicados. Los reversos respetan fondos suficientes y estados de cuenta. Transferencias y reversos de transferencias garantizan atomicidad. |
| P-05 | Las cuentas se consultan y permiten actualizar sus atributos autorizados, incluidos estado y `exentaGmf`; propietario, tipo y número no pueden modificarse. El saldo cambia mediante movimientos financieros. La cancelación sustituye la eliminación física y exige saldo cero. Los estados de cuenta incluyen saldo e historial de movimientos. |
| P-06 | `exentaGmf` es `false` por defecto y puede actualizarse. Las modificaciones de la cuenta actualizan automáticamente su fecha de modificación. No se calcula ni cobra el impuesto. |
| P-07 | Frontend funcional para clientes, cuentas y transacciones; pruebas unitarias y de integración; Docker Compose para ejecutar backend y PostgreSQL. Los servicios de nube son opcionales. |

El CRUD genérico de RF-01 se adapta por decisión aprobada: las transacciones exitosas se corrigen mediante reversos y las cuentas se cancelan en lugar de eliminarse físicamente. Estas adaptaciones no son formulaciones literales del PDF; preservan el historial financiero. El requisito original permanece documentado.

## 8. Condiciones aprobadas de desarrollo y entrega

Las pruebas se desarrollarán junto con cada funcionalidad en la rama hija de `develop`, utilizando JUnit 5 y Mockito para pruebas unitarias y pruebas de integración cuando corresponda. QA volverá a ejecutarlas y añadirá regresión y validación de criterios de aceptación según el cambio. No se difiere la creación de tests a QA.

Se conservarán las ramas permanentes `main`, `develop` y `qa`. El flujo aprobado es:

1. Trabajar incrementalmente en una rama hija de `develop`, mostrar cambios y resultados reales, y obtener autorización antes de commit y push. Tras el push autorizado, preparar un PR hacia `develop`; el merge requiere autorización.
2. Para QA, actualizar `qa` desde `origin/qa`, crear una rama `qa/...` y promocionar los commits aprobados en orden, con sus dependencias y tests, mediante `git cherry-pick -x`. Ejecutar las validaciones y preparar un PR hacia `qa`, sujeto a autorización de merge. Si fallan pruebas, detener la promoción; las correcciones funcionales se originan en una rama hija de `develop`.
3. Para una versión validada en QA, crear `release/...` desde `main`, promocionar los commits aprobados mediante `git cherry-pick -x`, comprobar dependencias y evitar duplicados, ejecutar validaciones finales y preparar un PR hacia `main`. Integrar o publicar requiere autorización.

No se permiten commits directos en ramas permanentes, merges directos entre ellas, merges automáticos ni `push --force`. Eliminar ramas requiere autorización. Si se usa squash merge, se identificará su commit resultante antes de promocionarlo. Cada cambio de etapa requiere autorización.

Estas condiciones se detallarán en el futuro documento de pruebas y versionamiento. En este avance solo se valida documentación; la implementación y su configuración comenzarán después de aprobar la documentación.
