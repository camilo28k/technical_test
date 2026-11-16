# Contrato de la API REST

Propuesta para revisión. Define rutas, cuerpos y errores que comparten backend y frontend. Las reglas provienen de [requerimientos y reglas](01-requerimientos-y-reglas.md); los nombres de recursos siguen el [modelo de datos](03-modelo-de-datos.md) y las [historias de usuario](06-historias-de-usuario.md).

## 1. Convenciones

| Aspecto | Decisión |
| --- | --- |
| Base | `/api/v1`. JSON en UTF-8 (`Content-Type: application/json`). |
| Nombres | Campos en `camelCase`; valores enumerados en `MAYUSCULAS`. |
| Importes | Texto decimal con dos decimales (`"150000.00"`), COP. Evita pérdida de precisión en clientes JavaScript; el backend lo convierte a `BigDecimal` (P-03). Se rechazan más de dos decimales. |
| Fechas | `fechaNacimiento` como `yyyy-MM-dd`. Fechas de auditoría en ISO-8601 con desplazamiento (`2026-11-16T10:30:00-05:00`). |
| Campos calculados | `id`, `fechaCreacion`, `fechaModificacion`, `numero`, `estado` y `saldo` los asigna el sistema; si llegan en una petición se ignoran. |
| Paginación | Listados con `page` (desde 0, por defecto 0) y `size` (por defecto 20, máximo 100). Respuesta: `{ "contenido": [...], "page", "size", "totalElementos", "totalPaginas" }`. |
| Autenticación | Fuera del alcance (el PDF no la define). |

Enumerados:

- `tipoIdentificacion`: `CC`, `CE`, `PASAPORTE`.
- `tipoCuenta`: `AHORROS`, `CORRIENTE`.
- `estadoCuenta`: `ACTIVA`, `INACTIVA`, `CANCELADA`.
- `claseOperacion`: `CONSIGNACION`, `RETIRO`, `TRANSFERENCIA`, `REVERSO`.
- `sentido`: `DEBITO`, `CREDITO`.

## 2. Clientes

| Método y ruta | Uso | Éxito | Errores principales |
| --- | --- | --- | --- |
| `POST /clientes` | Registrar cliente | `201` + `Location` + cliente | `400`, `409 CLIENTE_MENOR_DE_EDAD`, `409 IDENTIFICACION_DUPLICADA` |
| `GET /clientes` | Listar (paginado; filtro opcional `numeroIdentificacion`) | `200` | — |
| `GET /clientes/{id}` | Consultar ficha | `200` | `404 CLIENTE_NO_ENCONTRADO` |
| `PUT /clientes/{id}` | Actualizar ficha completa | `200` + cliente | `400`, `404`, `409 CLIENTE_MENOR_DE_EDAD`, `409 IDENTIFICACION_DUPLICADA` |
| `DELETE /clientes/{id}` | Eliminar cliente sin productos | `204` | `404`, `409 CLIENTE_CON_PRODUCTOS` |
| `GET /clientes/{id}/cuentas` | Cuentas del cliente, incluidas canceladas | `200` (lista) | `404` |

Petición de alta y actualización:

```json
{
  "tipoIdentificacion": "CC",
  "numeroIdentificacion": "1075123456",
  "nombres": "Ana María",
  "apellido": "Gómez",
  "correo": "ana.gomez@correo.com",
  "fechaNacimiento": "1995-04-12"
}
```

Validaciones de formato (`400`): todos los campos obligatorios; `nombres` y `apellido` de 2 a 100 caracteres; `correo` con formato `xxxx@xxxxx.xxx`; `numeroIdentificacion` de 1 a 20 caracteres. La edad mínima (P-01) es regla de negocio y responde `409`.

Respuesta:

```json
{
  "id": 1,
  "tipoIdentificacion": "CC",
  "numeroIdentificacion": "1075123456",
  "nombres": "Ana María",
  "apellido": "Gómez",
  "correo": "ana.gomez@correo.com",
  "fechaNacimiento": "1995-04-12",
  "fechaCreacion": "2026-11-16T10:30:00-05:00",
  "fechaModificacion": null
}
```

## 3. Cuentas

No existe `DELETE`: la cuenta se cancela (P-05). Propietario, tipo y número no se modifican.

| Método y ruta | Uso | Éxito | Errores principales |
| --- | --- | --- | --- |
| `POST /cuentas` | Abrir cuenta | `201` + `Location` + cuenta | `400`, `404 CLIENTE_NO_ENCONTRADO` |
| `GET /cuentas` | Listar (paginado; filtros opcionales `clienteId`, `estado`) | `200` | — |
| `GET /cuentas/{id}` | Consultar cuenta | `200` | `404 CUENTA_NO_ENCONTRADA` |
| `GET /cuentas/numero/{numero}` | Buscar por número | `200` | `404` |
| `PATCH /cuentas/{id}` | Actualizar `exentaGmf` | `200` | `400`, `404`, `409 CUENTA_CANCELADA` |
| `POST /cuentas/{id}/activar` | Reactivar cuenta inactiva | `200` | `404`, `409 CUENTA_CANCELADA` |
| `POST /cuentas/{id}/inactivar` | Inactivar cuenta activa | `200` | `404`, `409 CUENTA_CANCELADA` |
| `POST /cuentas/{id}/cancelar` | Cancelar cuenta con saldo cero | `200` | `404`, `409 SALDO_DISTINTO_DE_CERO`, `409 CUENTA_CANCELADA` |
| `GET /cuentas/{id}/estado-de-cuenta` | Saldo e historial (sección 5) | `200` | `404` |

Activar una cuenta ya activa, o inactivar una ya inactiva, responde `200` sin cambios (operación idempotente) y no altera `fechaModificacion`.

Apertura: `{ "clienteId": 1, "tipoCuenta": "AHORROS" }`. Un `tipoCuenta` distinto de los admitidos responde `400`.

Actualización: `{ "exentaGmf": true }`.

Respuesta:

```json
{
  "id": 10,
  "clienteId": 1,
  "tipoCuenta": "AHORROS",
  "numero": "5300000001",
  "estado": "ACTIVA",
  "saldo": "0.00",
  "saldoDisponible": "0.00",
  "exentaGmf": false,
  "fechaCreacion": "2026-11-16T10:35:00-05:00",
  "fechaModificacion": null
}
```

`saldoDisponible` se expone para cumplir el PDF; mientras no existan retenciones es igual a `saldo` (DA-02).

## 4. Operaciones financieras

Las operaciones identifican cuentas por su `numero`, que es el dato con el que trabaja el empleado. No existen `PUT` ni `DELETE`: los errores se corrigen con un reverso (DA-01).

| Método y ruta | Uso | Éxito | Errores principales |
| --- | --- | --- | --- |
| `POST /operaciones/consignaciones` | Consignar | `201` + operación | `400`, `404 CUENTA_NO_ENCONTRADA`, `409 CUENTA_NO_ACTIVA` |
| `POST /operaciones/retiros` | Retirar | `201` | `400`, `404`, `409 CUENTA_NO_ACTIVA`, `409 FONDOS_INSUFICIENTES` |
| `POST /operaciones/transferencias` | Transferir | `201` | `400`, `404`, `409 MISMA_CUENTA`, `409 CUENTA_NO_ACTIVA`, `409 FONDOS_INSUFICIENTES` |
| `POST /operaciones/{id}/reverso` | Reversar una operación completa | `201` + operación de reverso | `404 OPERACION_NO_ENCONTRADA`, `409 OPERACION_YA_REVERSADA`, `409 REVERSO_NO_REVERSABLE`, `409 CUENTA_NO_ACTIVA`, `409 FONDOS_INSUFICIENTES` |
| `GET /operaciones/{id}` | Consultar operación y sus movimientos | `200` | `404` |

Peticiones:

```json
{ "numeroCuenta": "5300000001", "importe": "150000.00" }
```

```json
{ "numeroCuentaOrigen": "5300000001", "numeroCuentaDestino": "3300000004", "importe": "30000.00" }
```

El reverso no lleva cuerpo. Un reverso no puede reversarse (`REVERSO_NO_REVERSABLE`). Un `importe` menor o igual a cero, o con más de dos decimales, responde `400`.

Respuesta (transferencia):

```json
{
  "id": 501,
  "clase": "TRANSFERENCIA",
  "importe": "30000.00",
  "fecha": "2026-11-16T11:00:00-05:00",
  "operacionOriginalId": null,
  "movimientos": [
    { "id": 901, "numeroCuenta": "5300000001", "sentido": "DEBITO", "saldoResultante": "120000.00" },
    { "id": 902, "numeroCuenta": "3300000004", "sentido": "CREDITO", "saldoResultante": "30000.00" }
  ]
}
```

`saldoResultante` es el saldo de la cuenta tras confirmar la operación; se calcula en la respuesta y no se almacena. El `201` se emite solo después del commit (ADR-002).

## 5. Estado de cuenta

`GET /cuentas/{id}/estado-de-cuenta?desde=2026-11-01&hasta=2026-11-30&page=0&size=20`

`desde` y `hasta` son opcionales (fechas civiles de Colombia, ambos inclusive); sin ellos se devuelve todo el historial. `desde` posterior a `hasta` responde `400`. El historial se ordena del más reciente al más antiguo, con el id del movimiento como desempate.

```json
{
  "cuenta": { "id": 10, "numero": "5300000001", "tipoCuenta": "AHORROS", "estado": "ACTIVA", "titular": "Ana María Gómez" },
  "saldo": "120000.00",
  "saldoDisponible": "120000.00",
  "fechaConsulta": "2026-11-16T11:05:00-05:00",
  "movimientos": {
    "contenido": [
      { "operacionId": 501, "clase": "TRANSFERENCIA", "sentido": "DEBITO", "importe": "30000.00", "fecha": "2026-11-16T11:00:00-05:00", "contraparte": "3300000004", "reversada": false }
    ],
    "page": 0, "size": 20, "totalElementos": 2, "totalPaginas": 1
  }
}
```

Una cuenta sin movimientos devuelve `contenido` vacío y saldo `"0.00"` (CA-11).

## 6. Errores

Todas las respuestas de error usan el mismo cuerpo:

```json
{
  "codigo": "FONDOS_INSUFICIENTES",
  "mensaje": "La cuenta 5300000001 no tiene fondos suficientes para retirar 200000.00 COP.",
  "fecha": "2026-11-16T11:10:00-05:00",
  "ruta": "/api/v1/operaciones/retiros",
  "detalles": []
}
```

En `400 VALIDACION`, `detalles` lista `{ "campo": "correo", "mensaje": "formato inválido" }`. Nunca se exponen trazas ni SQL.

| HTTP | Código | Cuándo |
| --- | --- | --- |
| 400 | `VALIDACION` | Campo ausente, formato inválido, enumerado desconocido, importe no positivo, rango de fechas inválido. |
| 400 | `CUERPO_ILEGIBLE` | JSON mal formado. |
| 404 | `CLIENTE_NO_ENCONTRADO`, `CUENTA_NO_ENCONTRADA`, `OPERACION_NO_ENCONTRADA` | El recurso no existe. |
| 409 | `CLIENTE_MENOR_DE_EDAD` | Menor de 18 años en Colombia al crear o actualizar (P-01). |
| 409 | `IDENTIFICACION_DUPLICADA` | Par tipo/número ya registrado (P-02). |
| 409 | `CLIENTE_CON_PRODUCTOS` | Eliminar cliente con cuentas, incluso canceladas (RN-02). |
| 409 | `CUENTA_NO_ACTIVA` | Movimiento sobre cuenta inactiva o cancelada (DA-03). |
| 409 | `CUENTA_CANCELADA` | Cambio de estado o atributos sobre una cuenta cancelada. |
| 409 | `SALDO_DISTINTO_DE_CERO` | Cancelar con saldo distinto de cero (RN-08). |
| 409 | `FONDOS_INSUFICIENTES` | Retiro, transferencia o reverso sin fondos (DA-02). |
| 409 | `MISMA_CUENTA` | Transferencia con origen igual a destino (DA-05). |
| 409 | `OPERACION_YA_REVERSADA` | Segundo reverso del mismo original (P-04). |
| 409 | `REVERSO_NO_REVERSABLE` | Intento de reversar un reverso. |
| 409 | `CONFLICTO_CONCURRENCIA` | Bloqueo no resuelto; la operación se revirtió y puede reintentarse. |
| 500 | `ERROR_INTERNO` | Fallo no previsto; mensaje genérico. |

## 7. Documentación ejecutable y trazabilidad

Se propone publicar el contrato con springdoc-openapi (`/swagger-ui.html`) para que el evaluador pruebe la API sin el frontend. Las pruebas de controlador (doc 05) verifican estado HTTP, cuerpo y código de error de cada fila de las tablas anteriores.

| Historia | Endpoints |
| --- | --- |
| HU-01 a HU-03 | Sección 2 |
| HU-04 a HU-06 | Sección 3 |
| HU-07 a HU-10 | Sección 4 |
| HU-11 | Sección 5 |
