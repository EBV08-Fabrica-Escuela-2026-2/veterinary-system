ALTER TABLE servicio
ADD COLUMN categoria VARCHAR(100),
ADD COLUMN indicaciones_previas VARCHAR(1000);

ALTER TABLE veterinario
ADD COLUMN foto_url VARCHAR(500);

UPDATE servicio SET categoria = 'Medicina preventiva',
    indicaciones_previas = 'Trae el carné de vacunación de tu mascota. Evita alimentarla durante las 2 horas previas. Llega 10 minutos antes para realizar el registro.'
WHERE nombre ILIKE 'Vacunaci%';

UPDATE servicio SET categoria = 'Medicina preventiva'
WHERE nombre ILIKE 'Desparasitaci%';

UPDATE servicio SET categoria = 'Consulta'
WHERE nombre ILIKE 'Consulta%';

UPDATE servicio SET categoria = 'Cirugía',
    indicaciones_previas = 'Mantén a tu mascota en ayuno de 8 horas antes del procedimiento.'
WHERE nombre ILIKE 'Cirug%';