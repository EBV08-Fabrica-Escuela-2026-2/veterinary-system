ALTER TABLE cita
    ADD COLUMN decision_horario VARCHAR(20),
    ADD COLUMN cancelada_por VARCHAR(150);

ALTER TABLE cita
    ADD CONSTRAINT chk_cita_decision_horario
    CHECK (decision_horario IS NULL OR decision_horario IN ('LIBERAR_CUPO', 'BLOQUEAR_FRANJA'));

CREATE TABLE bloqueo_horario (
    id BIGSERIAL PRIMARY KEY,
    veterinario_id BIGINT NOT NULL REFERENCES veterinario(id),
    cita_id BIGINT REFERENCES cita(id),
    fecha_hora_inicio TIMESTAMP NOT NULL,
    fecha_hora_fin TIMESTAMP NOT NULL,
    motivo VARCHAR(240) NOT NULL,
    creado_por VARCHAR(150) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bloqueo_rango CHECK (fecha_hora_fin > fecha_hora_inicio)
);

CREATE INDEX idx_bloqueo_vet_rango
    ON bloqueo_horario (veterinario_id, fecha_hora_inicio, fecha_hora_fin)
    WHERE activo = TRUE;

CREATE UNIQUE INDEX uq_bloqueo_activo_por_cita
    ON bloqueo_horario (cita_id)
    WHERE activo = TRUE AND cita_id IS NOT NULL;