# Historias de usuario en Gherkin

Cada bloque describe una capacidad desde la perspectiva del trabajador de la entidad. Las etiquetas enlazan los escenarios con los [criterios de aceptación](01-requerimientos-y-reglas.md). Las etapas corresponden al [plan de implementación](04-plan-de-implementacion.md).

## HU-01 · Registrar un cliente

Etapa 2.

```gherkin
# language: es
@HU-01 @CA-01 @CA-02 @CA-13
Característica: Registrar un cliente
  Como trabajador de la entidad financiera
  Quiero registrar al cliente
  Para abrir una ficha válida

  Escenario: Alta el día del cumpleaños
    Dado que hoy es 3 de noviembre de 2026 en Colombia y el cliente nació el 3 de noviembre de 2008
    Cuando registro sus datos válidos con una identificación nueva
    Entonces puedo consultar la ficha con su fecha de creación automática

  Escenario: Identificación repetida
    Dado que ya existe un cliente con la cédula de ciudadanía 12345
    Cuando intento registrar otro cliente con esa misma combinación de tipo y número
    Entonces el registro se rechaza y sigue existiendo una sola ficha con esa identificación
```

## HU-02 · Actualizar la ficha

Etapa 2.

```gherkin
# language: es
@HU-02 @CA-01 @CA-02 @CA-11 @CA-13
Característica: Actualizar la ficha
  Como trabajador de la entidad financiera
  Quiero actualizar los datos del cliente
  Para atenderlo con información vigente

  Escenario: Cambio de correo
    Dado que el cliente tiene una ficha registrada
    Cuando actualizo su correo con una dirección válida
    Entonces al consultar la ficha veo el nuevo correo y la fecha de la modificación

  Escenario: Corrección inválida
    Dado que el cliente tiene una fecha de nacimiento válida
    Cuando intento cambiarla por una fecha que lo haría menor de 18 años
    Entonces la actualización se rechaza y se conserva la fecha registrada
```

## HU-03 · Eliminar un cliente

Etapa 2 y 3.

```gherkin
# language: es
@HU-03 @CA-03
Característica: Eliminar un cliente
  Como trabajador de la entidad financiera
  Quiero tramitar la baja del cliente
  Para retirar su ficha cuando ya no tenga productos vinculados

  Escenario: Cliente sin productos
    Dado que el cliente no tiene cuentas vinculadas
    Cuando solicito eliminar su ficha
    Entonces la eliminación se confirma y la ficha deja de aparecer en la consulta

  Escenario: Cuenta cancelada vinculada
    Dado que el cliente conserva una cuenta cancelada
    Cuando solicito eliminar su ficha
    Entonces la solicitud se rechaza y la cuenta sigue vinculada al cliente
```

## HU-04 · Abrir una cuenta

Etapa 3.

```gherkin
# language: es
@HU-04 @CA-04 @CA-10 @CA-14
Característica: Abrir una cuenta
  Como trabajador de la entidad financiera
  Quiero abrir una cuenta para un cliente registrado
  Para entregarle un número con el que pueda operar

  Escenario: Apertura de ahorros
    Dado que el cliente está registrado
    Cuando abro una cuenta de ahorros para él
    Entonces la cuenta queda activa con saldo de 0,00 COP y sin exención de GMF
    Y recibe un número único de diez dígitos que empieza por 53

  Escenario: Apertura de cuenta corriente
    Dado que el cliente está registrado
    Cuando abro una cuenta corriente para él
    Entonces la cuenta queda activa con saldo de 0,00 COP y un número único de diez dígitos que empieza por 33
```

## HU-05 · Cambiar el estado de una cuenta

Etapa 3 y 4.

```gherkin
# language: es
@HU-05 @CA-05 @CA-08 @CA-12
Característica: Cambiar el estado de una cuenta
  Como trabajador de la entidad financiera
  Quiero gestionar el estado de una cuenta
  Para controlar cuándo admite movimientos

  Escenario: Reactivar una cuenta
    Dado que la cuenta está inactiva
    Cuando solicito reactivarla
    Entonces la cuenta queda activa

  Escenario: Cancelar una cuenta sin saldo
    Dado que la cuenta está activa y tiene saldo cero
    Cuando solicito cancelarla
    Entonces queda registrada como cancelada y se rechaza cualquier intento posterior de reactivación

  Escenario: Cancelación con saldo
    Dado que la cuenta activa conserva 10,00 COP
    Cuando solicito cancelarla
    Entonces la cancelación se rechaza y la cuenta conserva su estado y saldo
```

## HU-06 · Actualizar la exención de GMF

Etapa 3.

```gherkin
# language: es
@HU-06 @CA-10 @CA-11 @CA-12
Característica: Actualizar la exención de GMF
  Como trabajador de la entidad financiera
  Quiero actualizar la exención de GMF
  Para mantener vigente ese dato de la cuenta

  Escenario: Cambio de exención
    Dado que la cuenta está registrada sin exención
    Cuando activo el indicador de exención
    Entonces al consultar la cuenta veo el indicador activo y la fecha de modificación actualizada
    Y conserva su propietario, tipo y número
```

## HU-07 · Consignar dinero

Etapa 4.

```gherkin
# language: es
@HU-07 @CA-06 @CA-08 @CA-14
Característica: Consignar dinero
  Como trabajador de la entidad financiera
  Quiero registrar el dinero recibido
  Para abonarlo a la cuenta y dejar constancia del ingreso

  Escenario: Primera consignación
    Dado que la cuenta está activa y tiene saldo cero
    Cuando consigno 100,00 COP
    Entonces el saldo actual y el disponible quedan en 100,00 COP
    Y el historial muestra un crédito de 100,00 COP independiente de la apertura

  Escenario: Cuenta inactiva
    Dado que la cuenta está inactiva y tiene saldo cero
    Cuando intento consignar 10,00 COP
    Entonces la consignación se rechaza y se conservan el saldo y el historial
```

## HU-08 · Retirar dinero

Etapa 4.

```gherkin
# language: es
@HU-08 @CA-06 @CA-08
Característica: Retirar dinero
  Como trabajador de la entidad financiera
  Quiero registrar un retiro
  Para entregar dinero disponible en una cuenta activa

  Escenario: Retiro con fondos
    Dado que la cuenta está activa y dispone de 100,00 COP
    Cuando retiro 80,00 COP
    Entonces el saldo actual y el disponible quedan en 20,00 COP y se registra un débito de 80,00 COP

  Escenario: Fondos insuficientes
    Dado que la cuenta activa dispone de 20,00 COP
    Cuando intento retirar 30,00 COP
    Entonces el retiro se rechaza y el saldo sigue en 20,00 COP sin un nuevo débito
```

## HU-09 · Transferir entre cuentas

Etapa 5.

```gherkin
# language: es
@HU-09 @CA-07 @CA-08
Característica: Transferir entre cuentas
  Como trabajador de la entidad financiera
  Quiero transferir dinero entre cuentas
  Para completar el envío con el débito y el crédito correspondientes

  Escenario: Transferencia completa
    Dado que las cuentas A y B están activas y sus saldos son 100,00 y 0,00 COP
    Cuando transfiero 30,00 COP de A a B
    Entonces A queda con 70,00 COP y B con 30,00 COP
    Y se registran un débito en A y un crédito en B por 30,00 COP

  Escenario: Fallo al registrar el crédito
    Dado que A dispone de 100,00 COP y B está activa con saldo cero
    Cuando intento transferir 30,00 COP de A a B y falla el registro del crédito
    Entonces la transferencia se rechaza y ambas cuentas conservan sus saldos e historiales iniciales
```

## HU-10 · Reversar una operación

Etapa 6.

```gherkin
# language: es
@HU-10 @CA-08 @CA-09
Característica: Reversar una operación
  Como trabajador de la entidad financiera
  Quiero corregir una transferencia registrada por error
  Para compensar sus efectos y conservar el historial

  Escenario: Reverso total
    Dado que una transferencia de 30,00 COP de A a B está confirmada, ambas cuentas siguen activas y B dispone de 30,00 COP
    Cuando solicito el reverso total
    Entonces se debitan 30,00 COP de B y se acreditan 30,00 COP a A en una sola operación
    Y se conserva la transferencia original con un único reverso vinculado

  Escenario: Reverso duplicado
    Dado que la transferencia ya tiene un reverso confirmado
    Cuando intento reversarla de nuevo
    Entonces la solicitud se rechaza y los saldos e historiales se conservan
```

## HU-11 · Consultar el estado de cuenta

Etapa 7.

```gherkin
# language: es
@HU-11 @CA-09 @CA-11
Característica: Consultar el estado de cuenta
  Como trabajador de la entidad financiera
  Quiero consultar el estado de cuenta
  Para explicar al cliente su saldo y los movimientos que lo componen

  @CA-15
  Escenario: Consulta desde el frontend
    Dado que una cuenta recibió una consignación de 100,00 COP, envió 30,00 COP y recuperó ese importe mediante un reverso
    Cuando consulto su estado de cuenta desde el frontend
    Entonces veo un saldo de 100,00 COP y el historial con la consignación, el débito y el crédito del reverso

  Escenario: Cuenta recién abierta
    Dado que la cuenta acaba de abrirse con saldo cero
    Cuando consulto su estado de cuenta
    Entonces veo 0,00 COP de saldo y un historial vacío
```

En la etapa 8 se verifica el recorrido desde el frontend (CA-15). CA-16 aplica a cada incremento y exige desarrollar sus pruebas en la misma rama que la funcionalidad. La evidencia esperada se define en la [estrategia de pruebas](05-pruebas-y-versionamiento.md).
