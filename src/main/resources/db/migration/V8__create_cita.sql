CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE cita (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES cliente(id),
    mascota_id BIGINT NOT NULL REFERENCES mascota(id),
    veterinario_id BIGINT NOT NULL REFERENCES veterinario(id),
    servicio_id BIGINT NOT NULL REFERENCES servicio(id),
    fecha_hora_inicio TIMESTAMP NOT NULL,
    fecha_hora_fin TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE cita
    ADD CONSTRAINT no_doble_reserva
    EXCLUDE USING gist (
        veterinario_id WITH =,
        tsrange(fecha_hora_inicio, fecha_hora_fin, '[)') WITH &&
    )
    WHERE (estado = 'CONFIRMADA');