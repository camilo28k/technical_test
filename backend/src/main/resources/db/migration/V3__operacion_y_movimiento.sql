-- Etapa 4: historial inmutable de operaciones financieras (RF-06, RN-10, DA-01, P-04).

CREATE TABLE operacion (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clase       varchar(15)   NOT NULL,
    importe     numeric(19,2) NOT NULL,
    fecha       timestamptz   NOT NULL DEFAULT now(),
    original_id bigint,
    CONSTRAINT ck_operacion_clase CHECK (clase IN ('CONSIGNACION', 'RETIRO', 'TRANSFERENCIA', 'REVERSO')),
    -- DA-05: importes estrictamente positivos.
    CONSTRAINT ck_operacion_importe_positivo CHECK (importe > 0),
    -- P-04: solo un reverso referencia un original, y como máximo uno por original.
    CONSTRAINT fk_operacion_original FOREIGN KEY (original_id) REFERENCES operacion (id) ON DELETE RESTRICT,
    CONSTRAINT uk_operacion_original UNIQUE (original_id),
    CONSTRAINT ck_operacion_reverso_con_original CHECK ((clase = 'REVERSO') = (original_id IS NOT NULL)),
    CONSTRAINT ck_operacion_sin_autorreferencia CHECK (original_id <> id)
);

CREATE TABLE movimiento (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operacion_id bigint      NOT NULL,
    cuenta_id    bigint      NOT NULL,
    sentido      varchar(7)  NOT NULL,
    CONSTRAINT fk_movimiento_operacion FOREIGN KEY (operacion_id) REFERENCES operacion (id) ON DELETE RESTRICT,
    -- Una cuenta con movimientos no se elimina: conserva su historial.
    CONSTRAINT fk_movimiento_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta (id) ON DELETE RESTRICT,
    CONSTRAINT ck_movimiento_sentido CHECK (sentido IN ('DEBITO', 'CREDITO')),
    CONSTRAINT uk_movimiento_operacion_cuenta UNIQUE (operacion_id, cuenta_id)
);

CREATE INDEX idx_movimiento_cuenta_operacion ON movimiento (cuenta_id, operacion_id);
