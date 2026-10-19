UPDATE veterinario
SET nombre = 'Dr. Camilo Durán',
    documento_identidad = '1030000001',
    correo = 'camilo.duran@vetagenda.com',
    especialidad = 'Medicina Interna',
    activo = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;
