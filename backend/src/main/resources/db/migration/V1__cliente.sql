-- Etapa 2: clientes (RF-02, RN-01, RN-03, P-01, P-02).

CREATE TABLE cliente (
    id                     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_identificacion    varchar(10)  NOT NULL CHECK (tipo_identificacion IN ('CC', 'CE', 'PASAPORTE')),
    numero_identificacion  varchar(20)  NOT NULL CHECK (length(trim(numero_identificacion)) > 0),
    nombres                varchar(100) NOT NULL CHECK (length(trim(nombres)) >= 2),
    apellido               varchar(100) NOT NULL CHECK (length(trim(apellido)) >= 2),
    correo                 varchar(254) NOT NULL,
    fecha_nacimiento       date         NOT NULL,
    fecha_creacion         timestamptz  NOT NULL DEFAULT now(),
    fecha_modificacion     timestamptz,
    CONSTRAINT uk_cliente_identificacion UNIQUE (tipo_identificacion, numero_identificacion)
);

-- Un cliente menor de edad no puede existir en la base de datos (RN-01), ni siquiera por una
-- escritura directa. La edad se calcula con la fecha civil de Colombia (P-01).
CREATE FUNCTION validar_mayoria_de_edad() RETURNS trigger AS $$
BEGIN
    IF NEW.fecha_nacimiento > ((now() AT TIME ZONE 'America/Bogota')::date - INTERVAL '18 years')::date THEN
        RAISE EXCEPTION 'ck_cliente_mayor_de_edad: el cliente debe tener al menos 18 años'
            USING ERRCODE = 'check_violation', CONSTRAINT = 'ck_cliente_mayor_de_edad';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_cliente_mayor_de_edad
    BEFORE INSERT OR UPDATE OF fecha_nacimiento ON cliente
    FOR EACH ROW EXECUTE FUNCTION validar_mayoria_de_edad();
