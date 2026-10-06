CREATE TABLE horario_atencion (
    id BIGSERIAL PRIMARY KEY,
    veterinario_id BIGINT NOT NULL REFERENCES veterinario(id) ON DELETE CASCADE,
    dia_semana VARCHAR(10) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_horario_rango CHECK (hora_fin > hora_inicio)
);

CREATE INDEX idx_horario_vet_dia ON horario_atencion (veterinario_id, dia_semana);

-- Seed: lunes a viernes 08:00-17:00 para todos los veterinarios existentes
INSERT INTO horario_atencion (veterinario_id, dia_semana, hora_inicio, hora_fin)
SELECT v.id, d.dia, TIME '08:00:00', TIME '17:00:00'
FROM veterinario v
CROSS JOIN (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY')) AS d(dia);
