
ALTER TABLE veterinario
ADD COLUMN documento_identidad VARCHAR(20),
ADD COLUMN correo VARCHAR(150);

UPDATE veterinario SET documento_identidad = 'PENDIENTE-' || id WHERE documento_identidad IS NULL;
UPDATE veterinario SET correo = 'pendiente' || id || '@veterinaria.com' WHERE correo IS NULL;


ALTER TABLE veterinario
ALTER COLUMN documento_identidad SET NOT NULL,
ALTER COLUMN correo SET NOT NULL;

ALTER TABLE veterinario
ADD CONSTRAINT uq_veterinario_documento UNIQUE (documento_identidad),
ADD CONSTRAINT uq_veterinario_correo UNIQUE (correo);