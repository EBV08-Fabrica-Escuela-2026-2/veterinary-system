ALTER TABLE cita
    ADD COLUMN diagnostico VARCHAR(1000),
    ADD COLUMN tratamiento VARCHAR(1000),
    ADD COLUMN notas_clinicas VARCHAR(1000);

CREATE INDEX idx_cita_mascota_fecha ON cita (mascota_id, fecha_hora_inicio DESC, id DESC);
CREATE INDEX idx_cita_veterinario_mascota ON cita (veterinario_id, mascota_id);
