-- Etapa 3: cuentas de ahorro y corrientes (RF-03 a RF-05, RN-02, RN-04 a RN-09, DA-03, DA-04).

-- Sufijo de ocho dígitos por tipo; no se recicla (RN-06).
CREATE SEQUENCE cuenta_ahorros_seq MAXVALUE 99999999 NO CYCLE;
CREATE SEQUENCE cuenta_corriente_seq MAXVALUE 99999999 NO CYCLE;

CREATE TABLE cuenta (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id         bigint        NOT NULL,
    tipo               varchar(10)   NOT NULL,
    numero             varchar(10)   NOT NULL,
    estado             varchar(10)   NOT NULL DEFAULT 'ACTIVA',
    saldo              numeric(19,2) NOT NULL DEFAULT 0,
    exenta_gmf         boolean       NOT NULL DEFAULT false,
    fecha_creacion     timestamptz   NOT NULL DEFAULT now(),
    fecha_modificacion timestamptz,
    -- RN-02 y RN-04: el propietario existe y no se elimina mientras tenga cuentas, aunque estén canceladas.
    CONSTRAINT fk_cuenta_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE RESTRICT,
    CONSTRAINT uk_cuenta_numero UNIQUE (numero),
    CONSTRAINT ck_cuenta_tipo CHECK (tipo IN ('AHORROS', 'CORRIENTE')),
    CONSTRAINT ck_cuenta_estado CHECK (estado IN ('ACTIVA', 'INACTIVA', 'CANCELADA')),
    -- RN-05 y DA-02: sin saldos negativos.
    CONSTRAINT ck_cuenta_saldo_no_negativo CHECK (saldo >= 0),
    -- RN-06: diez dígitos con prefijo 53 (ahorros) o 33 (corriente).
    CONSTRAINT ck_cuenta_numero_por_tipo CHECK (
        (tipo = 'AHORROS'   AND numero ~ '^53[0-9]{8}$') OR
        (tipo = 'CORRIENTE' AND numero ~ '^33[0-9]{8}$')),
    -- RN-08: solo se cancela con saldo cero.
    CONSTRAINT ck_cuenta_cancelada_sin_saldo CHECK (estado <> 'CANCELADA' OR saldo = 0)
);

CREATE INDEX idx_cuenta_cliente ON cuenta (cliente_id);
