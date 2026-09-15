-- =============================================
-- VETERINARIA - Script de creación y datos de prueba
-- =============================================

-- Eliminar tablas si existen (solo para desarrollo)
DROP TABLE IF EXISTS servicio CASCADE;
DROP TABLE IF EXISTS veterinario CASCADE;

-- =============================================
-- 1. CREACIÓN DE TABLAS
-- =============================================

CREATE TABLE veterinario (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    telefono VARCHAR(20),
    direccion VARCHAR(255),
    horario_atencion VARCHAR(100),
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE servicio (
    id BIGSERIAL PRIMARY KEY,
    veterinario_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    precio DECIMAL(10, 2) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_servicio_veterinario
        FOREIGN KEY (veterinario_id)
        REFERENCES veterinario(id)
        ON DELETE CASCADE
);

-- =============================================
-- 2. DATOS DE PRUEBA
-- =============================================

-- Veterinario CON servicios (Escenario 1, 2, 3)
INSERT INTO veterinario (nombre, telefono, direccion, horario_atencion)
VALUES ('Dr. Carlos Pérez', '310-123-4567', 'Calle 10 #5-20, Bogotá', '8:00 AM - 6:00 PM');

INSERT INTO servicio (veterinario_id, nombre, descripcion, precio)
VALUES (1, 'Consulta General', 'Revisión completa del paciente', 85000.00);

INSERT INTO servicio (veterinario_id, nombre, descripcion, precio)
VALUES (1, 'Vacunación', 'Aplicación de vacunas al paciente', 45000.00);

INSERT INTO servicio (veterinario_id, nombre, descripcion, precio)
VALUES (1, 'Cirugía Mayor', 'Procedimiento quirúrgico complejo', 350000.00);

INSERT INTO servicio (veterinario_id, nombre, descripcion, precio)
VALUES (1, 'Desparasitación', 'Tratamiento antiparasitario interno y externo', 35000.00);

-- Veterinario CON servicios (Escenario 1, 2, 3)
INSERT INTO veterinario (nombre, telefono, direccion, horario_atencion)
VALUES ('Dra. María López', '315-987-6543', 'Carrera 8 #12-34, Medellín', '9:00 AM - 5:00 PM');

INSERT INTO servicio (veterinario_id, nombre, descripcion, precio)
VALUES (2, 'Dermatología', 'Tratamiento de problemas de piel', 120000.00);

INSERT INTO servicio (veterinario_id, nombre, descripcion, precio)
VALUES (2, 'Odontología', 'Limpieza y tratamiento dental', 150000.00);

INSERT INTO servicio (veterinario_id, nombre, descripcion, precio)
VALUES (2, 'Ecografía', 'Examen de ultrasonido', 95000.00);

-- Veterinario SIN servicios (Escenario 5)
INSERT INTO veterinario (nombre, telefono, direccion, horario_atencion)
VALUES ('Dr. Andrés Martínez', '320-456-7890', 'Avenida 5 #20-15, Cali', '10:00 AM - 4:00 PM');

-- Veterinario adicional con servicios de alto valor
INSERT INTO veterinario (nombre, telefono, direccion, horario_atencion)
VALUES ('Dra. Laura Rodríguez', '301-234-5678', 'Calle 72 #10-25, Bogotá', '7:00 AM - 3:00 PM');

INSERT INTO servicio (veterinario_id, nombre, descripcion, precio)
VALUES (4, 'Cirugía Cardíaca', 'Procedimiento cardíaco especializado', 2500000.00);

INSERT INTO servicio (veterinario_id, nombre, descripcion, precio)
VALUES (4, 'Rehabilitación', 'Terapia física y rehabilitación', 180000.00);

-- =============================================
-- 3. VERIFICACIÓN
-- =============================================

-- Ver veterinarios
SELECT id, nombre, telefono, horario_atencion FROM veterinario;

-- Ver servicios con precio formateado
SELECT 
    v.nombre AS veterinario,
    s.nombre AS servicio,
    '$' || REPLACE(REPLACE(CAST(s.precio AS TEXT), '.', ''), '.', '') AS precio_cop
FROM servicio s
JOIN veterinario v ON s.veterinario_id = v.id
ORDER BY v.nombre, s.nombre;
