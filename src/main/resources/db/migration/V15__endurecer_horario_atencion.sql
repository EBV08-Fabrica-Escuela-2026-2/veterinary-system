-- Datos creados antes de esta restricción pudieron quedar traslapados: se conserva
-- la franja más antigua (menor id) y se desactivan las que choquen con ella.
DO $$
DECLARE
    franja RECORD;
BEGIN
    FOR franja IN
        SELECT id, veterinario_id, dia_semana, hora_inicio, hora_fin
        FROM horario_atencion
        WHERE activo
        ORDER BY id
    LOOP
        IF EXISTS (
            SELECT 1
            FROM horario_atencion otra
            WHERE otra.activo
              AND otra.id < franja.id
              AND otra.veterinario_id = franja.veterinario_id
              AND otra.dia_semana = franja.dia_semana
              AND otra.hora_inicio < franja.hora_fin
              AND franja.hora_inicio < otra.hora_fin
        ) THEN
            UPDATE horario_atencion
            SET activo = FALSE, updated_at = CURRENT_TIMESTAMP
            WHERE id = franja.id;
        END IF;
    END LOOP;
END $$;

ALTER TABLE horario_atencion
    ADD CONSTRAINT chk_horario_dia_semana
    CHECK (dia_semana IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'));

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE horario_atencion
    ADD CONSTRAINT no_traslape_horario
    EXCLUDE USING gist (
        veterinario_id WITH =,
        dia_semana WITH =,
        tsrange(DATE '2000-01-01' + hora_inicio, DATE '2000-01-01' + hora_fin, '[)') WITH &&
    )
    WHERE (activo);
