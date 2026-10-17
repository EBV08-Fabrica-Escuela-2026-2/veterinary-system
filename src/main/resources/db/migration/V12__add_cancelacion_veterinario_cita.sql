ALTER TABLE cita ALTER COLUMN estado TYPE VARCHAR(30);

ALTER TABLE cita
    ADD COLUMN motivo_cancelacion VARCHAR(240),
    ADD COLUMN cancelada_por_veterinario_id BIGINT REFERENCES veterinario(id),
    ADD COLUMN cancelada_at TIMESTAMP;