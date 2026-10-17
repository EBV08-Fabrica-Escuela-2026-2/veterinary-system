-- Perfil demostrativo integrado para las revisiones académicas.
INSERT INTO cliente (nombre, documento_identidad, telefono, correo, direccion, activo)
VALUES (
    'Carolina Restrepo',
    '1012345678',
    '3001234567',
    'carolina.restrepo@vetagenda.com',
    'Medellín, Antioquia',
    TRUE
)
ON CONFLICT (documento_identidad) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    telefono = EXCLUDED.telefono,
    correo = EXCLUDED.correo,
    direccion = EXCLUDED.direccion,
    activo = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO cliente_credencial (cliente_id, password_hash)
SELECT c.id, '$2y$10$RnNxY7Vyu9xDwJk0wjXKYeht3z7GDYVCsM0HEOWQh.E96j/ryyxiS'
FROM cliente c
WHERE c.documento_identidad = '1012345678'
ON CONFLICT (cliente_id) DO UPDATE SET
    password_hash = EXCLUDED.password_hash;

INSERT INTO mascota (nombre, especie, raza, edad, sexo, observaciones, cliente_id)
SELECT 'Rocky', 'Canino', 'Golden Retriever', 4, 'Macho',
       'Paciente demostrativo de VetAgenda', c.id
FROM cliente c
WHERE c.documento_identidad = '1012345678'
  AND NOT EXISTS (
      SELECT 1
      FROM mascota m
      WHERE m.cliente_id = c.id AND LOWER(m.nombre) = 'rocky'
  );

INSERT INTO servicio (veterinario_id, nombre, descripcion, precio, activo, duracion_minutos, categoria)
SELECT v.id, 'Control veterinario', 'Seguimiento clínico del paciente', 50000.00, TRUE, 30, 'Consulta'
FROM veterinario v
WHERE v.correo = 'pendiente1@veterinaria.com'
  AND NOT EXISTS (
      SELECT 1
      FROM servicio s
      WHERE s.veterinario_id = v.id AND LOWER(s.nombre) = 'control veterinario'
  );

INSERT INTO cita (
    cliente_id, mascota_id, veterinario_id, servicio_id,
    fecha_hora_inicio, fecha_hora_fin, estado
)
SELECT c.id, m.id, v.id, s.id,
       TIMESTAMP '2026-10-25 08:00:00', TIMESTAMP '2026-10-25 08:30:00', 'CONFIRMADA'
FROM cliente c
JOIN mascota m ON m.cliente_id = c.id AND LOWER(m.nombre) = 'rocky'
JOIN veterinario v ON v.correo = 'pendiente1@veterinaria.com'
JOIN servicio s ON s.veterinario_id = v.id AND LOWER(s.nombre) = 'consulta general'
WHERE c.documento_identidad = '1012345678'
  AND NOT EXISTS (
      SELECT 1 FROM cita ci
      WHERE ci.cliente_id = c.id AND ci.fecha_hora_inicio = TIMESTAMP '2026-10-25 08:00:00'
  )
ON CONFLICT DO NOTHING;

INSERT INTO cita (
    cliente_id, mascota_id, veterinario_id, servicio_id,
    fecha_hora_inicio, fecha_hora_fin, estado
)
SELECT c.id, m.id, v.id, s.id,
       TIMESTAMP '2026-10-28 10:00:00', TIMESTAMP '2026-10-28 10:20:00', 'CONFIRMADA'
FROM cliente c
JOIN mascota m ON m.cliente_id = c.id AND LOWER(m.nombre) = 'rocky'
JOIN veterinario v ON v.correo = 'pendiente1@veterinaria.com'
JOIN servicio s ON s.veterinario_id = v.id AND LOWER(s.nombre) = 'vacunación'
WHERE c.documento_identidad = '1012345678'
  AND NOT EXISTS (
      SELECT 1 FROM cita ci
      WHERE ci.cliente_id = c.id AND ci.fecha_hora_inicio = TIMESTAMP '2026-10-28 10:00:00'
  )
ON CONFLICT DO NOTHING;

INSERT INTO cita (
    cliente_id, mascota_id, veterinario_id, servicio_id,
    fecha_hora_inicio, fecha_hora_fin, estado, motivo_cancelacion, cancelada_at
)
SELECT c.id, m.id, v.id, s.id,
       TIMESTAMP '2026-10-15 14:00:00', TIMESTAMP '2026-10-15 14:30:00',
       'ANULADA', 'Imprevisto del tutor de la mascota', TIMESTAMP '2026-10-14 09:00:00'
FROM cliente c
JOIN mascota m ON m.cliente_id = c.id AND LOWER(m.nombre) = 'rocky'
JOIN veterinario v ON v.correo = 'pendiente1@veterinaria.com'
JOIN servicio s ON s.veterinario_id = v.id AND LOWER(s.nombre) = 'control veterinario'
WHERE c.documento_identidad = '1012345678'
  AND NOT EXISTS (
      SELECT 1 FROM cita ci
      WHERE ci.cliente_id = c.id AND ci.fecha_hora_inicio = TIMESTAMP '2026-10-15 14:00:00'
  );
