UPDATE servicio
SET descripcion = CASE
    WHEN lower(nombre) LIKE 'consulta general%' THEN
        'Valoración clínica inicial para revisar el estado general de la mascota, identificar necesidades de atención y orientar los siguientes pasos con el veterinario.'
    WHEN lower(nombre) LIKE 'vacunaci%' THEN
        'Aplicación o actualización de vacunas según la edad, antecedentes y esquema preventivo de la mascota, previa revisión del profesional.'
    WHEN lower(nombre) LIKE 'desparasitaci%' THEN
        'Atención preventiva frente a parásitos internos o externos; el veterinario define el producto y la pauta adecuados para cada mascota.'
    WHEN lower(nombre) LIKE 'cirugía cardíaca%' THEN
        'Valoración y atención quirúrgica especializada de afecciones del corazón; requiere evaluación previa y criterio del equipo veterinario.'
    WHEN lower(nombre) LIKE 'cirugía mayor%' THEN
        'Atención para procedimientos quirúrgicos de mayor complejidad cuando, tras la valoración veterinaria, se determina que son necesarios.'
    WHEN lower(nombre) LIKE 'cirug%' THEN
        'Valoración y atención de casos que pueden requerir un procedimiento quirúrgico; el profesional determina su pertinencia y alcance.'
    WHEN lower(nombre) LIKE 'dermatolog%' THEN
        'Valoración de alteraciones de piel y pelo, como picazón, irritación, enrojecimiento, lesiones o caída de pelo, para orientar su atención.'
    WHEN lower(nombre) LIKE 'odontolog%' OR lower(nombre) LIKE '%limpieza dental%' THEN
        'Evaluación de dientes y encías y atención de necesidades de higiene o salud oral, según los hallazgos del veterinario.'
    WHEN lower(nombre) LIKE 'ecografía%' OR lower(nombre) LIKE 'ecografia%' OR lower(nombre) LIKE '%ultrasonido%' THEN
        'Examen de diagnóstico por imagen que permite evaluar órganos y tejidos internos mediante ultrasonido, cuando el veterinario lo indica.'
    WHEN lower(nombre) LIKE 'rehabilitación%' OR lower(nombre) LIKE 'rehabilitacion%' THEN
        'Atención de apoyo para movilidad y recuperación funcional mediante ejercicios o terapias definidos según la valoración profesional.'
    WHEN lower(nombre) LIKE 'control veterinario%' THEN
        'Seguimiento clínico para revisar la evolución de la mascota y ajustar las recomendaciones de cuidado con el profesional.'
    ELSE
        'Servicio veterinario relacionado con ' || lower(nombre) || '. El profesional determina su alcance y pertinencia según la valoración de la mascota.'
END
WHERE descripcion IS NULL
   OR btrim(descripcion) = ''
   OR lower(btrim(descripcion)) IN (
       'revisión completa del paciente',
       'revision completa del paciente',
       'aplicación de vacunas al paciente',
       'aplicacion de vacunas al paciente',
       'tratamiento antiparasitario interno y externo',
       'tratamiento de parásitos internos y externos para mantener la salud del paciente',
       'tratamiento de parasitos internos y externos para mantener la salud del paciente',
       'procedimiento quirúrgico complejo',
       'procedimiento quirurgico complejo',
       'tratamiento de problemas de piel',
       'limpieza y tratamiento dental',
       'examen de ultrasonido',
       'procedimiento cardíaco especializado',
       'procedimiento cardiaco especializado',
       'terapia física y rehabilitación',
       'terapia fisica y rehabilitacion',
       'seguimiento clínico del paciente',
       'seguimiento clinico del paciente'
   );

ALTER TABLE servicio
    ALTER COLUMN descripcion SET NOT NULL;
